package ir.ayantech.ayannetworking.helper

import com.google.gson.reflect.TypeToken

inline fun <reified T> getTypeOf() = object : TypeToken<T>() {
}