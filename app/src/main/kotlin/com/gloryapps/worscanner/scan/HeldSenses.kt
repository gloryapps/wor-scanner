package com.gloryapps.worscanner.scan

import com.gloryapps.worscanner.capture.CaptureSession
import com.gloryapps.worscanner.scanner.senses.Senses

/** The projection the capture service holds and the accessibility service while the system binds it. */
class HeldSenses(private val session: CaptureSession, private val touch: TouchState) : Senses {
    override val screen get() = checkNotNull(session.screen.value) { "no capture session" }
    override val hand get() = checkNotNull(touch.hand.value) { "accessibility service not bound" }
}
