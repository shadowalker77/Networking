package ir.ayantech.ayannetworking.networking

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

        fun getOkHttpInstance(timeout: Long): OkHttpClient {
            val okHttpClientBuilder = OkHttpClient.Builder()
            okHttpClientBuilder.callTimeout(timeout, TimeUnit.SECONDS)
            okHttpClientBuilder.connectTimeout(timeout, TimeUnit.SECONDS)
            okHttpClientBuilder.readTimeout(timeout, TimeUnit.SECONDS)
            okHttpClientBuilder.writeTimeout(timeout, TimeUnit.SECONDS)
            return okHttpClient ?: okHttpClientBuilder.build().also { okHttpClient = it }
        }
    }
}
