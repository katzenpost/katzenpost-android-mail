package com.fsck.k9.backend.katzenpost

import android.content.Context
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.mail.store.StoreConfig
import java.io.File


class KatzenpostStore(
        private val settings: KatzenpostServerSettings,
        storeConfig: StoreConfig,
        context: Context
) : RemoteStore(storeConfig, null) {
    private val cacheDir: File

    init {
        cacheDir = File(context.cacheDir, "katzencache")
    }

    @Throws(MessagingException::class)
    override fun checkSettings() {

    }

    override fun getPersonalNamespaces(): MutableList<out Folder<Message>> {
        return FOLDERS.map(this::getFolder).toMutableList()
    }

    override fun getFolder(name: String): Folder<Message> {
        return DummyFolder(name)
    }

    @Throws(MessagingException::class)
    override fun sendMessages(messages: List<com.fsck.k9.mail.Message>) {
        for (message in messages) {
            //            sendMessage(message);
        }
    }

    companion object {
        val FOLDERS = listOf("INBOX")
    }
}

class KatzenpostServerSettings(val provider: String, val name: String, val linkkey: String, val idkey: String) : ServerSettings("katzenpost") {
    val address: String
        get() = "$name@$provider"
}
