package ir.ayantech.ayannetworking.ayanModel

data class Identity(var Token: String?)

data class Status(
    val Code: String?,
    val Description: String?,
    val Hint: String?,
    val IsFromCache: Boolean?,
    val Retryable: Boolean?,
    val Type: String?
)

data class AyanRequest<T>(var Identity: Any?, var Parameters: T?)

data class AyanResponse<T>(var Parameters: T?, var Status: Status)

data class EscapedParameters(val Params: String, var MethodName: String? = null)