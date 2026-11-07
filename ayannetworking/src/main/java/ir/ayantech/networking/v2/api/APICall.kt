package ir.ayantech.networking.v2.api

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import ir.ayantech.networking.v2.model.AyanRequest
import ir.ayantech.networking.v2.model.Identity
import ir.ayantech.networking.v2.network.KtorClient

class APICall(val requirements: AyanApiRequirements) {

    val httpClient: HttpClient by lazy { KtorClient.getClient(requirements) }

    suspend inline fun <reified T, reified R> post(
        body: T,
        apiPath: String,
        baseUrl: String? = null
    ): Result<R> =
        safeApiCall<R> {
            val url = buildString {
                append(baseUrl ?: requirements.baseUrl)
                append(apiPath)
            }
            val identity = Identity(token = requirements.getUserToken?.invoke())
            val ayanRequest = AyanRequest(identity = identity, parameters = body)

            httpClient.post(url) {
                setBody(ayanRequest)
            }
        }
}
