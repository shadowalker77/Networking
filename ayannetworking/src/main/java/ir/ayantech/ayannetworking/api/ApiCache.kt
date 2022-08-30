package ir.ayantech.ayannetworking.api

import com.google.gson.reflect.TypeToken
import ir.ayantech.ayannetworking.helper.getTypeOf

typealias OutputCallBack<Output> = (Output) -> Unit

class ApiCache<T>(
    var ayanApi: AyanApi,
    val endPoint: String,
    val typeToken: TypeToken<T>
) {

    companion object {
        inline fun <reified P> create(ayanApi: AyanApi, endPoint: String): ApiCache<P> =
            ApiCache(ayanApi, endPoint, getTypeOf())
    }

    var input: Any? = null

    var calledOnce = false
    var output: WrappedPackage<*, T>? = null
    var callbacks: ArrayList<OutputCallBack<*>>? = null

    fun getApiResult(
        ayanApi: AyanApi = this.ayanApi,
        callback: OutputCallBack<T>
    ) {
        if (output?.response?.Parameters != null) {
            callback(output?.response?.Parameters!! as T)
            return
        }
        (callbacks
            ?: arrayListOf<OutputCallBack<*>>().also { callbacks = it }).add(callback)
        if (!calledOnce) {
            calledOnce = true
            output = ayanApi.callSite(
                typeToken,
                AyanCallStatus {
                    success {
                        it.response?.Parameters?.let { resp ->
                            callbacks?.forEach {
                                try {
                                    (it as OutputCallBack<T>).invoke(resp)
                                } catch (e: Exception) {
                                }
                            }
                            callbacks?.clear()
                        }
                    }
                    changeStatus {
                        if (it == CallingState.LOADING) calledOnce = true
                    }
                    failure {
                        ayanApi.commonCallStatus?.dispatchFail(it)
                        calledOnce = false
                    }
                },
                input = input,
                endPoint = endPoint
            )
        }
    }

    fun clear() {
        callbacks = null
        calledOnce = false
        output = null
    }
}