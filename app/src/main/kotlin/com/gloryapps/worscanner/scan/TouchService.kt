package com.gloryapps.worscanner.scan

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.gloryapps.worscanner.scanner.senses.Touch
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.android.ext.android.inject
import java.util.concurrent.atomic.AtomicReference

/** The hand: taps and drags over the game by gesture, since a Unity screen shows it no views. */
class TouchService : AccessibilityService(), Touch {
    private val state: TouchState by inject()

    override fun onServiceConnected() {
        state.bound(this)
    }

    override fun onDestroy() {
        state.unbound()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    /* A touch of the screen cancels the gesture under way: the tap goes again once the touch has passed, so a touch never stops a scan; Stop does. */
    override suspend fun tap(x: Int, y: Int) {
        while (!send(GestureDescription.StrokeDescription(Path().apply { moveTo(x.toFloat(), y.toFloat()) }, 0, TAP_MILLIS))) delay(AGAIN_MILLIS)
    }

    /* The finger stops and stays before it lifts, so the grid takes no fling from the release. A drag a touch cut short is left as it went: the walk measures where the grid stopped. */
    override suspend fun drag(fromX: Int, fromY: Int, toX: Int, toY: Int, millis: Long) {
        val move = GestureDescription.StrokeDescription(
            Path().apply {
                moveTo(fromX.toFloat(), fromY.toFloat())
                lineTo(toX.toFloat(), toY.toFloat())
            },
            0,
            millis,
            true,
        )
        val hold = move.continueStroke(Path().apply { moveTo(toX.toFloat(), toY.toFloat()) }, 0, HOLD_MILLIS, false)
        if (send(move)) send(hold)
    }

    /** Whether the gesture ran to its end; false where Android cancelled it, a finger having touched the screen. */
    private suspend fun send(stroke: GestureDescription.StrokeDescription): Boolean = suspendCancellableCoroutine { continuation ->
        val ended = GestureEnded(continuation)
        continuation.invokeOnCancellation { ended.waiting.set(null) }
        if (!dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), ended, null)) ended.resume(Result.failure(IllegalStateException("gesture refused")))
    }

    /** Android 9 keeps every callback a gesture was dispatched with for as long as the service lives: this one lets go of the scan, and the frames it holds, once the gesture ends. */
    private class GestureEnded(continuation: CancellableContinuation<Boolean>) : GestureResultCallback() {
        val waiting = AtomicReference(continuation)

        override fun onCompleted(gestureDescription: GestureDescription?) = resume(Result.success(true))

        override fun onCancelled(gestureDescription: GestureDescription?) = resume(Result.success(false))

        fun resume(result: Result<Boolean>) {
            waiting.getAndSet(null)?.takeIf { it.isActive }?.resumeWith(result)
        }
    }

    private companion object {
        const val TAP_MILLIS = 60L
        const val HOLD_MILLIS = 200L
        /** Long enough for a touch to have lifted before the tap goes again. */
        const val AGAIN_MILLIS = 300L
    }
}
