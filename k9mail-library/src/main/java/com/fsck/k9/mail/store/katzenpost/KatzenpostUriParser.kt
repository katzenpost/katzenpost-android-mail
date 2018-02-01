package com.fsck.k9.mail.store.katzenpost


internal object KatzenpostUriParser {
    fun decode(uri: String): KatzenpostServerSettings {
        val (scheme, content) = uri.split(":", limit = 2)
        if (scheme != "katzenpost") {
            throw IllegalArgumentException("invalid uri!")
        }

        val (userinfo, provider) = content.split("@")
        val (keys, username) = userinfo.split(":")
        val (linkkey, idkey) = keys.split("/")

        return KatzenpostServerSettings(provider, username, linkkey, idkey)
    }
}
