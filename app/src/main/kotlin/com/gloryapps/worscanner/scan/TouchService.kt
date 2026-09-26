package com.gloryapps.worscanner.scan

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import com.gloryapps.worscanner.scanner.senses.Touch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.android.ext.android.inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    override suspend fun tap(x: Int, y: Int) = stroke(Path().apply { moveTo(x.toFloat(), y.toFloat()) }, TAP_MILLIS)

    /* The finger stops and stays before it lifts, so the grid takes no fling from the release. */
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
        send(move)
        send(hold)
    }

    /** Android cancels an injected gesture the moment a finger touches the screen: the player took it back, and the scan ends as stopped. */
    private class TouchedByHand : CancellationException("a finger touched the screen")

    private suspend fun stroke(path: Path, millis: Long) = send(GestureDescription.StrokeDescription(path, 0, millis))

    private suspend fun send(stroke: GestureDescription.StrokeDescription) = suspendCancellableCoroutine { continuation ->
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        val sent = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (continuation.isActive) continuation.resumeWithException(TouchedByHand())
                }
            },
            null,
        )
        if (!sent) continuation.resumeWithException(IllegalStateException("gesture refused"))
    }

    private companion object {
        const val TAP_MILLIS = 60L
        const val HOLD_MILLIS = 200L
    }
}
