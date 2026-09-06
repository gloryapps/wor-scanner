package com.gloryapps.worscanner.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.math.hypot

/**
 * The Compose strip over every other app, owned by the service that shows it, and the close
 * target that appears under it while it is dragged.
 *
 * Compose needs a lifecycle and a saved-state owner on the view tree; a service has neither, so
 * this window carries its own. They run once: a window is shown once and hidden once, and the
 * service makes a new one to show the overlay again.
 */
class OverlayWindow(private val context: Context, private val onClose: () -> Unit) : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val manager = context.getSystemService(WindowManager::class.java)
    private var strip: ComposeView? = null
    private var target: ComposeView? = null
    private val params = layout(Gravity.TOP or Gravity.START)
    private val targetParams = layout(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { y = TARGET_MARGIN_PX }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    fun show() {
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
        strip = compose { OverlayContent(onDrag = ::moveBy, onDragStart = ::showTarget, onDragEnd = ::dropped) }
            .also { manager.addView(it, params) }
    }

    fun hide() {
        hideTarget()
        strip?.let(manager::removeView)
        strip = null
        registry.currentState = Lifecycle.State.DESTROYED
    }

    /** The window follows the finger on its handle; the params are the window's, not the view's. */
    private fun moveBy(dx: Float, dy: Float) {
        params.x += dx.toInt()
        params.y += dy.toInt()
        strip?.let { manager.updateViewLayout(it, params) }
    }

    private fun showTarget() {
        if (target != null) return
        target = compose { CloseTarget() }.also { manager.addView(it, targetParams) }
    }

    private fun hideTarget() {
        target?.let(manager::removeView)
        target = null
    }

    /*
     * Let go over the target, the strip is closed with the session, the way a bubble is. The finger
     * is on the grip, so the grip is what lands; both are measured where the screen shows them, since
     * the system places an overlay window under the status bar and the params do not say so.
     */
    private fun dropped() {
        val strip = strip
        val target = target
        hideTarget()
        if (strip == null || target == null) return
        val grip = strip.onScreen()
        val centre = target.onScreen()
        val distance = hypot(
            grip[0] + GRIP_CENTRE_PX - (centre[0] + target.width / 2.0),
            grip[1] + strip.height / 2.0 - (centre[1] + target.height / 2.0),
        )
        if (distance < TARGET_SIZE_PX) onClose()
    }

    private fun ComposeView.onScreen(): IntArray = IntArray(2).also(::getLocationOnScreen)

    private fun compose(content: @Composable () -> Unit) = ComposeView(context).apply {
        setViewTreeLifecycleOwner(this@OverlayWindow)
        setViewTreeSavedStateRegistryOwner(this@OverlayWindow)
        setContent(content)
    }

    private fun layout(gravity: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply { this.gravity = gravity }

    private val TARGET_MARGIN_PX get() = (48 * context.resources.displayMetrics.density).toInt()
    /** The strip's padding plus half the grip icon, in `OverlayContent`'s own dp. */
    private val GRIP_CENTRE_PX get() = (20 * context.resources.displayMetrics.density).toInt()
    private val TARGET_SIZE_PX get() = (CloseTarget.SIZE_DP * context.resources.displayMetrics.density).toInt()
}
