package ir.ayantech.networking.v2

import android.content.Context
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.ayanModel.LogLevel
import ir.ayantech.networking.helper.AppSignatureHelper
import ir.ayantech.networking.v2.api.APICall
import ir.ayantech.networking.v2.api.AyanApiRequirements
import ir.ayantech.networking.v2.helpers.generateUserAgent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class AyanApi private constructor(private val requirements: AyanApiRequirements) {

    val apiCall: APICall by lazy {
        APICall(requirements)
    }

    suspend inline fun <reified Body, reified Response> post(
        body: Body,
        endPoint: String,
        baseUrl: String? = null,
    ): Result<Response> {
        return apiCall.post(body = body, apiPath = endPoint, baseUrl = baseUrl)
    }

    class Builder(
        private val context: Context,
        private val baseUrl: String,
    ) {

        private var _getUserToken: (() -> String)? = null
        private var _pathUrl: String = ""
        private var _timeout: Duration = 30.seconds
        private var _headers: HashMap<String, String> = HashMap()
        private var _stringParameters: Boolean = false
        private var _setNoProxy: Boolean = true
        private var _logLevel: LogLevel = LogLevel.LOG_ALL
        private var _acceptLanguage: Language = Language.PERSIAN
        private var _followRedirect: Boolean = true


        /*
         *TODO: It's bad way for read user token,
         *  it's better to use dependency injection or another solution like observer pattern.
         */
        fun setInvokeUserToken(getUserToken: () -> String): Builder {
            this._getUserToken = getUserToken
            return this
        }

        fun setNoProxyMode(noProxy: Boolean): Builder {
            this._setNoProxy = noProxy
            return this
        }

        fun setTimeOutDuration(timeOut: Duration): Builder {
            this._timeout = timeOut
            return this
        }

        fun setLogLevel(level: LogLevel): Builder {
            this._logLevel = level
            return this
        }

        fun setCustomHeaders(headers: HashMap<String, String>): Builder {
            this._headers = headers
            return this
        }

        fun setPathUrl(path: String): Builder {
            this._pathUrl = path
            return this
        }

        fun setAcceptLanguage(language: Language): Builder {
            this._acceptLanguage = language
            return this
        }

        fun setFollowRedirect(redirect: Boolean): Builder {
            this._followRedirect = redirect
            return this
        }

        fun build(): AyanApi {
            val sign = AppSignatureHelper(context).appSignatures.firstOrNull().orEmpty()
            val userAgent = context.generateUserAgent(sign = sign)
            val requirements = AyanApiRequirements(
                userAgent = userAgent,
                baseUrl = baseUrl,
                getUserToken = _getUserToken,
                pathUrl = _pathUrl,
                timeout = _timeout,
                headers = _headers,
                stringParameters = _stringParameters,
                setNoProxy = _setNoProxy,
                logLevel = _logLevel,
                language = _acceptLanguage,
                followRedirects = _followRedirect
            )

            return AyanApi(requirements = requirements)
        }

    }
}

