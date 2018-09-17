package com.fsck.k9.backend.katzenpost.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.fsck.k9.backend.katzenpost.KatzenpostServerSettings
import com.fsck.k9.backend.katzenpost.R
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import katzenpost.Client
import katzenpost.Config
import katzenpost.Katzenpost
import katzenpost.LogConfig
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class KatzenpostService(private val settings: KatzenpostServerSettings) : Service() {
    private var clientThread: ClientThread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("KatzenpostService Intent: %s", intent)
        when (intent?.action) {
            "start" -> startClientThread(intent.getStringExtra("account"))
            "stop" -> stopClientThread()
        }

        return START_STICKY
    }

    private fun startClientThread(accountUuid: String) {
        if (clientThread != null) {
            return
        }

        val config = createConfig(settings)

        val client = Katzenpost.new_(config)
        val clientThread = ClientThread(applicationContext, client)

        this.clientThread = clientThread

        val notification = getNotification()
        startForeground(1234567, notification)

        clientThread.start()
    }

    private fun stopClientThread() {
        stopForeground(true)

        clientThread?.interrupt()
        clientThread?.join(3000)

        clientThread = null

        stopSelf()
    }

    private fun getNotification(): Notification {
        val builder = NotificationCompat.Builder(this)
        builder.setSmallIcon(R.drawable.ic_katzenpost)
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

    private class ClientThread(val context: Context, val client: Client) : Thread() {
        override fun run() {
            client.waitToConnect()

            while (!Thread.interrupted()) {
                val msg = client.getMessage(2500)
                if (msg != null) {
                    receiveQueue.offer(msg.payload)
                    triggerPolling()
                }
                Thread.sleep(2500)
            }

            stopService(context)
        }

        private fun triggerPolling() {
            // TODO
        }
    }

    companion object {
        private val receiveQueue = ConcurrentLinkedQueue<String>()
        private val sendQueue = ConcurrentLinkedQueue<OutgoingMsg>()

        private const val PKI_ADDRESS = "37.218.242.147:29485"
        private const val PKI_PINNED_PUBLIC_KEY = "DFD5E1A26E9B3EF7B3DA0102002B93C66FC36B12D14C608C3FBFCA03BF3EBCDC"

        fun pollMessage(): String? = receiveQueue.poll()

        @Throws(MessagingException::class)
        fun sendMessage(context: Context, accountUuid: String, message: com.fsck.k9.mail.Message) {
            val baos = ByteArrayOutputStream()
            try {
                message.writeTo(baos)
            } catch (e: IOException) {
                throw MessagingException("Error encoding message for transport!", e)
            }

            val msgBytes = baos.toByteArray()

            val addresses = ArrayList<Address>()
            run {
                addresses.addAll(Arrays.asList(*message.getRecipients(Message.RecipientType.TO)))
                addresses.addAll(Arrays.asList(*message.getRecipients(Message.RecipientType.CC)))
                addresses.addAll(Arrays.asList(*message.getRecipients(Message.RecipientType.BCC)))
            }
            message.setRecipients(Message.RecipientType.BCC, null)

            for (address in addresses) {
                sendQueue.offer(OutgoingMsg(address.address, String(msgBytes)))
            }

            refreshService(context, accountUuid)
        }

        fun refreshService(context: Context, accountUuid: String) {
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

private data class OutgoingMsg(val recipient: String, val message: String)