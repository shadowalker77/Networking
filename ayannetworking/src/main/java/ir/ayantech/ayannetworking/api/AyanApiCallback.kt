package ir.ayantech.ayannetworking.api

import ir.ayantech.ayannetworking.ayanModel.Failure

typealias SimpleCallback = () -> Unit
typealias SuccessCallback<T> = (response: T?) -> Unit
typealias FailureCallback = (failure: Failure) -> Unit
typealias ChangeStatusCallback = (callingState: CallingState) -> Unit

class AyanApiCallback<T> {
    var useCommonSuccessCallback: Boolean = false
    var useCommonFailureCallback: Boolean = true
    var useCommonChangeStatusCallback: Boolean = true

    var successCallback: SuccessCallback<T> = {}
    var failureCallback: FailureCallback = {}
    var changeStatusCallback: ChangeStatusCallback = {}

    fun success(successCallback: SuccessCallback<T>) {
        this.successCallback = successCallback
    }

    fun failure(failureCallback: FailureCallback) {
        this.failureCallback = failureCallback
    }

    fun changeStatusCallback(changeStatusCallback: ChangeStatusCallback) {
        this.changeStatusCallback = changeStatusCallback
    }
}