package ir.ayantech.ayannetworking.ayanModel

data class Identity(var Token: String)

data class Status(var Code: String, var Description: String)

data class AyanRequest<T>(var Identity: Identity?, var Parameters: T?)

data class AyanResponse<T>(var Parameters: T?, var Status: Status)

inline fun <reified T> toJson(input: T): String = ""