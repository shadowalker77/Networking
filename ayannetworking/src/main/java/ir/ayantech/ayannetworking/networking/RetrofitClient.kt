package ir.ayantech.ayannetworking.networking

import android.content.Context
import android.os.Build
import ir.ayantech.ayannetworking.helper.AppSignatureHelper
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

        fun getInstance(context: Context?, defaultBaseUrl: String, timeout: Long = 20): Retrofit =
            retrofit
                ?: Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(defaultBaseUrl)
                    .client(getOkHttpInstance(context, timeout))
                    .build()
                    .also { retrofit = it }

        private fun getOkHttpInstance(context: Context?, timeout: Long): OkHttpClient {
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
                    .header("User-Agent", getFormattedDeviceInfo(context))
                    .build()
                it.proceed(userAgentRequest)
            }
            return okHttpClient ?: okHttpClientBuilder.build().also { okHttpClient = it }
        }

        fun cancelCalls() {
            okHttpClient?.dispatcher()?.cancelAll()
        }

        private fun getFormattedDeviceInfo(context: Context?): String {
            val sign = try {
                if (context != null)
                    AppSignatureHelper(context).appSignatures.first()
                else
                    ""
            } catch (e: Exception) {
                ""
            }
            val information = listOf(
                "BuildVersion:(${Build.VERSION.RELEASE})",
                "Brand:(${Build.BRAND})",
                "Model:(${Build.MODEL})",
                "Device:(${Build.DEVICE})",
                "AppVersion:(${context?.packageManager?.getPackageInfo(context.packageName, 0)?.versionName})",
                "Sign:(${sign})"
            )
            return information.joinToString(separator = " ")
        }
    }
}
