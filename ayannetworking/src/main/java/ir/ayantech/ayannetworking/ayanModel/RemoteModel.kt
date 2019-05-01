package ir.ayantech.ayannetworking.ayanModel

data class Identity(var Token: String)

data class Status(var Code: String, var Description: String)

data class AyanRequest<T>(var Identity: Any?, var Parameters: T?)

data class AyanResponse<T>(var Parameters: T?, var Status: Status)