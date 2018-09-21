package com.fsck.k9.backend.katzenpost

import android.content.Context
import com.fsck.k9.backend.api.*
import com.fsck.k9.backend.katzenpost.service.KatzenpostClientManager
import com.fsck.k9.backend.katzenpost.service.KatzenpostService
import com.fsck.k9.mail.*
import com.fsck.k9.mail.internet.MimeMessage
import timber.log.Timber

class KatzenpostBackend(
        private val context: Context,
        private val accountUuid: String,
        val backendStorage: BackendStorage,
        private val katzenpostServerSettings: KatzenpostServerSettings,
        private val katzenpostClientManager: KatzenpostClientManager
) : Backend {
    // private val katzenpostSync: KatzenpostSync = KatzenpostSync(accountUuid, backendStorage, katzenpostStore)
    private val commandRefreshFolderList = CommandRefreshFolderList(backendStorage, katzenpostServerSettings.address)

    override val supportsSeenFlag = false
    override val supportsExpunge = false
    override val supportsMove = false
    override val supportsCopy = false
    override val supportsUpload = false
    override val supportsTrashFolder = false
    override val supportsSearchByDate = false
    override val isPushCapable = true

    override fun refreshFolderList() {
        commandRefreshFolderList.refreshFolderList()
    }

    override fun sync(folder: String, syncConfig: SyncConfig, listener: SyncListener, providedRemoteFolder: Folder<*>?) {
        if (folder != "INBOX") return

        Timber.i("Polling Katzenpost %s", katzenpostServerSettings.address)

        val backendFolder by lazy { backendStorage.getFolder(folder) }
        listener.syncStarted(folder, backendFolder.name)

        // TODO
        // KatzenpostService.ensureForegroundService(context)

        var newMessageCount = 0
        var msg: String?
        while (true) {
            msg = katzenpostClientManager.pollMessage(katzenpostServerSettings)
            if (msg == null) break

            val message = MimeMessage.parseMimeMessage(msg.byteInputStream(), true)
            backendFolder.saveCompleteMessage(message)
            listener.syncNewMessage(folder, message.uid, false)
            newMessageCount += 1
        }

        backendFolder.setLastChecked(System.currentTimeMillis())
        backendFolder.setMoreMessages(BackendFolder.MoreMessages.FALSE)

        listener.syncFinished(folder, backendFolder.getMessageCount(), newMessageCount)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) =
            throw UnsupportedOperationException("not implemented")

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        // throw UnsupportedOperationException("not supported")
    }

    override fun markAllAsRead(folderServerId: String) = throw UnsupportedOperationException("not supported")

    override fun expunge(folderServerId: String) = throw UnsupportedOperationException("not supported")

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) =
            throw UnsupportedOperationException("not supported")

    override fun deleteAllMessages(folderServerId: String) = throw UnsupportedOperationException("not supported")

    override fun moveMessages(
            sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>
    ): Map<String, String>? = throw UnsupportedOperationException("not supported")

    override fun copyMessages(
            sourceFolderServerId: String, targetFolderServerId: String, messageServerIds: List<String>
    ): Map<String, String>? = throw UnsupportedOperationException("not supported")

    override fun search(
            folderServerId: String, query: String?, requiredFlags: Set<Flag>?, forbiddenFlags: Set<Flag>?
    ): List<String> = throw UnsupportedOperationException("not supported")

    override fun fetchMessage(folderServerId: String, messageServerId: String, fetchProfile: FetchProfile): Message {
        throw UnsupportedOperationException("not supported")
        // return commandFetchMessage.fetchMessage(folderServerId, messageServerId, fetchProfile)
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) =
        throw UnsupportedOperationException("not supported")

    override fun findByMessageId(folderServerId: String, messageId: String): String? = null

    override fun uploadMessage(folderServerId: String, message: Message): String? =
            throw UnsupportedOperationException("not supported")

    override fun createPusher(receiver: PushReceiver): Pusher {
        return object : Pusher {
            var lastRefreshValue: Long = 0

            override fun start(folderServerIds: MutableList<String>?) {
                lastRefreshValue = currentTimeMillis()

                Timber.i("Starting Katzenpost client %s", katzenpostServerSettings.address)
                katzenpostClientManager.registerKatzenpostClient(katzenpostServerSettings, receiver)

                KatzenpostService.ensureForegroundService(context)
            }

            override fun refresh() {
                Timber.i("Refreshing Katzenpost client %s", katzenpostServerSettings.address)
                KatzenpostService.ensureForegroundService(context)
            }

            override fun stop() {
                Timber.i("Stopping Katzenpost client %s", katzenpostServerSettings.address)
                KatzenpostService.stopService(context)
            }

            override fun getRefreshInterval() = 60 * 1000

            override fun setLastRefresh(lastRefresh: Long) {
                lastRefreshValue = lastRefresh
            }

            override fun getLastRefresh() = lastRefreshValue

            fun currentTimeMillis() = System.currentTimeMillis()
        }
    }

    override fun checkIncomingServerSettings() {
        // check connection
    }

    override fun checkOutgoingServerSettings() {
        // check connection
    }

    override fun sendMessage(message: Message) {
        katzenpostClientManager.sendMessage(katzenpostServerSettings, message)
    }
}
