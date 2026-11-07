package ir.ayantech.ayannetworking.v2.api

import com.sun.net.httpserver.HttpServer
import ir.ayantech.networking.ayanModel.FailureType
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.ayanModel.LogLevel
import ir.ayantech.networking.v2.api.APICall
import ir.ayantech.networking.v2.api.AyanApiRequirements
import ir.ayantech.networking.v2.api.safeApiCall
import ir.ayantech.networking.v2.helpers.Failure
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class SafeApiCallStatusTest {
    private lateinit var server: HttpServer
    private lateinit var apiCall: APICall
    private var responseBody = ""
    private var responseStatus = 200

    @Before
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0)
        server.createContext("/") { exchange ->
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(responseStatus, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        apiCall = APICall(
            AyanApiRequirements(
                userAgent = "networking-test",
                baseUrl = "http://127.0.0.1:${server.address.port}/",
                getUserToken = { "test-token" },
                pathUrl = null,
                timeout = 5.seconds,
                headers = hashMapOf(),
                stringParameters = false,
                setNoProxy = true,
                logLevel = LogLevel.DO_NOT_LOG,
                language = Language.PERSIAN,
            )
        )
    }

    @After
    fun tearDown() {
        apiCall.httpClient.close()
        server.stop(0)
    }

    @Test
    fun rejectsNullParametersUsingServerStatusAndDescription() = runBlocking {
        // Arrange
        val description = "رمز یکبار مصرف معتبر نیست."
        responseBody =
            """{"Parameters":null,"Status":{"Code":"GR0053","Description":"$description","IsFromCache":false,"Retryable":false,"Type":"Validation"}}"""

        // Act
        val result = call()

        // Assert
        val failure = result.exceptionOrNull() as Failure
        assertEquals("GR0053", failure.failureCode)
        assertEquals(description, failure.failureMessage)
        assertEquals(description, failure.failureStatus?.description)
        assertEquals("Validation", failure.failureStatus?.type)
    }

    @Test
    fun rejectsFailureBeforeDecodingItsUnrelatedParameters() = runBlocking {
        // Arrange
        responseBody =
            """{"Parameters":{"Unrelated":"value"},"Status":{"Code":"G00002","Description":"Sign in again"}}"""

        // Act
        val result = call()

        // Assert
        val failure = result.exceptionOrNull() as Failure
        assertEquals(FailureType.LOGIN_REQUIRED, failure.failureType)
        assertEquals("Sign in again", failure.failureMessage)
    }

    @Test
    fun decodesSuccessfulParameters() = runBlocking {
        // Arrange
        responseBody =
            """{"Parameters":{"Value":"ready"},"Status":{"Code":"G00000"}}"""

        // Act
        val result = call()

        // Assert
        assertEquals("ready", result.getOrThrow().value)
    }

    @Test
    fun retainsServerDescriptionOnHttpError() = runBlocking {
        // Arrange
        responseStatus = 400
        responseBody =
            """{"Parameters":null,"Status":{"Code":"GR0053","Description":"Invalid one-time code"}}"""

        // Act
        val result = call()

        // Assert
        val failure = result.exceptionOrNull() as Failure
        assertEquals(FailureType.NOT_200, failure.failureType)
        assertEquals("GR0053", failure.failureCode)
        assertEquals("Invalid one-time code", failure.failureMessage)
    }

    @Test
    fun usesTransportFallbackWhenHttpErrorHasNoServiceEnvelope() = runBlocking {
        // Arrange
        responseStatus = 500
        responseBody = "Server unavailable"

        // Act
        val result = call()

        // Assert
        val failure = result.exceptionOrNull() as Failure
        assertEquals(FailureType.NOT_200, failure.failureType)
        assertEquals("500", failure.failureCode)
        assertEquals(null, failure.failureStatus)
    }

    @Test
    fun propagatesCancellation() = runBlocking {
        val expected = CancellationException("request canceled")
        try {
            safeApiCall<Payload> { throw expected }
            fail("Expected cancellation to propagate")
        } catch (actual: CancellationException) {
            assertSame(expected, actual)
        }
    }

    private suspend fun call(): Result<Payload> =
        apiCall.post(body = Payload("request"), apiPath = "")

    @Serializable
    private data class Payload(@SerialName("Value") val value: String)
}
