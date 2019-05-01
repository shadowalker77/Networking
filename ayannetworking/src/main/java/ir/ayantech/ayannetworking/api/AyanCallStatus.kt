package ir.ayantech.ayannetworking.api

import ir.ayantech.ayannetworking.ayanModel.Failure

typealias OnLoading = () -> Unit
typealias OnSuccess<T> = (wrappedPackage: WrappedPackage<*, T>) -> Unit
typealias OnFailure = (failure: Failure) -> Unit
typealias OnChangeStatus = (callingState: CallingState) -> Unit

@Suppress("FunctionName")
fun <T> AyanCallStatus(block: AyanCallStatus<T>.() -> Unit) = AyanCallStatus.newInstance<T>().apply(block)

@Suppress("FunctionName")
fun <T> AyanCallStatus(ayanCommonCallStatus: AyanCommonCallStatus, block: AyanCallStatus<T>.() -> Unit) =
    AyanCallStatus.newInstance<T>().also { it.ayanCommonCallingStatus = ayanCommonCallStatus }.apply(block)

@Suppress("FunctionName")
fun AyanCommonCallStatus(block: AyanCommonCallStatus.() -> Unit) = AyanCommonCallStatus.newInstance().apply(block)

class AyanCallStatus<T> private constructor() {

    companion object {
        fun <T> newInstance(): AyanCallStatus<T> = AyanCallStatus()
    }

    var ayanCommonCallingStatus: AyanCommonCallStatus? = null

    private var onSuccess: OnSuccess<T>? = null

    var callingState = CallingState.NOT_USED

    fun success(block: OnSuccess<T>) {
        onSuccess = block
    }

    fun dispatchSuccess(wrappedPackage: WrappedPackage<*, T>) {
        onSuccess?.invoke(wrappedPackage)
        dispatchOnChangeStatus(CallingState.SUCCESSFUL)
    }

    fun dispatchLoad() {
        ayanCommonCallingStatus?.dispatchLoad()
        dispatchOnChangeStatus(CallingState.LOADING)
    }

    fun dispatchFail(failure: Failure) {
        ayanCommonCallingStatus?.dispatchFail(failure)
        dispatchOnChangeStatus(CallingState.FAILED)
    }

    fun dispatchOnChangeStatus(callingState: CallingState) {
        this.callingState = callingState
        ayanCommonCallingStatus?.dispatchChangeStatus(callingState)
    }
}

enum class CallingState {
    NOT_USED, LOADING, FAILED, SUCCESSFUL
}

class AyanCommonCallStatus private constructor() {

    companion object {
        fun newInstance(): AyanCommonCallStatus = AyanCommonCallStatus()
    }

    private var onLoading: OnLoading? = null
    private var onFailure: OnFailure? = null
    private var onChangeStatus: OnChangeStatus? = null

    fun loading(block: OnLoading) {
        onLoading = block
    }

    fun failure(block: OnFailure) {
        onFailure = block
    }

    fun changeStatus(block: OnChangeStatus) {
        onChangeStatus = block
    }

    fun dispatchLoad() {
        onLoading?.invoke()
    }

    fun dispatchFail(failure: Failure) {
        onFailure?.invoke(failure)
    }

    fun dispatchChangeStatus(callingState: CallingState) {
        onChangeStatus?.invoke(callingState)
    }
}