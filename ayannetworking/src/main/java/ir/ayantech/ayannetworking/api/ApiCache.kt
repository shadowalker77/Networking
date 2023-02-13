package ir.ayantech.ayannetworking.api

import com.google.gson.reflect.TypeToken
import ir.ayantech.ayannetworking.helper.getTypeOf

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

    var callBacks: ArrayList<AyanApiCallback<T>>? = null

    fun getApiResult(
        ayanApi: AyanApi = this.ayanApi,
        callback: (T) -> Unit
    ) {
        getFullApiResult(ayanApi) {
            useCommonChangeStatusCallback = false
            success {
                it?.let { it1 -> callback(it1) }
            }
            changeStatusCallback {
                if (it == CallingState.LOADING) calledOnce = true
            }
        }
    }

    fun getFullApiResult(
        ayanApi: AyanApi = this.ayanApi,
        callback: AyanApiCallback<T>.() -> Unit
    ) {
        val temp = AyanApiCallback<T>().apply(callback)
        if (output?.response?.Parameters != null) {
            temp.successCallback(output?.response?.Parameters!! as T)
            return
        }
        (callBacks
            ?: arrayListOf<AyanApiCallback<T>>().also { callBacks = it }).add(temp)
        if (!calledOnce) {
            calledOnce = true
            output = ayanApi.callSite(
                typeToken,
                AyanCallStatus {
                    success {
                        if (temp.useCommonSuccessCallback)
                            ayanCommonCallingStatus?.dispatchSuccess(it)
                        it.response?.Parameters?.let { resp ->
                            callBacks?.forEach {
                                try {
                                    it.successCallback.invoke(resp)
                                } catch (e: Exception) {
                                }
                            }
                            callBacks?.clear()
                        }
                    }
                    failure { failure ->
                        if (temp.useCommonFailureCallback && calledOnce)
                            ayanCommonCallingStatus?.dispatchFail(failure)
                        callBacks?.forEach {
                            try {
                                temp.failureCallback.invoke(failure)
                            } catch (e: Exception) {
                            }
                        }
                        calledOnce = false
                    }
                    changeStatus { cs ->
                        if (temp.useCommonChangeStatusCallback)
                            ayanCommonCallingStatus?.dispatchChangeStatus(cs)
                        if (cs == CallingState.LOADING) calledOnce = true
                        callBacks?.forEach {
                            try {
                                temp.changeStatusCallback.invoke(cs)
                            } catch (e: Exception) {
                            }
                        }
                    }
                },
                input = input,
                endPoint = endPoint
            )
        }
    }

    fun clear() {
        callBacks = null
        calledOnce = false
        output = null
    }
}