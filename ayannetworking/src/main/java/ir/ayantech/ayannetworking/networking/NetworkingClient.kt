package ir.ayantech.ayannetworking.networking

import com.google.gson.Gson
import ir.ayantech.ayannetworking.helper.dePent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.Proxy
import java.util.concurrent.TimeUnit

object NetworkingClient {

    @Synchronized
    fun getInstance(
        okHttpClient: OkHttpClient,
        defaultBaseUrl: String,
        gson: Gson?
    ): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(
                if (gson == null) GsonConverterFactory.create()
                else GsonConverterFactory.create(gson)
            )
            .baseUrl(defaultBaseUrl)
            .client(okHttpClient)
            .build()

    fun getOkHttpInstance(
        userAgent: String,
        timeout: Long,
        setNoProxy: Boolean,
        hostName: String? = null,
        logItems: List<Int>? = null,
        feed: Array<Int>? = null
    ): OkHttpClient {
        val okHttpClientBuilder = OkHttpClient.Builder()
        okHttpClientBuilder.callTimeout(timeout, TimeUnit.SECONDS)
        okHttpClientBuilder.connectTimeout(timeout, TimeUnit.SECONDS)
        okHttpClientBuilder.readTimeout(timeout, TimeUnit.SECONDS)
        okHttpClientBuilder.writeTimeout(timeout, TimeUnit.SECONDS)
        okHttpClientBuilder.addNetworkInterceptor(SHA256FingerprintInterceptor())
        if (setNoProxy)
            okHttpClientBuilder.proxy(Proxy.NO_PROXY)
        okHttpClientBuilder.protocols(arrayListOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        okHttpClientBuilder.addInterceptor {
            val userAgentRequest = it.request()
                .newBuilder()
                .header("User-Agent", userAgent)
                .build()
            it.proceed(userAgentRequest)
        }
//        if (hostName != null && logItems != null && feed != null) {
//            okHttpClientBuilder.certificatePinner(
//                CertificatePinner.Builder().add(
//                    hostName,
//                    logItems.dePent(feed)
//                ).build()
//            )
//        }
        return okHttpClientBuilder.build()
    }
}
