package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.katzenpost.KatzenpostBackend
import com.fsck.k9.backend.katzenpost.KatzenpostServerSettings
import com.fsck.k9.backend.katzenpost.KatzenpostStore
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.K9BackendStorage

class KatzenpostBackendFactory(private val context: Context, private val preferences: Preferences) : BackendFactory {
    override val transportUriPrefix = "katzenpost"

    override fun createBackend(account: Account): Backend {
        val accountName = account.displayName
        val settings = decodeStoreUri(account.getStoreUri())
        val backendStorage = K9BackendStorage(preferences, account, account.localStore)
        val katzenpostStore = KatzenpostStore(settings, account, context)

        return KatzenpostBackend(accountName, backendStorage, katzenpostStore)
    }

    override fun decodeStoreUri(storeUri: String) = KatzenpostUriParser.decode(storeUri)

    override fun createStoreUri(serverSettings: ServerSettings): String {
        serverSettings as KatzenpostServerSettings
        return "katzenpost:" + serverSettings.linkkey + ":" + serverSettings.idkey + ":" + serverSettings.username + "@" + serverSettings.provider
    }

    override fun decodeTransportUri(transportUri: String) = KatzenpostUriParser.decode(transportUri)

    override fun createTransportUri(serverSettings: ServerSettings): String {
        serverSettings as KatzenpostServerSettings
        return "katzenpost:" + serverSettings.linkkey + ":" + serverSettings.idkey + ":" + serverSettings.username + "@" + serverSettings.provider
    }

    companion object {
        private val PKI_ADDRESS = "37.218.242.147:29485"
        private val PKI_PINNED_PUBLIC_KEY = "DFD5E1A26E9B3EF7B3DA0102002B93C66FC36B12D14C608C3FBFCA03BF3EBCDC"
    }
}

private object KatzenpostUriParser {
    fun decode(uri: String): KatzenpostServerSettings {
        val (scheme, content) = uri.split(":", limit = 2)
        if (scheme != "katzenpost") {
            throw IllegalArgumentException("invalid uri!")
        }

        val (userinfo, provider) = content.split("@")
        val (linkkey, idkey, username) = userinfo.split(":")

        return KatzenpostServerSettings(provider, username, linkkey, idkey)
    }
}
