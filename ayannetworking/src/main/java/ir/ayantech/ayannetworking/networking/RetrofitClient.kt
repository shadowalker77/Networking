package ir.ayantech.ayannetworking.networking

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient private constructor() {

    companion object {

        @Volatile
        private var retrofit: Retrofit? = null

        fun getInstance(defaultBaseUrl: String): Retrofit = retrofit
            ?: Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(defaultBaseUrl)
                .build()
                .also { retrofit = it }
    }
}
