package com.fsck.k9.backend.katzenpost.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.fsck.k9.backend.katzenpost.R
import org.koin.android.ext.android.inject
import timber.log.Timber

class KatzenpostService : Service() {
    val katzenpostClientManager: KatzenpostClientManager by inject()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("KatzenpostService Intent: %s", intent)
        when (intent?.action) {
            "refresh" -> refreshAllClients()
            "stop" -> stopAllClients()
        }

        return START_STICKY
    }

    private fun refreshAllClients() {
        katzenpostClientManager.refreshAll()

        val notification = getNotification()
        startForeground(1234567, notification)
    }

    private fun stopAllClients() {
        katzenpostClientManager.stopAll()

        stopForeground(true)
        stopSelf()
    }

    private fun getNotification(): Notification {
        val builder = NotificationCompat.Builder(this)
        builder.setSmallIcon(R.drawable.ic_katzenpost)
        builder.setContentTitle("Katzenpost")
        builder.setContentText("Mixin' it up…")
        builder.priority = NotificationCompat.PRIORITY_MIN
        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun ensureForegroundService(context: Context) {
            val intent = Intent(context, KatzenpostService::class.java)
            intent.action = "refresh"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KatzenpostService::class.java)
            intent.action = "stop"
            context.startService(intent)
        }
    }
}
