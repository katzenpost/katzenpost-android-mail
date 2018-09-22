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
import java.util.concurrent.atomic.AtomicBoolean

private data class KatzenpostClientState(
        val client: Client,
        internal val serverSettings: KatzenpostServerSettings,
        internal val isShutdown: AtomicBoolean,
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
            if (clientState.isShutdown.get()) {
                throw MessagingException("Katzenpost client not connected! (was shut down)")
            }
            sendMessage(clientState, message)
        } else {
            Timber.w("No client state for %s on poll!", settings.address)
            throw MessagingException("Katzenpost client not connected!")
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
        return if (clientState != null) {
            if (clientState.isShutdown.get()) {
                Timber.w("Client was shut down for %s on poll!", settings.address)
            }
            clientState.receiveQueue.poll()
        } else {
            Timber.w("No client state for %s on poll!", settings.address)
            null
        }
    }

    fun registerKatzenpostClient(settings: KatzenpostServerSettings, pushReceiver: PushReceiver) {
        if (!clientStateMap.containsKey(settings.address)) {
            clientStateMap[settings.address] = createNewClientState(settings, pushReceiver)
        }
    }

    private fun createNewClientState(settings: KatzenpostServerSettings, pushReceiver: PushReceiver): KatzenpostClientState {
        val config = createConfig(settings)
        val client = Katzenpost.new_(config)
        val receiveQueue = ConcurrentLinkedQueue<String>()
        val isShutdown = AtomicBoolean(false)
        val pollThread = KatzenpostMessagePollThread("Katzenpost/${settings.address}", client, isShutdown) { _, body ->
            receiveQueue.offer(body)
            pushReceiver.syncFolder("INBOX", null)
        }
        pollThread.start()

        return KatzenpostClientState(client, settings, isShutdown, pushReceiver, pollThread, receiveQueue)
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
        clientStateMap.filter { it.value.isShutdown.get() } .forEach { address, oldState ->
            Timber.d("Refreshing Katzenpost client %s", address)
            val newClientState = createNewClientState(oldState.serverSettings, oldState.pushReceiver)
            clientStateMap[address] = newClientState
        }
    }

    fun stopAll() {
        for (k in clientStateMap) {
            k.value.isShutdown.set(true)
            k.value.pollThread.interrupt()
        }
    }

    companion object {
        private const val PKI_ADDRESS = "37.218.242.147:29485"
        private const val PKI_PINNED_PUBLIC_KEY = "39Xhom6bPvez2gECACuTxm/DaxLRTGCMP7/KA78+vNw="
//        private const val PKI_ADDRESS = "95.179.156.72:29483"
//        private const val PKI_PINNED_PUBLIC_KEY = "o4w1Nyj/nKNwho5SWfAIfh7SMU8FRx52nMHGgYsMHqQ="
    }
}

const val MAXATTEMPTS = 10

private class KatzenpostMessagePollThread(
        threadName: String,
        private val client: Client,
        private val isShutdown: AtomicBoolean,
        private val onReceiveCallback: (sender: String, message: String) -> Unit
) : Thread(threadName) {
    override fun run() {
        try {
            var attempts = 1
            while (!Thread.interrupted() && !isShutdown.get()) {
                Timber.d("$name: Waiting to connect ($attempts/$MAXATTEMPTS)…")
                val connected = client.waitToConnect(6 * 1000)
                if (connected) {
                    Timber.d("$name: Connected!")
                    break
                }
                attempts += 1
                if (attempts > MAXATTEMPTS) {
                    throw(Exception("Timeout waiting to connect!"))
                }

            }
        } catch (e: Exception) {
            Timber.e(e, "$name: Failed to connect!")
            shutdownSilently()
            return
        }

        try {
            while (!Thread.interrupted() && !isShutdown.get()) {
                Timber.d("$name: Looping…")
                try {
                    val msg = client.getMessage(10 * 60 * 1000)
                    if (msg != null) {
                        Timber.d("$name: Got a message!")
                        onReceiveCallback(msg.sender, msg.payload)
                    }
                    // just make sure we don't super-hotloop
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    Timber.d("$name: Interrupted!")
                    // nvm
                } catch (e: Exception) {
                    Timber.e(e, "$name: Error waiting for message!")
                    break
                }
            }
        } finally {
            shutdownSilently()
        }
    }

    fun shutdownSilently() {
        try {
            isShutdown.set(true)
            Timber.d("$name: Shutting down")
            client.shutdown()
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
