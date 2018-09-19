package com.fsck.k9.backend.katzenpost


import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo


internal class CommandRefreshFolderList(private val backendStorage: BackendStorage) {
    fun refreshFolderList() {
        val folderServerIds = backendStorage.getFolderServerIds()
        if ("INBOX" !in folderServerIds) {
            val inbox = FolderInfo("INBOX", "INBOX")
            backendStorage.createFolders(listOf(inbox))
        }
    }
}
