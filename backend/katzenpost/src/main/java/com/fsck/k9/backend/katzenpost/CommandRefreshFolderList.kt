package com.fsck.k9.backend.katzenpost


import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo


internal class CommandRefreshFolderList(private val backendStorage: BackendStorage) {
    fun refreshFolderList() {
        val folderServerIds = backendStorage.getFolderServerIds()
        if ("INBOX" !in folderServerIds) {
            val inbox = FolderInfo("INBOX", "Inbox")
            backendStorage.createFolders(listOf(inbox))

            val backendFolder = backendStorage.getFolder("INBOX")
            backendFolder.setMoreMessages(BackendFolder.MoreMessages.FALSE)
        }
    }
}
