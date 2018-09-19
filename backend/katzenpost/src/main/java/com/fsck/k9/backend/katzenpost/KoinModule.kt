package com.fsck.k9.backend.katzenpost

import com.fsck.k9.backend.katzenpost.service.KatzenpostClientManager
import org.koin.dsl.module.applicationContext

val katzenpostModule = applicationContext {
    bean { KatzenpostClientManager(get()) }
}
