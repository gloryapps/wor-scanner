package com.gloryapps.worscanner.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import com.gloryapps.worscanner.scanner.runs.ReadScreen
import com.gloryapps.worscanner.ui.said
import com.gloryapps.worscanner.scanner.runs.ScanState
import com.gloryapps.worscanner.scanner.runs.Scanning
import com.gloryapps.worscanner.scan.TouchState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.gloryapps.worscanner.ui.resources.Res
import com.gloryapps.worscanner.ui.resources.stop
import org.jetbrains.compose.resources.getString
import org.koin.android.ext.android.inject

/**
 * Holds the projection for as long as a scan can run: the Activity is gone once the game is in
 * front, and Android hands a projection only to a foreground service.
 */
class CaptureService : LifecycleService() {
    private val session: CaptureSession by inject()
    private val scanning: Scanning by inject()
    private val touch: TouchState by inject()
    private val readScreen: ReadScreen by inject()
    private var screen: ProjectionScreen? = null
    private var overlay: OverlayWindow? = null
    private var scan: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> stopSelf()
            ACTION_SCAN -> startScan(intent.kind())
            ACTION_STOP_SCAN -> scan?.cancel()
            ACTION_READ -> read(intent.kind())
            else -> intent?.consent()?.let(::open) ?: stopSelf()
        }

        return START_NOT_STICKY
    }

    /* The game turns the phone to landscape once it is in front; the mirror must show that, not the app's portrait. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val metrics = realMetrics()
        screen?.resize(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
    }

    override fun onDestroy() {
        scan?.cancel()
        hideOverlay()
        session.closed()
        screen?.close()
        super.onDestroy()
    }

    private fun open(consent: Consent) {
        /* One projection at a time: a second would stop this one, whose stop ends the service. */
        if (screen != null) return
        goForeground(notification(getString(R.string.capture_notification_title)))
        val projection = getSystemService(MediaProjectionManager::class.java)
            .getMediaProjection(consent.resultCode, consent.data)
        if (projection == null) {
            stopSelf()
            return
        }
        val metrics = realMetrics()
        screen = ProjectionScreen(projection, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, onStop = ::stopSelf)
            .also(session::opened)
        scanning.idle()
        lifecycleScope.launch { scanning.state.onEach(::show).collect() }
        showOverlay()
    }

    private fun showOverlay() {
        overlay = OverlayWindow(this, onClose = ::stopSelf).also { it.show() }
    }

    private fun hideOverlay() {
        overlay?.hide()
        overlay = null
    }

    /* The overlay stays, showing the scan and its stop; it is captured with the game, so the capsule is to be kept off the grid and the panel. */
    private fun startScan(kind: Kind) {
        /* Until a stopped scan has written what it read, a tap still stops it rather than starting another. */
        if (scan?.isCompleted == false) {
            scan?.cancel()
            return
        }
        /* A service reborn after its process died has no projection: there is nothing to scan with. */
        if (session.screen.value == null) {
            stopSelf()
            return
        }
        if (touch.hand.value == null) {
            Toast.makeText(this, R.string.scan_needs_touch, Toast.LENGTH_LONG).show()
            return
        }
        /* The walk reads pixels and waits on the recogniser; it runs off the thread the overlay draws on. */
        scan = lifecycleScope.launch(Dispatchers.Default) {
            sheetLeaves()
            scanning.run(kind)
        }
    }

    /* Read here, not in the sheet that asked: the sheet closes on the tap and takes its coroutines with it. */
    private fun read(kind: Kind) {
        lifecycleScope.launch {
            sheetLeaves()
            val said = withContext(Dispatchers.Default) { readScreen.now(kind) }.said()
            Toast.makeText(this@CaptureService, said, Toast.LENGTH_LONG).show()
        }
    }

    /* The sheet that asked closes on the same tap, but stays on the display a frame or two, over what is read first. */
    private suspend fun sheetLeaves() = delay(SHEET_LEAVES_MS)

    private suspend fun show(state: ScanState) {
        val text = state.said() ?: getString(R.string.capture_notification_title)
        val stop = if (state is ScanState.Running) getString(Res.string.stop) else null
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(text, stop))
        /* The end of a scan is said out loud too: the notification is easy to miss under a game. */
        if (state is ScanState.Ended) Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }

    /** `stop` is the stop action's label, where the scan under way can be stopped from the notification. */
    private fun notification(text: String, stop: String? = null): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
        if (stop != null) {
            val stopping = PendingIntent.getService(this, 1, Intent(this, CaptureService::class.java).setAction(ACTION_STOP_SCAN), PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, stop, stopping)
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

    private fun Intent.kind(): Kind = Kind.valueOf(checkNotNull(getStringExtra(EXTRA_KIND)) { "no kind named" })

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
        private const val ACTION_READ = "com.gloryapps.worscanner.READ"
        private const val EXTRA_RESULT_CODE = "resultCode"
        private const val EXTRA_RESULT_DATA = "resultData"
        private const val EXTRA_KIND = "kind"
        private const val SHEET_LEAVES_MS = 250L

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

        /** Scans the kind, or stops the scan under way: the menu's one Scan line does both. */
        fun scan(context: Context, kind: Kind) = send(context, ACTION_SCAN) { putExtra(EXTRA_KIND, kind.name) }

        /** Reads the frame on screen as one kind and says what it kept. */
        fun read(context: Context, kind: Kind) = send(context, ACTION_READ) { putExtra(EXTRA_KIND, kind.name) }

        fun stopScan(context: Context) = send(context, ACTION_STOP_SCAN)

        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String, extras: Intent.() -> Intent = { this }) {
            context.startService(Intent(context, CaptureService::class.java).setAction(action).extras())
        }
    }
}
