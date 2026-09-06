package com.gloryapps.worscanner.scanner

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ResultOfTest {
    @Test
    fun `a value comes back as success`() {
        assertEquals(Result.success(3), resultOf { 3 })
    }

    @Test
    fun `an exception comes back as failure`() {
        val failure = IllegalStateException("panel not drawn")

        assertEquals(failure, resultOf<Int> { throw failure }.exceptionOrNull())
    }

    @Test
    fun `cancellation is not caught`() {
        assertFailsWith<CancellationException> { resultOf<Int> { throw CancellationException() } }
    }

    @Test
    fun `a timeout is a failure`() = runTest {
        val outcome = resultOf { withTimeout(1) { kotlinx.coroutines.delay(10) } }

        assertTrue(outcome.isFailure)
    }
}
