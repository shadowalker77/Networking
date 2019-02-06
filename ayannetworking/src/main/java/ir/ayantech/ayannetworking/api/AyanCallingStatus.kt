package ir.ayantech.ayannetworking.api

import ir.ayantech.ayannetworking.ayanModel.Failure

typealias OnLoading = () -> Unit
typealias OnSuccess<T> = (wrappedPackage: WrappedPackage<*, T>) -> Unit
typealias OnFailure = (failure: Failure) -> Unit

@Suppress("FunctionName")
fun <T> AyanCallStatus(block: AyanCallingStatus<T>.() -> Unit) = AyanCallingStatus<T>().apply(block)

@Suppress("FunctionName")
fun <T> AyanCallStatus(ayanCommonCallingStatus: AyanCommonCallingStatus, block: AyanCallingStatus<T>.() -> Unit) =
    AyanCallingStatus<T>(ayanCommonCallingStatus).apply(block)

@Suppress("FunctionName")
fun AyanCommonCallStatus(block: AyanCommonCallingStatus.() -> Unit) = AyanCommonCallingStatus().apply(block)

class AyanCallingStatus<T>() {
    private var ayanCommonCallingStatus = AyanCommonCallingStatus()
    private var onSuccess: OnSuccess<T>? = null

    var callingState = CallingState.NOT_USED

    constructor(ayanCommonCallingStatus: AyanCommonCallingStatus) : this() {
        this.ayanCommonCallingStatus = ayanCommonCallingStatus
    }

    fun loading(block: OnLoading) {
        ayanCommonCallingStatus.loading(block)
    }

    fun failure(block: OnFailure) {
        ayanCommonCallingStatus.failure(block)
    }

    fun success(block: OnSuccess<T>) {
        onSuccess = block
    }

    fun dispatchSuccess(wrappedPackage: WrappedPackage<*, T>) {
        onSuccess?.invoke(wrappedPackage)
        callingState = CallingState.SUCCESSFUL
    }

    fun dispatchLoad() {
        ayanCommonCallingStatus.dispatchLoad()
        callingState = CallingState.LOADING
    }

    fun dispatchFail(failure: Failure) {
        ayanCommonCallingStatus.dispatchFail(failure)
        callingState = CallingState.FAILED
    }
}

enum class CallingState {
    NOT_USED, LOADING, FAILED, SUCCESSFUL
}

class AyanCommonCallingStatus {
    private var onLoading: OnLoading? = null
    private var onFailure: OnFailure? = null

    fun loading(block: OnLoading) {
        onLoading = block
    }

    fun failure(block: OnFailure) {
        onFailure = block
    }

    fun dispatchLoad() {
        onLoading?.invoke()
    }

    fun dispatchFail(failure: Failure) {
        onFailure?.invoke(failure)
    }
}