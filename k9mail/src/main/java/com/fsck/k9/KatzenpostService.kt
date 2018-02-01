package com.fsck.k9

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.store.katzenpost.KatzenpostServerSettings
import com.fsck.k9.mail.store.katzenpost.KatzenpostStore
import katzenpost.Client
import katzenpost.Config
import katzenpost.Katzenpost
import katzenpost.LogConfig
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

class KatzenpostService : Service() {
    private var clientThread: Thread? = null
    private var account: Account? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("KatzenpostService Intent: %s", intent)
        when (intent?.action) {
            "start" -> startClientThread(intent.getStringExtra("account"))
            "stop" -> stopClientThread()
        }

        return START_STICKY
    }

    private fun startClientThread(accountUuid: String) {
        val account = Preferences.getPreferences(this).getAccount(accountUuid)
        if (this.account != null && this.account != account) throw IllegalArgumentException("only one account supported!")

        if (clientThread != null) return

        val settings = KatzenpostStore.decodeUri(account.storeUri)
        val config = createConfig(settings)

        val client = Katzenpost.new_(config)
        val clientThread = ClientThread(applicationContext, account, client)

        this.clientThread = clientThread
        this.account = account

        val notification = getNotification()
        startForeground(1234567, notification)

        clientThread.start()
    }

    private fun stopClientThread() {
        stopForeground(true)

        clientThread?.interrupt()
        clientThread?.join(3000)

        clientThread = null
        account = null
    }

    private fun getNotification(): Notification {
        val builder = NotificationCompat.Builder(this)
        builder.setSmallIcon(R.drawable.icon)
        builder.setContentTitle("Katzenpost")
        builder.setContentText("Mixin' it up…")
        builder.priority = NotificationCompat.PRIORITY_MIN
        return builder.build()
    }

    private fun createConfig(settings: KatzenpostServerSettings): Config {
        val config = prepareConfig()

        val key = Katzenpost.stringToKey(settings.linkkey)

        config.provider = settings.provider
        config.user = settings.username
        config.linkKey = key

        return config
    }

    private fun prepareConfig(): Config {
        val logConfig = LogConfig()
        logConfig.level = "DEBUG"
        logConfig.enabled = true

        val katzenCacheDir = File(cacheDir, "katzencache")

        val config = Config()
        config.pkiAddress = PKI_ADDRESS
        config.pkiKey = PKI_PINNED_PUBLIC_KEY
        config.dataDir = katzenCacheDir.absolutePath
        config.log = logConfig
        return config
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private class ClientThread(val context: Context, val account: Account, val client: Client) : Thread() {
        override fun run() {
            client.waitToConnect()

            while (!Thread.interrupted()) {
                val msg = client.getMessage(2500)
                if (msg != null) {
                    queue.offer(msg.payload)
                    triggerPolling()
                }
                Thread.sleep(2500)
            }

            stopService(context)
        }

        private fun triggerPolling() {
            MessagingController.getInstance(context).synchronizeMailbox(account, null, null, null)
        }
    }

    companion object {
        private val queue = ConcurrentLinkedQueue<String>()

        private val PKI_ADDRESS = "37.218.242.147:29485"
        private val PKI_PINNED_PUBLIC_KEY = "DFD5E1A26E9B3EF7B3DA0102002B93C66FC36B12D14C608C3FBFCA03BF3EBCDC"

        fun pollMessage(): String? {
            try {
                return queue.remove()
            } catch (e: NoSuchElementException) {
                return null
            }
        }

        fun startService(context: Context, accountUuid: String) {
            val intent = Intent(context, KatzenpostService::class.java)
            intent.action = "start"
            intent.putExtra("account", accountUuid)
            context.startService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KatzenpostService::class.java)
            intent.action = "stop"
            context.startService(intent)
        }
    }
}
