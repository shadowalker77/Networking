package ir.ayantech.ayannetworking.networking

import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Proxy
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var okHttpClient: OkHttpClient? = null

    fun getInstance(
        userAgent: String, defaultBaseUrl: String, timeout: Long = 20
    ): Retrofit =
        retrofit
            ?: Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(defaultBaseUrl)
                .client(getOkHttpInstance(userAgent, timeout))
                .build()
                .also { retrofit = it }

    private fun getOkHttpInstance(
        userAgent: String,
        timeout: Long
    ): OkHttpClient {
        val okHttpClientBuilder = OkHttpClient.Builder()
        okHttpClientBuilder.callTimeout(timeout, TimeUnit.SECONDS)
        okHttpClientBuilder.connectTimeout(timeout, TimeUnit.SECONDS)
        okHttpClientBuilder.readTimeout(timeout, TimeUnit.SECONDS)
        okHttpClientBuilder.writeTimeout(timeout, TimeUnit.SECONDS)
        okHttpClientBuilder.proxy(Proxy.NO_PROXY)
        okHttpClientBuilder.protocols(arrayListOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        okHttpClientBuilder.addInterceptor {
            val userAgentRequest = it.request()
                .newBuilder()
                .header("User-Agent", userAgent)
                .build()
            it.proceed(userAgentRequest)
        }
        return okHttpClient ?: okHttpClientBuilder.build().also { okHttpClient = it }
    }

    fun cancelCalls() {
        okHttpClient?.dispatcher()?.cancelAll()
    }

    fun clear() {
        retrofit = null
        okHttpClient = null
    }
}
