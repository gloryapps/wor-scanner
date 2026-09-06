package com.gloryapps.worscanner.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.app.MainActivity
import com.gloryapps.worscanner.overlay.OverlayWindow
import org.koin.android.ext.android.inject

/**
 * Holds the projection for as long as a scan can run: the Activity is gone once the game is in
 * front, and Android hands a projection only to a foreground service.
 */
class CaptureService : LifecycleService() {
    private val session: CaptureSession by inject()
    private var screen: ProjectionScreen? = null
    private var overlay: OverlayWindow? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val consent = intent?.consent()
        if (intent?.action == ACTION_STOP || consent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        goForeground()
        val projection = getSystemService(MediaProjectionManager::class.java)
            .getMediaProjection(consent.resultCode, consent.data)
        if (projection == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val metrics = realMetrics()
        screen = ProjectionScreen(projection, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
            .also(session::opened)
        overlay = OverlayWindow(this).also { it.show() }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        overlay?.hide()
        session.closed()
        screen?.close()
        super.onDestroy()
    }

    private fun goForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.capture_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentIntent(open)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Suppress("DEPRECATION")
    private fun realMetrics(): DisplayMetrics = DisplayMetrics().also {
        getSystemService(WindowManager::class.java).defaultDisplay.getRealMetrics(it)
    }

    private class Consent(val resultCode: Int, val data: Intent)

    private fun Intent.consent(): Consent? {
        val code = getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
        val data = extraIntent(EXTRA_RESULT_DATA)

        return if (code == Int.MIN_VALUE || data == null) null else Consent(code, data)
    }

    @Suppress("DEPRECATION")
    private fun Intent.extraIntent(name: String): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, Intent::class.java)
        } else {
            getParcelableExtra(name)
        }

    companion object {
        private const val CHANNEL = "capture"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.gloryapps.worscanner.STOP"
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_RESULT_DATA = "resultData"

        /** Starts the service with the consent the projection dialog returned. */
        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, CaptureService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, CaptureService::class.java).setAction(ACTION_STOP))
        }
    }
}
