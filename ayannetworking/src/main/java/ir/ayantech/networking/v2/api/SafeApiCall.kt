package ir.ayantech.networking.v2.api

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import ir.ayantech.networking.ayanModel.FailureRepository
import ir.ayantech.networking.ayanModel.FailureType
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.v2.helpers.Failure
import ir.ayantech.networking.v2.model.AyanResponse
import ir.ayantech.networking.v2.model.Status
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

@PublishedApi
internal val responseJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

@PublishedApi
internal fun remoteFailure(
    type: FailureType,
    code: String,
    language: Language,
    status: Status?,
): Failure {
    val fallback = Failure(FailureRepository.REMOTE, type, code, language, status)
    val description = status?.description?.takeIf(String::isNotBlank) ?: return fallback
    return Failure(FailureRepository.REMOTE, type, code, language, status, description)
}

suspend inline fun <reified T> safeApiCall(
    language: Language = Language.PERSIAN,
    request: suspend () -> HttpResponse
): Result<T> {
    return try {
        val response = request.invoke()
        currentCoroutineContext().ensureActive()
        if (response.status != HttpStatusCode.OK) {
            val status = runCatching {
                response.body<AyanResponse<JsonElement>>().status
            }.getOrNull()
            currentCoroutineContext().ensureActive()
            return Result.failure(
                remoteFailure(
                    FailureType.NOT_200,
                    status?.code ?: response.status.value.toString(),
                    language,
                    status,
                )
            )
        }

        val data = response.body<AyanResponse<JsonElement>>()
        val status = data.status
        val failureCode = status.code ?: response.status.value.toString()

        if (status.code != "G00000") {
            val failureType =
                if (status.code == "G00002") FailureType.LOGIN_REQUIRED else FailureType.UNKNOWN
            return Result.failure(remoteFailure(failureType, failureCode, language, status))
        }

        val parameters = data.parameters
        if (parameters == null || parameters == JsonNull) {
            return Result.failure(
                remoteFailure(FailureType.UNKNOWN, failureCode, language, status)
            )
        }

        val value = responseJson.decodeFromJsonElement<T>(parameters)
        currentCoroutineContext().ensureActive()
        Result.success(value)

    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        currentCoroutineContext().ensureActive()
        Result.failure(throwable.toFailure(language = language))
    }
}

fun Throwable.toFailure(language: Language): Failure {
    return when {
        this is UnknownHostException -> Failure(
            failureRepository = FailureRepository.LOCAL,
            failureType = FailureType.NO_INTERNET_CONNECTION,
            failureCode = Failure.APP_INTERNAL_ERROR_CODE,
            language = language,
            failureStatus = null
        )

        this is TimeoutException -> Failure(
            failureRepository = FailureRepository.LOCAL,
            failureType = FailureType.TIMEOUT,
            failureCode = Failure.APP_INTERNAL_ERROR_CODE,
            language = language,
            failureStatus = null
        )

        this is SocketTimeoutException -> Failure(
            failureRepository = FailureRepository.LOCAL,
            failureType = FailureType.TIMEOUT,
            failureCode = Failure.APP_INTERNAL_ERROR_CODE,
            language = language,
            failureStatus = null
        )

        this is InterruptedIOException && this.message == "timeout" -> Failure(
            failureRepository = FailureRepository.LOCAL,
            failureType = FailureType.TIMEOUT,
            failureCode = Failure.APP_INTERNAL_ERROR_CODE,
            language = language,
            failureStatus = null
        )

        (this is IOException && this.message == "canceled") -> Failure(
            failureRepository = FailureRepository.LOCAL,
            failureType = FailureType.TIMEOUT,
            failureCode = Failure.APP_INTERNAL_ERROR_CODE,
            language = language,
            failureStatus = null
        )

        else -> Failure(
            failureRepository = FailureRepository.LOCAL,
            failureType = FailureType.UNKNOWN,
            failureCode = Failure.NO_CODE_SERVER_ERROR_CODE,
            language = language,
            failureStatus = null
        )
    }

}
