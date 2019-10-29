package ir.ayantech.ayannetworking.networking

import android.os.Build
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

class RetrofitClient private constructor() {

    companion object {

        @Volatile
        private var retrofit: Retrofit? = null

        @Volatile
        private var okHttpClient: OkHttpClient? = null

        fun getInstance(defaultBaseUrl: String, timeout: Long = 20): Retrofit = retrofit
            ?: Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(defaultBaseUrl)
                .client(getOkHttpInstance(timeout))
                .build()
                .also { retrofit = it }

        private fun getOkHttpInstance(timeout: Long): OkHttpClient {
            val okHttpClientBuilder = OkHttpClient.Builder()
            okHttpClientBuilder.callTimeout(timeout, TimeUnit.SECONDS)
            okHttpClientBuilder.connectTimeout(timeout, TimeUnit.SECONDS)
            okHttpClientBuilder.readTimeout(timeout, TimeUnit.SECONDS)
            okHttpClientBuilder.writeTimeout(timeout, TimeUnit.SECONDS)
            okHttpClientBuilder.proxy(Proxy.NO_PROXY)
//            okHttpClientBuilder.proxy(
//                Proxy(
//                    Proxy.Type.HTTP,
//                    InetSocketAddress("192.168.1.10", 8888)
//                )
//            )
            okHttpClientBuilder.protocols(arrayListOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            okHttpClientBuilder.addInterceptor {
                val userAgentRequest = it.request()
                    .newBuilder()
                    .header("User-Agent", getFormattedDeviceInfo())
                    .build()
                it.proceed(userAgentRequest)
            }
            return okHttpClient ?: okHttpClientBuilder.build().also { okHttpClient = it }
        }

        fun cancelCalls() {
            okHttpClient?.dispatcher()?.cancelAll()
        }

        private fun getFormattedDeviceInfo(): String {
            val information = listOf(
                "BuildVersion:(${Build.VERSION.RELEASE})",
                "Brand:(${Build.BRAND})",
                "Model:(${Build.MODEL})",
                "Device:(${Build.DEVICE})"
            )
            return information.joinToString(separator = " ")
        }
    }
}
