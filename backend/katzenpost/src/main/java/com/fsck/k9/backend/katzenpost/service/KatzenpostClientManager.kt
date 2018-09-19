package com.fsck.k9.backend.katzenpost.service

import android.content.Context
import android.os.Build
import com.fsck.k9.backend.katzenpost.KatzenpostServerSettings
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.PushReceiver
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

private data class KatzenpostClientState(
        val client: Client,
        internal var pushReceiver: PushReceiver,
        internal var pollThread: Thread,
        internal val receiveQueue: ConcurrentLinkedQueue<String>
)

class KatzenpostClientManager(val context: Context) {
    private val clientStateMap: MutableMap<String,KatzenpostClientState> = mutableMapOf()

    @Throws(MessagingException::class)
    fun sendMessage(settings: KatzenpostServerSettings, message: Message) {
        val clientState = clientStateMap[settings.address]
        if (clientState != null) {
            sendMessage(clientState, message)
        } else {
            Timber.w("No client state for %s on poll!", settings.address)
            throw MessagingException("Katzenpost client not connected for %s!")
        }
    }

    private fun sendMessage(clientState: KatzenpostClientState, message: Message) {
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
            if (!clientState.client.hasKey(address.address)) {
                clientState.client.getKey(address.address)
            }
            clientState.client.send(address.address, String(msgBytes))
        }
    }

    fun pollMessage(settings: KatzenpostServerSettings): String? {
        val clientState = clientStateMap[settings.address]
        if (clientState != null) {
            return clientState.receiveQueue.poll()
        } else {
            Timber.w("No client state for %s on poll!", settings.address)
            return null
        }
    }

    fun registerKatzenpostClient(settings: KatzenpostServerSettings, pushReceiver: PushReceiver) {
        getOrCreateKatzenpostClientState(settings, pushReceiver)
    }

    private fun getOrCreateKatzenpostClientState(settings: KatzenpostServerSettings, pushReceiver: PushReceiver): KatzenpostClientState {
        return clientStateMap.getOrPut(settings.address) { create(settings, pushReceiver) }
    }

    private fun create(settings: KatzenpostServerSettings, pushReceiver: PushReceiver): KatzenpostClientState {
        val config = createConfig(settings)
        val client = Katzenpost.new_(config)
        val receiveQueue = ConcurrentLinkedQueue<String>()
        val pollThread = KatzenpostMessagePollThread(client) { sender, body ->
            receiveQueue.offer(body)
            pushReceiver.syncFolder("INBOX", null)
        }
        pollThread.start()

        return KatzenpostClientState(client, pushReceiver, pollThread, receiveQueue)
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

        val katzenCacheDir = File(context.filesDir, "katzencache")
        katzenCacheDir.mkdir()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val permissions0700 = setOf(
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE)
            Files.setPosixFilePermissions(katzenCacheDir.toPath(), permissions0700)
        }

        val config = Config()
        config.pkiAddress = PKI_ADDRESS
        config.pkiKey = PKI_PINNED_PUBLIC_KEY
        config.dataDir = katzenCacheDir.absolutePath
        config.log = logConfig

        return config
    }

    fun refreshAll() {
        // TODO
    }

    fun stopAll() {
        for (k in clientStateMap) {
            k.value.pollThread.interrupt()
            clientStateMap.remove(k.key)
        }
    }

    companion object {
        private const val PKI_ADDRESS = "95.179.156.72:29483"
        private const val PKI_PINNED_PUBLIC_KEY = "o4w1Nyj/nKNwho5SWfAIfh7SMU8FRx52nMHGgYsMHqQ="
    }
}

private class KatzenpostMessagePollThread(
        private val client: Client,
        private val onReceiveCallback: (sender: String, message: String) -> Unit
) : Thread() {
    override fun run() {
        client.waitToConnect()

        while (!Thread.interrupted()) {
            try {
                val msg = client.getMessage(10 * 60 * 1000)
                if (msg != null) {
                    onReceiveCallback(msg.sender, msg.payload)
                }
                // just make sure we don't super-hotloop
                Thread.sleep(1000)
            } catch (e: InterruptedException) {
                // nvm
            }
        }

        client.shutdown()
    }
}
