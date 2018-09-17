package com.fsck.k9.backends

import com.fsck.k9.backend.BackendManager
import org.koin.dsl.module.applicationContext

val backendsModule = applicationContext {
    bean {
        BackendManager(
                mapOf(
                        "imap" to get<ImapBackendFactory>(),
                        "pop3" to get<Pop3BackendFactory>(),
                        "webdav" to get<WebDavBackendFactory>(),
                        "katzenpost" to get<KatzenpostBackendFactory>()
                ))
    }
    bean { ImapBackendFactory(get(), get(), get()) }
    bean { Pop3BackendFactory(get(), get()) }
    bean { WebDavBackendFactory(get()) }
    bean { KatzenpostBackendFactory(get(), get()) }
}
