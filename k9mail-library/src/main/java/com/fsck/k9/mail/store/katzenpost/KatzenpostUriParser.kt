package com.fsck.k9.mail.store.katzenpost

import android.net.Uri


internal object KatzenpostUriParser {
    fun decode(uriString: String): KatzenpostServerSettings {
        val uri = Uri.parse(uriString)

        if (uri.scheme != "katzenpost") {
            throw IllegalArgumentException("invalid uri!")
        }

        val (username, linkkey) = uri.userInfo.split(":")

        return KatzenpostServerSettings(uri.host, username, linkkey)
    }
}
