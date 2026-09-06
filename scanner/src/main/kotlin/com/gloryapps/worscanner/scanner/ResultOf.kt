package com.gloryapps.worscanner.scanner

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

/** `runCatching` that lets cancellation through; a timeout is the work failing, not the caller leaving. */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (timeout: TimeoutCancellationException) {
    Result.failure(timeout)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    Result.failure(failure)
}
