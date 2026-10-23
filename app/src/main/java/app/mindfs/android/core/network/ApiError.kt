package app.mindfs.android.core.network

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

sealed class ApiError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Timeout(cause: Throwable) : ApiError("Connection timed out", cause)
    class Network(cause: Throwable) :
        ApiError("Network error: ${cause.message ?: cause::class.java.simpleName}", cause)

    class Http(val status: HttpStatusCode) : ApiError("HTTP ${status.value} ${status.description}")
    class Decoding(cause: Throwable) : ApiError("Unexpected response format", cause)

    companion object {
        fun from(cause: Throwable): ApiError = when (cause) {
            is ApiError -> cause
            is SocketTimeoutException -> Timeout(cause)
            is HttpRequestTimeoutException -> Timeout(cause)
            is ResponseException -> Http(cause.response.status)
            is SerializationException -> Decoding(cause)
            is IOException -> Network(cause)
            else -> Network(cause)
        }
    }
}

/** Runs one API call, mapping every failure to an [ApiError] (cancellations pass through). */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failed: Throwable) {
        throw ApiError.from(failed)
    }
