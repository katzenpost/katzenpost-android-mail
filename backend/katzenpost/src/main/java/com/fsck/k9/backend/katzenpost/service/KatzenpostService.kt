package com.fsck.k9.backend.katzenpost.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import com.fsck.k9.backend.katzenpost.R
import com.fsck.k9.ui.misc.KatzenpostLogViewer
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
        val notification = getNotification()
        startForeground(1234567, notification)

        katzenpostClientManager.refreshAll()
    }

    private fun stopAllClients() {
        katzenpostClientManager.stopAll()

        stopForeground(true)
        stopSelf()
    }

    private fun getNotification(): Notification {
        val logViewIntent = Intent(this, KatzenpostLogViewer::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val logViewPendingIntent = PendingIntent.getActivity(
                applicationContext, 0, logViewIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val closeServiceIntent = Intent(this, KatzenpostService::class.java).apply {
            action = "stop"
        }
        val closeServicePendingIntent = PendingIntent.getService(
                applicationContext, 0, closeServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this).apply {
            setSmallIcon(R.drawable.ic_katzenpost)
            setContentTitle("Katzenpost")
            setContentText("Mixin' it up…")
            priority = NotificationCompat.PRIORITY_DEFAULT
            addAction(0, "View Log", logViewPendingIntent)
            addAction(0, "Close", closeServicePendingIntent)
            setContentIntent(logViewPendingIntent)
        }.build()
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
