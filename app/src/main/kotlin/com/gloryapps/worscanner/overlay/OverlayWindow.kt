package com.gloryapps.worscanner.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * A Compose view over every other app, owned by the service that shows it.
 *
 * Compose needs a lifecycle and a saved-state owner on the view tree; a service has neither, so
 * this window carries its own and walks them with show and hide.
 */
class OverlayWindow(private val context: Context) : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val manager = context.getSystemService(WindowManager::class.java)
    private var view: ComposeView? = null
    private val params = layout()

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    fun show() {
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@OverlayWindow)
            setViewTreeSavedStateRegistryOwner(this@OverlayWindow)
            setContent { OverlayContent(onDrag = ::moveBy) }
        }
        manager.addView(view, params)
        this.view = view
    }

    fun hide() {
        view?.let(manager::removeView)
        view = null
        registry.currentState = Lifecycle.State.DESTROYED
    }

    /** The window follows the finger on its handle; the params are the window's, not the view's. */
    private fun moveBy(dx: Float, dy: Float) {
        params.x += dx.toInt()
        params.y += dy.toInt()
        view?.let { manager.updateViewLayout(it, params) }
    }

    private fun layout() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }
}
