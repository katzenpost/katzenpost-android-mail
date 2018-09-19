package com.fsck.k9.backend.katzenpost

import android.content.Context
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.SyncConfig
import com.fsck.k9.backend.api.SyncListener
import com.fsck.k9.backend.katzenpost.service.KatzenpostService
import com.fsck.k9.mail.*

class KatzenpostBackend(
        private val context: Context,
        private val accountUuid: String,
        backendStorage: BackendStorage,
        private val katzenpostServerSettings: KatzenpostServerSettings
) : Backend {
    // private val katzenpostSync: KatzenpostSync = KatzenpostSync(accountUuid, backendStorage, katzenpostStore)
    private val commandRefreshFolderList = CommandRefreshFolderList(backendStorage)

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
        KatzenpostService.ensureService(context, katzenpostServerSettings)
        // katzenpostSync.sync(folder, syncConfig, listener)
    }

    override fun downloadMessage(syncConfig: SyncConfig, folderServerId: String, messageServerId: String) {
        throw UnsupportedOperationException("not implemented")
    }

    override fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        throw UnsupportedOperationException("not supported")
    }

    override fun markAllAsRead(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expunge(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        throw UnsupportedOperationException("not supported")
    }

    override fun deleteAllMessages(folderServerId: String) {
        throw UnsupportedOperationException("not supported")
    }

    override fun moveMessages(
            sourceFolderServerId: String,
            targetFolderServerId: String,
            messageServerIds: List<String>
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun copyMessages(
            sourceFolderServerId: String,
            targetFolderServerId: String,
            messageServerIds: List<String>
    ): Map<String, String>? {
        throw UnsupportedOperationException("not supported")
    }

    override fun search(
            folderServerId: String,
            query: String?,
            requiredFlags: Set<Flag>?,
            forbiddenFlags: Set<Flag>?
    ): List<String> {
        throw UnsupportedOperationException("not supported")
    }

    override fun fetchMessage(folderServerId: String, messageServerId: String, fetchProfile: FetchProfile): Message {
        throw UnsupportedOperationException("not supported")
        // return commandFetchMessage.fetchMessage(folderServerId, messageServerId, fetchProfile)
    }

    override fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        throw UnsupportedOperationException("not supported")
    }

    override fun findByMessageId(folderServerId: String, messageId: String): String? {
        return null
    }

    override fun uploadMessage(folderServerId: String, message: Message): String? {
        throw UnsupportedOperationException("not supported")
    }

    override fun createPusher(receiver: PushReceiver): Pusher {
        return object : Pusher {
            var lastRefreshValue: Long = 0

            override fun start(folderServerIds: MutableList<String>?) {
                KatzenpostService.ensureService(context, katzenpostServerSettings)
            }

            override fun refresh() {
                KatzenpostService.ensureService(context, katzenpostServerSettings)
            }

            override fun stop() {
                KatzenpostService.stopService(context)
            }

            override fun getRefreshInterval() = 60

            override fun setLastRefresh(lastRefresh: Long) {
                lastRefreshValue = lastRefresh
            }

            override fun getLastRefresh() = lastRefreshValue
        }
    }

    override fun checkIncomingServerSettings() {
        // check connection
    }

    override fun checkOutgoingServerSettings() {
        // check connection
    }

    override fun sendMessage(message: Message) {
        KatzenpostService.sendMessage(context, katzenpostServerSettings, message)
    }
}
