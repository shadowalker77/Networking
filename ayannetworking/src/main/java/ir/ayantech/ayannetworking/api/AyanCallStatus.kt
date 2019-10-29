package ir.ayantech.ayannetworking.api

import ir.ayantech.ayannetworking.ayanModel.Failure

typealias OnLoading = () -> Unit
typealias OnSuccess<T> = (wrappedPackage: WrappedPackage<*, T>) -> Unit
typealias OnFailure = (failure: Failure) -> Unit
typealias OnChangeStatus = (callingState: CallingState) -> Unit

@Suppress("FunctionName")
fun <T> AyanCallStatus(block: AyanCallStatus<T>.() -> Unit) =
    AyanCallStatus.newInstance<T>().apply(block)

@Suppress("FunctionName")
fun <T> AyanCallStatus(
    ayanCommonCallStatus: AyanCommonCallStatus,
    block: AyanCallStatus<T>.() -> Unit
) =
    AyanCallStatus.newInstance<T>().also {
        it.ayanCommonCallingStatus = ayanCommonCallStatus
    }.apply(block)

@Suppress("FunctionName")
fun AyanCommonCallStatus(block: AyanCommonCallStatus.() -> Unit) =
    AyanCommonCallStatus.newInstance().apply(block)

class AyanCallStatus<T> private constructor() {

    companion object {
        fun <T> newInstance(): AyanCallStatus<T> = AyanCallStatus()
    }

    var ayanCommonCallingStatus: AyanCommonCallStatus? = null

    private var onSuccess: OnSuccess<T>? = null
    private var onLoading: OnLoading? = null
    private var onFailure: OnFailure? = null
    private var onChangeStatus: OnChangeStatus? = null

    var callingState = CallingState.NOT_USED

    fun success(block: OnSuccess<T>) {
        onSuccess = block
    }

    fun loading(block: OnLoading) {
        onLoading = block
    }

    fun failure(block: OnFailure) {
        onFailure = block
    }

    fun changeStatus(block: OnChangeStatus) {
        onChangeStatus = block
    }

    fun dispatchSuccess(wrappedPackage: WrappedPackage<*, T>) {
        dispatchOnChangeStatus(CallingState.SUCCESSFUL)
        if (onSuccess == null)
            ayanCommonCallingStatus?.dispatchSuccess(wrappedPackage)
        else
            onSuccess?.invoke(wrappedPackage)
    }

    fun dispatchLoad() {
        dispatchOnChangeStatus(CallingState.LOADING)
        if (onLoading == null)
            ayanCommonCallingStatus?.dispatchLoad()
        else
            onLoading?.invoke()
    }

    fun dispatchFail(failure: Failure) {
        dispatchOnChangeStatus(CallingState.FAILED)
        if (onFailure == null)
            ayanCommonCallingStatus?.dispatchFail(failure)
        else
            onFailure?.invoke(failure)
    }

    fun dispatchOnChangeStatus(callingState: CallingState) {
        if (onChangeStatus == null)
            ayanCommonCallingStatus?.dispatchChangeStatus(callingState)
        else
            onChangeStatus?.invoke(callingState)
    }
}

enum class CallingState {
    NOT_USED, LOADING, FAILED, SUCCESSFUL
}

class AyanCommonCallStatus private constructor() {

    companion object {
        fun newInstance(): AyanCommonCallStatus = AyanCommonCallStatus()
    }

    private var onSuccess: OnSuccess<*>? = null
    private var onLoading: OnLoading? = null
    private var onFailure: OnFailure? = null
    private var onChangeStatus: OnChangeStatus? = null

    fun success(block: OnSuccess<*>) {
        onSuccess = block
    }

    fun loading(block: OnLoading) {
        onLoading = block
    }

    fun failure(block: OnFailure) {
        onFailure = block
    }

    fun changeStatus(block: OnChangeStatus) {
        onChangeStatus = block
    }

    fun dispatchSuccess(wrappedPackage: WrappedPackage<*, *>) {
        onSuccess?.invoke(wrappedPackage)
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