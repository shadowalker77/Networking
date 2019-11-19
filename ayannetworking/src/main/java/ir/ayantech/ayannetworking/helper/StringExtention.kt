package ir.ayantech.ayannetworking.helper

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

fun String.toPrettyFormat(): String {
    val parser = JsonParser()
    val json = parser.parse(this).asJsonObject
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(json)
}