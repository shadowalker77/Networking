package ir.ayantech.ayannetworking.ayanModel

import ir.ayantech.ayannetworking.api.ReCallApi

enum class FailureRepository {
    LOCAL,
    REMOTE
}

enum class FailureType {
    NO_INTERNET_CONNECTION,
    TIMEOUT,
    CANCELED,
    LOGIN_REQUIRED,
    NOT_200,
    SERVER_INTERNAL_ERROR,
    UNKNOWN
}

class Failure(
    val failureRepository: FailureRepository,
    val failureType: FailureType,
    val failureCode: String,
    val reCallApi: ReCallApi,
    val failureMessage: String = getErrorMessage(failureType)
) {

    companion object {
        const val NO_CODE_SERVER_ERROR_CODE = "NOCODEFROMSERVER"
        const val APP_INTERNAL_ERROR_CODE = "INTERNAL"

        private fun getErrorMessage(failureType: FailureType): String = when (failureType) {
            FailureType.NO_INTERNET_CONNECTION -> "ارتباط با سرور برقرار نشد. لطفا اتصال دستگاه خود به اینترنت را بررسی نمایید."
            FailureType.TIMEOUT -> "پاسخی از سرور دریافت نشد. لطفا دوباره تلاش نمایید."
            FailureType.CANCELED -> "ارتباط با سرور توسط کاربر لغو شد."
            FailureType.LOGIN_REQUIRED -> "خطای احراز هویت، لطفا دوباره وارد شوید."
            FailureType.NOT_200 -> "خطای داخلی، پاسخ دریافت شده نامعتبر است."
            FailureType.SERVER_INTERNAL_ERROR -> "خطای سرور، لطفا در صورت تکرار با پشتیبانی تماس بگیرید."
            FailureType.UNKNOWN -> "خطای داخلی، لطفا در صورت تکرار با پشتیبانی تماس بگیرید."
        }
    }
}