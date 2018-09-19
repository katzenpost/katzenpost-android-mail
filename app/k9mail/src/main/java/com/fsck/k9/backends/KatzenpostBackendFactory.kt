package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.katzenpost.KatzenpostBackend
import com.fsck.k9.backend.katzenpost.KatzenpostServerSettings
import com.fsck.k9.backend.katzenpost.service.KatzenpostClientManager
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.K9BackendStorage

class KatzenpostBackendFactory(
        private val context: Context,
        private val preferences: Preferences,
        private val katzenpostClientManager: KatzenpostClientManager
) : BackendFactory {
    override val transportUriPrefix = "katzenpost"

    override fun createBackend(account: Account): Backend {
        val accountName = account.displayName
        val settings = decodeStoreUri(account.getStoreUri())
        val backendStorage = K9BackendStorage(preferences, account, account.localStore)

        return KatzenpostBackend(context, accountName, backendStorage, settings, katzenpostClientManager)
    }

    override fun decodeStoreUri(storeUri: String) = KatzenpostUriParser.decode(storeUri)

    override fun createStoreUri(serverSettings: ServerSettings): String {
        serverSettings as KatzenpostServerSettings
        return "katzenpost:" + serverSettings.linkkey + ":" + serverSettings.idkey + ":" + serverSettings.name + "@" + serverSettings.provider
    }

    override fun decodeTransportUri(transportUri: String) = KatzenpostUriParser.decode(transportUri)

    override fun createTransportUri(serverSettings: ServerSettings): String {
        serverSettings as KatzenpostServerSettings
        return "katzenpost:" + serverSettings.linkkey + ":" + serverSettings.idkey + ":" + serverSettings.name + "@" + serverSettings.provider
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
