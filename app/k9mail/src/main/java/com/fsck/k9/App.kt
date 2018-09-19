package com.fsck.k9

import android.app.Application
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.backend.katzenpost.katzenpostModule
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.external.MessageProvider
import org.koin.android.ext.android.inject

class App : Application() {
    private val messagingController: MessagingController by inject()
    private val messagingListenerProvider: MessagingListenerProvider by inject()


    override fun onCreate() {
        Core.earlyInit(this)

        super.onCreate()

        DI.start(this, Core.coreModules + uiModules + appModules + katzenpostModule)

        K9.init(this)
        Core.init(this)
        MessageProvider.init()

        messagingListenerProvider.listeners.forEach { listener ->
            messagingController.addListener(listener)
        }
    }


    companion object {
        val appConfig = AppConfig(
                componentsToDisable = listOf(MessageCompose::class.java)
        )
    }
}
