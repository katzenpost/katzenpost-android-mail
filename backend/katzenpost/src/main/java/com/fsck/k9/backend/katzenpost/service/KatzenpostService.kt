package com.fsck.k9.backend.katzenpost.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
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
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class KatzenpostService : Service() {
    private var clientThread: ClientThread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("KatzenpostService Intent: %s", intent)
        when (intent?.action) {
            "start" -> startClientThread(serverSettings.getValue(intent.getStringExtra("address")))
            "stop" -> stopClientThread()
        }

        return START_STICKY
    }

    private fun startClientThread(settings: KatzenpostServerSettings) {
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

        config.provider = settings.provider
        config.user = settings.name
        config.linkKey = Katzenpost.stringToKey(settings.linkkey)
        config.identityKey = Katzenpost.stringToKey(settings.idkey)

        return config
    }

    private fun prepareConfig(): Config {
        val logConfig = LogConfig()
        logConfig.level = "DEBUG"
        logConfig.enabled = true

        val katzenCacheDir = File(cacheDir, "katzencache")
        katzenCacheDir.mkdir()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.setPosixFilePermissions(katzenCacheDir.toPath(), setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE))
        }

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
                for (outgoingMsg in sendQueue) {
                    client.send(outgoingMsg.recipient, outgoingMsg.message)
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
        private val serverSettings = mutableMapOf<String,KatzenpostServerSettings>()

        private const val PKI_ADDRESS = "95.179.156.72:29483"
        private const val PKI_PINNED_PUBLIC_KEY = "o4w1Nyj/nKNwho5SWfAIfh7SMU8FRx52nMHGgYsMHqQ="

        fun pollMessage(): String? = receiveQueue.poll()

        @Throws(MessagingException::class)
        fun sendMessage(context: Context, katzenpostServerSettings: KatzenpostServerSettings, message: com.fsck.k9.mail.Message) {
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

            ensureService(context, katzenpostServerSettings)
        }

        fun ensureService(context: Context, katzenpostServerSettings: KatzenpostServerSettings) {
            serverSettings.put(katzenpostServerSettings.address, katzenpostServerSettings)

            val intent = Intent(context, KatzenpostService::class.java)
            intent.action = "start"
            intent.putExtra("address", katzenpostServerSettings.address)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KatzenpostService::class.java)
            intent.action = "stop"
            context.startService(intent)
        }
    }
}

private data class OutgoingMsg(val recipient: String, val message: String)
