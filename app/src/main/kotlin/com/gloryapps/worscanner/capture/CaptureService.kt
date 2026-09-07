package com.gloryapps.worscanner.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gloryapps.worscanner.R
import com.gloryapps.worscanner.app.MainActivity
import com.gloryapps.worscanner.overlay.OverlayWindow
import com.gloryapps.worscanner.scanner.kinds.Kind
import com.gloryapps.worscanner.scanner.scan.Outcome
import com.gloryapps.worscanner.ui.label
import com.gloryapps.worscanner.scan.ScanState
import com.gloryapps.worscanner.scan.Scanning
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Holds the projection for as long as a scan can run: the Activity is gone once the game is in
 * front, and Android hands a projection only to a foreground service.
 */
class CaptureService : LifecycleService() {
    private val session: CaptureSession by inject()
    private val scanning: Scanning by inject()
    private var screen: ProjectionScreen? = null
    private var overlay: OverlayWindow? = null
    private var scan: Job? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch { scanning.state.onEach(::show).collect() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SCAN -> startScan(Kind.valueOf(checkNotNull(intent.getStringExtra(EXTRA_KIND)) { "no kind to scan" }))
            ACTION_STOP_SCAN -> scan?.cancel()
            else -> intent?.consent()?.let(::open) ?: stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scan?.cancel()
        hideOverlay()
        session.closed()
        screen?.close()
        super.onDestroy()
    }

    private fun open(consent: Consent) {
        goForeground(notification(getString(R.string.capture_notification_title)))
        val projection = getSystemService(MediaProjectionManager::class.java)
            .getMediaProjection(consent.resultCode, consent.data)
        if (projection == null) {
            stopSelf()
            return
        }
        val metrics = realMetrics()
        screen = ProjectionScreen(projection, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
            .also(session::opened)
        showOverlay()
    }

    private fun showOverlay() {
        overlay = OverlayWindow(this, onClose = ::stopSelf).also { it.show() }
    }

    private fun hideOverlay() {
        overlay?.hide()
        overlay = null
    }

    /* The overlay stays, showing the scan and its stop; the scan reads only the regions it knows, and the strip sits elsewhere. */
    private fun startScan(kind: Kind) {
        if (scan?.isActive == true) return
        scan = lifecycleScope.launch { scanning.run(kind) }
    }

    private fun show(state: ScanState) {
        val text = when (state) {
            ScanState.Idle -> getString(R.string.capture_notification_title)
            is ScanState.Running -> getString(R.string.scan_running, getString(state.kind.label), state.progress.done, state.progress.held)
            is ScanState.Ended -> when (val outcome = state.outcome) {
                is Outcome.Finished<*> -> getString(R.string.scan_finished, getString(state.kind.label), outcome.entries.size)
                is Outcome.Stopped<*> -> getString(R.string.scan_stopped, getString(state.kind.label), outcome.detail, outcome.entries.size)
                is Outcome.Failed<*> -> getString(R.string.scan_failed, getString(state.kind.label), outcome.cause.toString())
            }
        }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text, stoppable = state is ScanState.Running))
        /* The end of a scan is said out loud too: the notification is easy to miss under a game. */
        if (state is ScanState.Ended) Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    private fun notification(text: String, stoppable: Boolean = false): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
        if (stoppable) {
            val stop = PendingIntent.getService(this, 1, Intent(this, CaptureService::class.java).setAction(ACTION_STOP_SCAN), PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, getString(R.string.scan_stop), stop)
        }

        return builder.build()
    }

    private fun goForeground(notification: Notification) {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.capture_channel), NotificationManager.IMPORTANCE_LOW),
        )
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
        private const val ACTION_SCAN = "com.gloryapps.worscanner.SCAN"
        private const val ACTION_STOP_SCAN = "com.gloryapps.worscanner.STOP_SCAN"
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_RESULT_DATA = "resultData"
        private const val EXTRA_KIND = "kind"

        /** Starts the service with the consent the projection dialog returned. */
        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, CaptureService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_RESULT_DATA, data)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun scan(context: Context, kind: Kind) = send(context, ACTION_SCAN) { putExtra(EXTRA_KIND, kind.name) }

        fun stopScan(context: Context) = send(context, ACTION_STOP_SCAN)

        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String, extras: Intent.() -> Intent = { this }) {
            context.startService(Intent(context, CaptureService::class.java).setAction(action).extras())
        }
    }
}
