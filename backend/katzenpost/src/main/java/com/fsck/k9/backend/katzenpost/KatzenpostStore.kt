package com.fsck.k9.backend.katzenpost

import android.content.Context
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.store.RemoteStore
import com.fsck.k9.mail.store.StoreConfig
import java.io.File


class KatzenpostStore(private val settings: KatzenpostServerSettings, storeConfig: StoreConfig, context: Context) : RemoteStore(storeConfig, null) {
    private val cacheDir: File

    init {
        cacheDir = File(context.cacheDir, "katzencache")
    }

    @Throws(MessagingException::class)
    override fun checkSettings() {

    }

    override fun getPersonalNamespaces(): MutableList<out Folder<Message>> {
        return mutableListOf()
    }

    override fun getFolder(name: String): Folder<*> {
        return DummyFolder()
    }

    @Throws(MessagingException::class)
    override fun sendMessages(messages: List<com.fsck.k9.mail.Message>) {
        for (message in messages) {
            //            sendMessage(message);
        }
    }
}

class KatzenpostServerSettings(val provider: String, username: String, val linkkey: String, idkey: String) : ServerSettings("katzenpost") {
    val idkey: String

    val address: String
        get() = "$username@$provider"

    init {
        this.idkey = linkkey
    }
}
