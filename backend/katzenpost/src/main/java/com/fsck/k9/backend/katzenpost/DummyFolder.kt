package com.fsck.k9.backend.katzenpost


import java.io.IOException
import java.util.Date

import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessageRetrievalListener
import com.fsck.k9.mail.MessagingException


class DummyFolder(val folderName: String) : Folder<Message>() {
    var openMode: Int? = null

    override fun getName() = folderName

    @Throws(MessagingException::class)
    override fun open(mode: Int) {
        openMode = mode
    }

    override fun close() {
        openMode = null
    }

    override fun isOpen() = openMode != null

    override fun getMode(): Int {
        return openMode ?: 0
    }

    @Throws(MessagingException::class)
    override fun create(type: Folder.FolderType): Boolean {
        return true
    }

    @Throws(MessagingException::class)
    override fun exists(): Boolean {
        return true
    }

    @Throws(MessagingException::class)
    override fun getMessageCount(): Int {
        return 0
    }

    @Throws(MessagingException::class)
    override fun getUnreadMessageCount(): Int {
        return 0
    }

    @Throws(MessagingException::class)
    override fun getFlaggedMessageCount(): Int {
        return 0
    }

    @Throws(MessagingException::class)
    override fun getMessage(uid: String): Message? {
        return null
    }

    @Throws(IOException::class, MessagingException::class)
    override fun areMoreMessagesAvailable(indexOfOldestMessage: Int, earliestDate: Date): Boolean {
        return false
    }

    @Throws(MessagingException::class)
    override fun appendMessages(messages: List<Message>): Map<String, String>? {
        return null
    }

    @Throws(MessagingException::class)
    override fun setFlags(messages: List<Message>, flags: Set<Flag>, value: Boolean) {

    }

    @Throws(MessagingException::class)
    override fun setFlags(flags: Set<Flag>, value: Boolean) {

    }

    @Throws(MessagingException::class)
    override fun getUidFromMessageId(messageId: String): String? {
        return null
    }

    @Throws(MessagingException::class)
    override fun delete(recurse: Boolean) {

    }

    override fun getServerId(): String? {
        return null
    }

    @Throws(MessagingException::class)
    override fun fetch(messages: List<Message>, fp: FetchProfile, listener: MessageRetrievalListener<Message>) {

    }

    @Throws(MessagingException::class)
    override fun getMessages(start: Int, end: Int, earliestDate: Date, listener: MessageRetrievalListener<Message>): List<Message>? {
        return emptyList()
    }
}
