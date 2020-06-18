package ir.ayantech.ayannetworking.api

import android.content.Context
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import ir.ayantech.ayannetworking.ayanModel.*
import ir.ayantech.ayannetworking.helper.toPrettyFormat
import ir.ayantech.ayannetworking.networking.RetrofitClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

typealias ReCallApi = () -> Unit
typealias GetUserToken = () -> String

class AyanApi(
    val context: Context?,
    val getUserToken: GetUserToken? = null,
    val defaultBaseUrl: String = "",
    var commonCallStatus: AyanCommonCallStatus? = null,
    val timeout: Long = 30,
    val addMethodNameToIdentity: Boolean = false,
    val stringParameters: Boolean = false,
    val forceEndPoint: String? = null,
    val logLevel: LogLevel = LogLevel.LOG_ALL
) {

    @Deprecated("This method has been deprecated. Use the constructor with context passed to it.")
    constructor(
        getUserToken: GetUserToken? = null,
        defaultBaseUrl: String = "",
        commonCallStatus: AyanCommonCallStatus? = null,
        timeout: Long = 30,
        logLevel: LogLevel = LogLevel.LOG_ALL
    ) : this(
        null,
        getUserToken,
        defaultBaseUrl,
        commonCallStatus,
        timeout,
        false,
        false,
        null,
        logLevel
    )

    private var apiInterface: ApiInterface? = null

    fun aaa(defaultBaseUrl: String, timeout: Long) =
        (apiInterface ?: RetrofitClient.getInstance(
            context,
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
        if (addMethodNameToIdentity && finalIdentity is Identity) finalIdentity.MethodName =
            endPoint
        val request =
            AyanRequest(finalIdentity, if (stringParameters) Gson().toJson(input) else input)
        val finalUrl = baseUrl + (forceEndPoint ?: endPoint)
        val wrappedPackage = WrappedPackage<Any, GenericOutput>(
            finalUrl,
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
        aaa(defaultBaseUrl, timeout).callApi(finalUrl, request)
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
                                    parameters = when (jsonObject.get("Parameters")) {
                                        is JsonObject -> {
                                            Gson().fromJson(
                                                jsonObject.getAsJsonObject("Parameters"),
                                                GenericOutput::class.java
                                            )
                                        }
                                        is JsonArray -> {
                                            Gson().fromJson(
                                                jsonObject.getAsJsonArray("Parameters"),
                                                object : TypeToken<GenericOutput>() {
                                                }.type
                                            )
                                        }
                                        is JsonPrimitive -> {
                                            (jsonObject.getAsJsonPrimitive("Parameters").asString)?.let {
                                                if (stringParameters)
                                                    Gson().fromJson(
                                                        it,
                                                        GenericOutput::class.java
                                                    )
                                                else
                                                    it as GenericOutput
                                            }

                                        }
                                        else -> null
                                    }
                                } catch (e: Exception) {
                                    if (logLevel == LogLevel.LOG_ALL && !e.message.isNullOrEmpty())
                                        Log.e("Attention", e.message)
                                }
                                val status =
                                    Gson().fromJson(
                                        jsonObject.getAsJsonObject("Status"),
                                        Status::class.java
                                    )
                                wrappedPackage.response = AyanResponse(parameters, status)
                                when (wrappedPackage.response!!.Status.Code) {
                                    "G00000" -> ayanCallStatus.dispatchSuccess(wrappedPackage)
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
                            if (logLevel == LogLevel.LOG_ALL) {
                                Log.e("AyanRawResponse", response.body()?.string() ?: "")
                            }
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
                        (t is IOException && t.message == "Canceled") -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.CANCELED,
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