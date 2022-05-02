package ir.ayantech.ayannetworking.api

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import ir.ayantech.ayannetworking.ayanModel.*
import ir.ayantech.ayannetworking.helper.AppSignatureHelper
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
    context: Context?,
    var getUserToken: GetUserToken? = null,
    var defaultBaseUrl: String = "",
    var commonCallStatus: AyanCommonCallStatus? = null,
    var timeout: Long = 30,
    var headers: HashMap<String, String> = HashMap(),
    val stringParameters: Boolean = false,
    val forceEndPoint: String? = null,
    val logLevel: LogLevel = LogLevel.LOG_ALL
) {

    var checkTokenValidation: ((String?) -> Boolean) = { true }
    var refreshToken: ((oldToken: String?, newTokenReady: (() -> Unit)) -> Unit)? = null

    private var userAgent = ""

    init {
        userAgent = getFormattedDeviceInfo(context)
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
            "AppVersion:(${
                context?.packageManager?.getPackageInfo(
                    context.packageName,
                    0
                )?.versionName
            })",
            "Sign:(${sign})"
        )
        return information.joinToString(separator = " ")
    }

    private var apiInterface: ApiInterface? = null

    fun aaa(
        defaultBaseUrl: String,
        timeout: Long
    ) =
        (apiInterface ?: RetrofitClient.getInstance(
            userAgent,
            defaultBaseUrl,
            timeout
        ).create(ApiInterface::class.java).also {
            apiInterface = it
        })!!

    inline fun <reified GenericOutput> ayanCall(
        ayanCallStatus: AyanCallStatus<GenericOutput>,
        endPoint: String,
        input: Any? = null,
        identity: Any? = null,
        hasIdentity: Boolean = true,
        commonCallStatus: AyanCommonCallStatus? = null,
        baseUrl: String = defaultBaseUrl,
        checkTokenValidation: Boolean = true
    ): WrappedPackage<*, GenericOutput>? {
        return if (checkTokenValidation(getUserToken?.invoke())
            || !checkTokenValidation
            || refreshToken == null
            || getUserToken?.invoke().isNullOrEmpty()
        ) {
            oldAyanCall(
                ayanCallStatus,
                endPoint,
                input,
                identity,
                hasIdentity,
                commonCallStatus,
                baseUrl
            )
        } else {
            refreshToken?.invoke(getUserToken?.invoke()) {
                oldAyanCall(
                    ayanCallStatus,
                    endPoint,
                    input,
                    identity,
                    hasIdentity,
                    commonCallStatus,
                    baseUrl
                )
            }
            null
        }
    }

    inline fun <reified GenericOutput> oldAyanCall(
        ayanCallStatus: AyanCallStatus<GenericOutput>,
        endPoint: String,
        input: Any? = null,
        identity: Any? = null,
        hasIdentity: Boolean = true,
        commonCallStatus: AyanCommonCallStatus? = null,
        baseUrl: String = defaultBaseUrl
    ): WrappedPackage<*, GenericOutput> {
        var language = Language.PERSIAN

        if (this.headers.containsKey("Accept-Language")) {
            language = when(this.headers["Accept-Language"]) {
                "en" -> Language.ENGLISH
                "ar" -> Language.ARABIC
                else -> Language.PERSIAN
            }
        }

        if (commonCallStatus != null)
            ayanCallStatus.ayanCommonCallingStatus = commonCallStatus
        else if (this.commonCallStatus != null)
            ayanCallStatus.ayanCommonCallingStatus = this.commonCallStatus

        var finalIdentity: Any? = null
        if (hasIdentity && getUserToken != null) finalIdentity = Identity(getUserToken?.invoke())
        if (identity != null) finalIdentity = identity
        val request =
            AyanRequest(
                finalIdentity, if (stringParameters) {
                    EscapedParameters(Gson().toJson(input), endPoint)
                } else {
                    input
                }
            )
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
        aaa(defaultBaseUrl, timeout).callApi(finalUrl, request, headers)
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
                                wrappedPackage.request,
                                headers
                            )
                                .enqueue(this)
                        }
                        if (response.isSuccessful) {
                            if (response.body() == null) {
                                val failure = Failure(
                                    FailureRepository.REMOTE,
                                    FailureType.UNKNOWN,
                                    Failure.NO_CODE_SERVER_ERROR_CODE,
                                    wrappedPackage.reCallApi,
                                    language
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
                                            if (stringParameters)
                                                Gson().fromJson(
                                                    (jsonObject.get("Parameters") as JsonObject).get(
                                                        "Params"
                                                    ).asString,
                                                    GenericOutput::class.java
                                                )
                                            else
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
                                            (jsonObject.getAsJsonPrimitive("Parameters").asString) as GenericOutput
                                        }
                                        else -> null
                                    }
                                } catch (e: Exception) {
                                    if (logLevel == LogLevel.LOG_ALL && !e.message.isNullOrEmpty())
                                        Log.e("Attention", e.message ?: "")
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
                                            wrappedPackage.reCallApi,
                                            language
                                        ).also { wrappedPackage.failure = it }
                                    )
                                    else -> ayanCallStatus.dispatchFail(
                                        Failure(
                                            FailureRepository.REMOTE,
                                            FailureType.UNKNOWN,
                                            wrappedPackage.response!!.Status.Code,
                                            wrappedPackage.reCallApi,
                                            language,
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
                                wrappedPackage.reCallApi,
                                language
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
                            wrappedPackage.request,
                            headers
                        )
                            .enqueue(this)
                    }
                    val failure = when {
                        t is UnknownHostException -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.NO_INTERNET_CONNECTION,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi,
                            language
                        )
                        t is TimeoutException -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.TIMEOUT,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi,
                            language
                        )
                        t is SocketTimeoutException -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.TIMEOUT,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi,
                            language
                        )
                        (t is InterruptedIOException && t.message == "timeout") -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.TIMEOUT,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi,
                            language
                        )
                        (t is IOException && t.message == "Canceled") -> Failure(
                            FailureRepository.LOCAL,
                            FailureType.CANCELED,
                            Failure.APP_INTERNAL_ERROR_CODE,
                            wrappedPackage.reCallApi,
                            language
                        )
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
                            wrappedPackage.reCallApi,
                            language
                        )
                    }.also { wrappedPackage.failure = it }
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

    inline fun <reified GenericOutput> call(
        endPoint: String,
        input: Any? = null,
        crossinline callback: AyanApiCallback<GenericOutput>.() -> Unit
    ) {
        val temp = AyanApiCallback<GenericOutput>().apply(callback)
        ayanCall<GenericOutput>(
            AyanCallStatus {
                success {
                    if (temp.useCommonSuccessCallback)
                        ayanCommonCallingStatus?.dispatchSuccess(it)
                    temp.successCallback.invoke(it.response?.Parameters)
                }
                failure {
                    if (temp.useCommonFailureCallback)
                        ayanCommonCallingStatus?.dispatchFail(it)
                    temp.failureCallback.invoke(it)
                }
                changeStatus {
                    if (temp.useCommonChangeStatusCallback)
                        ayanCommonCallingStatus?.dispatchChangeStatus(it)
                    temp.changeStatusCallback.invoke(it)
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