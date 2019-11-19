package ir.ayantech.ayannetworking.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import ir.ayantech.ayannetworking.ayanModel.*
import ir.ayantech.ayannetworking.helper.toPrettyFormat
import ir.ayantech.ayannetworking.networking.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

typealias ReCallApi = () -> Unit
typealias GetUserToken = () -> String

class AyanApi(
    val getUserToken: GetUserToken? = null,
    val defaultBaseUrl: String = "",
    var commonCallStatus: AyanCommonCallStatus? = null,
    val timeout: Long = 30,
    val logLevel: LogLevel = LogLevel.LOG_ALL
) {

    private var apiInterface: ApiInterface? = null

    fun aaa(defaultBaseUrl: String, timeout: Long) =
        (apiInterface ?: RetrofitClient.getInstance(
            defaultBaseUrl,
            timeout
        ).create(ApiInterface::class.java).also {
            apiInterface = it
        })!!

    val wrappedPackages = ArrayList<WrappedPackage<*, *>>()

    inline fun <reified GenericOutput> ayanCall(
        ayanCallStatus: AyanCallStatus<GenericOutput>,
        endPoint: String,
        input: Any? = null,
        identity: Any? = null,
        hasIdentity: Boolean = true,
        commonCallStatus: AyanCommonCallStatus? = null,
        baseUrl: String = defaultBaseUrl
    ): WrappedPackage<*, GenericOutput> {
        if (commonCallStatus != null)
            ayanCallStatus.ayanCommonCallingStatus = commonCallStatus
        else if (this.commonCallStatus != null)
            ayanCallStatus.ayanCommonCallingStatus = this.commonCallStatus

        var finalIdentity: Any? = null
        if (hasIdentity && getUserToken != null) finalIdentity = Identity(getUserToken.invoke())
        if (identity != null) finalIdentity = identity
        val request = AyanRequest(finalIdentity, input)
        val wrappedPackage = WrappedPackage<Any, GenericOutput>(
            baseUrl + endPoint,
            request
        )

        ayanCallStatus.dispatchLoad()
        try {
            if (logLevel == LogLevel.LOG_ALL) {
                try {
                    Log.d("AyanReq", endPoint + ":\n" + Gson().toJson(request).toPrettyFormat())
                } catch (e: Exception) {
                    Log.d("AyanReq", endPoint + ":\n" + Gson().toJson(request))
                }
            }
        } catch (e: Exception) {
        }
        aaa(defaultBaseUrl, timeout).callApi(baseUrl + endPoint, request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    try {
                        wrappedPackage.reCallApi = {
                            ayanCallStatus.dispatchLoad()
                            aaa(defaultBaseUrl, timeout).callApi(
                                wrappedPackage.url,
                                wrappedPackage.request
                            )
                                .enqueue(this)
                        }
                        if (response.isSuccessful) {
                            if (response.body() == null) {
                                val failure = Failure(
                                    FailureRepository.REMOTE,
                                    FailureType.UNKNOWN,
                                    Failure.NO_CODE_SERVER_ERROR_CODE,
                                    wrappedPackage.reCallApi
                                ).also { wrappedPackage.failure = it }
                                ayanCallStatus.dispatchFail(failure)
                            } else {
                                val rawResponse = response.body()?.string()
                                if (logLevel == LogLevel.LOG_ALL) {
                                    try {
                                        Log.d(
                                            "AyanRawResponse",
                                            endPoint + ":\n" + rawResponse?.toPrettyFormat()
                                        )
                                    } catch (e: Exception) {
                                        Log.d(
                                            "AyanRawResponse",
                                            endPoint + ":\n" + (rawResponse ?: "")
                                        )
                                    }
                                }
                                if (logLevel == LogLevel.LOG_ALL)
                                    Log.d("AyanProtocol", response.raw().protocol().name)
                                val jsonObject = JsonParser().parse(rawResponse).asJsonObject
                                var parameters: GenericOutput? = null
                                try {
                                    parameters = Gson().fromJson<GenericOutput>(
                                        jsonObject.getAsJsonObject("Parameters"),
                                        GenericOutput::class.java
                                    )
                                } catch (e: Exception) {
                                    try {
                                        parameters = Gson().fromJson(
                                            jsonObject.getAsJsonArray("Parameters"),
                                            object : TypeToken<GenericOutput>() {
                                            }.type
                                        )
                                    } catch (e: Exception) {
                                        if (logLevel == LogLevel.LOG_ALL)
                                            Log.d("AyanLog", "Parameters is null.")
                                    }
                                }
                                val status =
                                    Gson().fromJson<Status>(
                                        jsonObject.getAsJsonObject("Status"),
                                        Status::class.java
                                    )
                                wrappedPackage.response = AyanResponse(parameters, status)
                                when (wrappedPackage.response!!.Status.Code) {
                                    "G00000" -> ayanCallStatus.dispatchSuccess(wrappedPackage)
                                    "G00001" -> ayanCallStatus.dispatchFail(
                                        Failure(
                                            FailureRepository.REMOTE,
                                            FailureType.SERVER_INTERNAL_ERROR, "G00001",
                                            wrappedPackage.reCallApi
                                        ).also { wrappedPackage.failure = it }
                                    )
                                    "G00002" -> ayanCallStatus.dispatchFail(
                                        Failure(
                                            FailureRepository.REMOTE,
                                            FailureType.LOGIN_REQUIRED, "G00002",
                                            wrappedPackage.reCallApi
                                        ).also { wrappedPackage.failure = it }
                                    )
                                    else -> ayanCallStatus.dispatchFail(
                                        Failure(
                                            FailureRepository.REMOTE,
                                            FailureType.UNKNOWN,
                                            wrappedPackage.response!!.Status.Code,
                                            wrappedPackage.reCallApi,
                                            wrappedPackage.response!!.Status.Description
                                        ).also { wrappedPackage.failure = it }
                                    )
                                }
                            }
                        } else {
                            val failure = Failure(
                                FailureRepository.REMOTE,
                                FailureType.NOT_200,
                                Failure.APP_INTERNAL_ERROR_CODE,
                                wrappedPackage.reCallApi
                            ).also { wrappedPackage.failure = it }
                            ayanCallStatus.dispatchFail(failure)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    wrappedPackage.reCallApi = {
                        ayanCallStatus.dispatchLoad()
                        aaa(defaultBaseUrl, timeout).callApi(
                            wrappedPackage.url,
                            wrappedPackage.request
                        )
                            .enqueue(this)
                    }
                    val failure = when {
                        t is UnknownHostException -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.NO_INTERNET_CONNECTION,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi
                        ).also { wrappedPackage.failure = it }
                        t is TimeoutException -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.TIMEOUT,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi
                        ).also { wrappedPackage.failure = it }
                        t is SocketTimeoutException -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.TIMEOUT,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi
                        ).also { wrappedPackage.failure = it }
                        (t is InterruptedIOException && t.message == "timeout") -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.TIMEOUT,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi
                        ).also { wrappedPackage.failure = it }
//                    is SocketException -> {
//                        canTry = false
//                        message = ""
//                        callback = false
//                    }
//                    is IOException -> {
//                        canTry = false
//                        message = ""
//                        callback = true
//                    }
                        else -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.NO_INTERNET_CONNECTION,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi
                        ).also { wrappedPackage.failure = it }
                    }
                    ayanCallStatus.dispatchFail(failure)
                }
            })
        return wrappedPackage
    }

    inline fun <reified GenericOutput> simpleCall(
        endPoint: String,
        input: Any? = null,
        crossinline onSuccess: (GenericOutput?) -> Unit
    ) {
        ayanCall<GenericOutput>(
            AyanCallStatus {
                success {
                    onSuccess(it.response?.Parameters)
                }
            },
            endPoint,
            input
        )
    }
}

class WrappedPackage<GenericInput, GenericOutput>
    (
    val url: String,
    val request: AyanRequest<GenericInput>?
) {

    init {
        val packageId = getNewPackageId()
    }

    companion object {

        private var uniqueId = 1
        @Synchronized
        private fun getNewPackageId() = uniqueId.also { uniqueId++ }

    }

    lateinit var reCallApi: ReCallApi
    var response: AyanResponse<GenericOutput>? = null
    var failure: Failure? = null

    fun clone(): WrappedPackage<GenericInput, GenericOutput> = WrappedPackage(url, request)

}