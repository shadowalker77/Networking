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
    UNKNOWN
}

enum class Language {
    PERSIAN, ENGLISH, ARABIC
}

class Failure(
    val failureRepository: FailureRepository,
    val failureType: FailureType,
    val failureCode: String,
    val reCallApi: ReCallApi,
    val language: Language,
    val failureStatus: Status?,
    val failureMessage: String = getErrorMessage(failureType, language)
) {

    companion object {
        const val NO_CODE_SERVER_ERROR_CODE = "NOCODEFROMSERVER"
        const val APP_INTERNAL_ERROR_CODE = "INTERNAL"

        const val NO_INTERNET_CONNECTION_FA =
            "ارتباط با سرور برقرار نشد. لطفا اتصال دستگاه خود به اینترنت را بررسی نمایید."
        const val TIMEOUT_FA = "پاسخی از سرور دریافت نشد. لطفا دوباره تلاش نمایید."
        const val CANCELED_FA = "ارتباط با سرور توسط کاربر لغو شد."
        const val LOGIN_REQUIRED_FA = "خطای احراز هویت، لطفا دوباره وارد شوید."
        const val NOT_200_FA = "خطای داخلی، پاسخ دریافت شده نامعتبر است."
        const val UNKNOWN_FA = "خطای داخلی، لطفا در صورت تکرار با پشتیبانی تماس بگیرید."

        const val NO_INTERNET_CONNECTION_EN =
            "No internet connection. Please check your internet connectivity."
        const val TIMEOUT_EN = "Connection timeout. Please try again."
        const val CANCELED_EN = "Operation canceled by user."
        const val LOGIN_REQUIRED_EN = "Session expired. Please login again."
        const val NOT_200_EN = "Internal error."
        const val UNKNOWN_EN = "Unknown error. Please contact support."

        const val NO_INTERNET_CONNECTION_AR = "لا يوجد اتصال بالإنترنت. يرجى التحقق من اتصالك بالإنترنت."
        const val TIMEOUT_AR = "انتهى وقت محاولة الاتصال. حاول مرة اخرى."
        const val CANCELED_AR = "ألغى المستخدم العملية."
        const val LOGIN_REQUIRED_AR = "انتهت الجلسة. الرجاد الدخول على الحساب من جديد."
        const val NOT_200_AR = "خطأ داخلي."
        const val UNKNOWN_AR = "خطأ غير معروف. يرجى الاتصال بالدعم."

        private fun getErrorMessage(failureType: FailureType, language: Language): String =
            when(language) {
                Language.PERSIAN -> when (failureType) {
                    FailureType.NO_INTERNET_CONNECTION -> NO_INTERNET_CONNECTION_FA
                    FailureType.TIMEOUT -> TIMEOUT_FA
                    FailureType.CANCELED -> CANCELED_FA
                    FailureType.LOGIN_REQUIRED -> LOGIN_REQUIRED_FA
                    FailureType.NOT_200 -> NOT_200_FA
                    FailureType.UNKNOWN -> UNKNOWN_FA
                }
                Language.ENGLISH -> when (failureType) {
                    FailureType.NO_INTERNET_CONNECTION -> NO_INTERNET_CONNECTION_EN
                    FailureType.TIMEOUT -> TIMEOUT_EN
                    FailureType.CANCELED -> CANCELED_EN
                    FailureType.LOGIN_REQUIRED -> LOGIN_REQUIRED_EN
                    FailureType.NOT_200 -> NOT_200_EN
                    FailureType.UNKNOWN -> UNKNOWN_EN
                }
                Language.ARABIC -> when (failureType) {
                    FailureType.NO_INTERNET_CONNECTION -> NO_INTERNET_CONNECTION_AR
                    FailureType.TIMEOUT -> TIMEOUT_AR
                    FailureType.CANCELED -> CANCELED_AR
                    FailureType.LOGIN_REQUIRED -> LOGIN_REQUIRED_AR
                    FailureType.NOT_200 -> NOT_200_AR
                    FailureType.UNKNOWN -> UNKNOWN_AR
                }
                else -> "Error"
            }
    }
}