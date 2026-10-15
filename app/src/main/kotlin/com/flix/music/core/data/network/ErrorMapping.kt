package com.flix.music.core.data.network

import com.flix.music.core.data.youtube.YouTubeMusicException
import com.flix.music.core.model.FlixError
import com.flix.music.core.model.FlixException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** What a network or YouTube failure means for the user (design "Errores"). */
fun Throwable.toFlixException(): FlixException = FlixException(
    error = when (this) {
        is UnknownHostException, is NoRouteToHostException, is ConnectException -> FlixError.NoConnection
        is SocketTimeoutException -> FlixError.Timeout
        is YouTubeMusicException, is ExtractionException -> FlixError.Upstream
        else -> FlixError.Unknown
    },
    cause = this,
)

/** Runs a blocking catalog call on the IO dispatcher and maps its failures to [FlixException]. */
suspend fun <Result> catalogCall(block: () -> Result): Result = withContext(Dispatchers.IO) {
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (exception: Exception) {
        throw exception.toFlixException()
    }
}
