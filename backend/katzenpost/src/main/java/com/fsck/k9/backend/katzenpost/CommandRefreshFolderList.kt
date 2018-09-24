package com.fsck.k9.backend.katzenpost


import com.fsck.k9.backend.api.BackendFolder
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.internet.MimeMessage
import java.util.*


internal class CommandRefreshFolderList(private val backendStorage: BackendStorage, private val address: String) {
    fun refreshFolderList() {
        val folderServerIds = backendStorage.getFolderServerIds()
        if ("INBOX" !in folderServerIds) {
            val inbox = FolderInfo("INBOX", "Inbox")
            backendStorage.createFolders(listOf(inbox))

            val backendFolder = backendStorage.getFolder("INBOX")
            backendFolder.setMoreMessages(BackendFolder.MoreMessages.FALSE)

            val welcomeMessage = createWelcomeMessage(address)
            backendFolder.saveCompleteMessage(welcomeMessage)
        }
    }

    private fun createWelcomeMessage(address: String): MimeMessage {
        val msg = """
            From: alice@idefix
            To: $address
            Subject: Welcome to Katzenpost!
            Content-Type: text/plain

            Hi, welcome to Katzenpost!

            This is an experimental messaging App that uses the Katzenpost mixnet. You can get an overview of this technology here:
            https://katzenpost.mixnetworks.org/

            Feel free to respond to this message, I'll reply back to you! Sending a message to a contact for the first time performs an anonymous handshake, which might take a while. It'll be fast after that!

            You can also follow some internals of the mixnet-nodes here:
            http://logviewer.katzenpost.mixnetworks.org/

            Cheers
              - Alice
        """.trimIndent()

        val mimeMessage = MimeMessage.parseMimeMessage(msg.byteInputStream(), false)
        mimeMessage.addSentDate(Date(), true)

        return mimeMessage
    }
}
