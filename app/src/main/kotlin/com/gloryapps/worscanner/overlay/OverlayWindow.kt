package com.gloryapps.worscanner.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gloryapps.worscanner.ui.ScannerTheme
import kotlin.math.hypot

/**
 * What the service draws over every other app: the progress hairline pinned to the top, the capsule
 * the finger moves, and the close target that appears under it while it is held.
 *
 * Compose needs a lifecycle and a saved-state owner on the view tree; a service has neither, so
 * this window carries its own. They run once: a window is shown once and hidden once, and the
 * service makes a new one to show the overlay again. Its windows ask for hardware drawing, which a
 * service's are not given by default and which Compose's layers need below Android 10.
 */
class OverlayWindow(private val context: Context, private val onClose: () -> Unit) : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val manager = context.getSystemService(WindowManager::class.java)
    private var hairline: ComposeView? = null
    private var capsule: ComposeView? = null
    private var sheet: ComposeView? = null
    private var target: ComposeView? = null
    private val over = mutableStateOf(false)
    private val params = layout(Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { y = INSET_PX - PAD }
    private val hairlineParams = layout(Gravity.TOP, touchable = false).apply { width = WindowManager.LayoutParams.MATCH_PARENT }
    private val targetParams = layout(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { y = TARGET_MARGIN }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    fun show() {
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
        hairline = compose { HairlineContent() }.also { manager.addView(it, hairlineParams) }
        capsule = compose {
            OverlayContent(onOpen = ::toggleSheet, onDrag = ::moveBy, onDragStart = ::held, onDragEnd = ::dropped)
        }.also { manager.addView(it, params) }
    }

    fun hide() {
        hideTarget()
        hideSheet()
        capsule?.let(manager::removeView)
        capsule = null
        hairline?.let(manager::removeView)
        hairline = null
        registry.currentState = Lifecycle.State.DESTROYED
    }

    private fun held() {
        hideSheet()
        pin()
        showTarget()
    }

    /** The capsule is parked centred until something takes hold of it; from then on it is placed by its corner. */
    private fun pin() {
        val capsule = capsule ?: return
        if (params.gravity == (Gravity.TOP or Gravity.START)) return
        val at = capsule.onScreen()
        params.gravity = Gravity.TOP or Gravity.START
        params.x = at[0]
        params.y = at[1]
        manager.updateViewLayout(capsule, params)
    }

    private fun toggleSheet() {
        if (sheet != null) hideSheet() else showSheet()
    }

    /**
     * The sheet is a window of its own, laid beside the capsule on whichever side of the screen has
     * the room. Drawing it inside the capsule's window would resize that window under the finger
     * that opened it, which is a flicker.
     */
    private fun showSheet() {
        val capsule = capsule ?: return
        pin()
        val height = context.resources.displayMetrics.heightPixels
        val below = params.y + capsule.height / 2 < height / 2
        val beside = layout(if (below) Gravity.TOP or Gravity.START else Gravity.BOTTOM or Gravity.START).apply {
            x = params.x.coerceIn(INSET_PX, (context.resources.displayMetrics.widthPixels - SHEET_WIDTH - INSET_PX).coerceAtLeast(INSET_PX))
            y = if (below) params.y + capsule.height - PAD + GAP else height - params.y - PAD + GAP
        }
        sheet = compose { SheetContent(onDone = ::hideSheet) }.also { manager.addView(it, beside) }
    }

    private fun hideSheet() {
        sheet?.let(manager::removeView)
        sheet = null
    }

    /** The window follows the finger on the capsule; the params are the window's, not the view's. */
    private fun moveBy(dx: Float, dy: Float) {
        params.x += dx.toInt()
        params.y += dy.toInt()
        capsule?.let { manager.updateViewLayout(it, params) }
        over.value = nearTarget()
    }

    private fun showTarget() {
        if (target != null) return
        target = compose { CloseTarget(over.value) }.also { manager.addView(it, targetParams) }
    }

    private fun hideTarget() {
        target?.let(manager::removeView)
        target = null
        over.value = false
    }

    /*
     * Let go over the target, the capsule is closed with the session, the way a bubble is; let go
     * anywhere else, it stays there. Both are measured where the screen shows them, since the system
     * places an overlay window under the status bar and the params do not say so.
     */
    private fun dropped() {
        val near = nearTarget()
        hideTarget()
        if (near) onClose()
    }

    private fun nearTarget(): Boolean {
        val capsule = capsule ?: return false
        val target = target ?: return false
        val centre = target.onScreen()
        val held = capsule.onScreen()
        val distance = hypot(
            held[0] + capsule.width / 2.0 - (centre[0] + target.width / 2.0),
            held[1] + capsule.height / 2.0 - (centre[1] + target.height / 2.0),
        )

        return distance < TARGET_SIZE
    }

    private fun ComposeView.onScreen(): IntArray = IntArray(2).also(::getLocationOnScreen)

    private fun compose(content: @Composable () -> Unit) = ComposeView(context).apply {
        setViewTreeLifecycleOwner(this@OverlayWindow)
        setViewTreeSavedStateRegistryOwner(this@OverlayWindow)
        setContent { ScannerTheme(content) }
    }

    private fun layout(gravity: Int, touchable: Boolean = true) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        PixelFormat.TRANSLUCENT,
    ).apply { this.gravity = gravity }

    private val INSET_PX get() = (INSET * context.resources.displayMetrics.density).toInt()
    private val PAD get() = ((TARGET.value - IDLE.value) / 2 * context.resources.displayMetrics.density).toInt()
    private val GAP get() = (6 * context.resources.displayMetrics.density).toInt()
    private val SHEET_WIDTH get() = (SHEET.value * context.resources.displayMetrics.density).toInt()
    /** How far in from the screen's edges the capsule starts and the sheet is kept. */
    private val INSET = 34
    private val TARGET_MARGIN get() = (30 * context.resources.displayMetrics.density).toInt()
    private val TARGET_SIZE get() = (CloseTarget.SIZE_DP * context.resources.displayMetrics.density).toInt()
}
