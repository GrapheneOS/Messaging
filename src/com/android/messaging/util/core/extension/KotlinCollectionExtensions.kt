package com.android.messaging.util.core.extension

fun <T> Collection<T>.allOrNull(predicate: (T) -> Boolean): Boolean? {
    return when {
        isEmpty() -> null
        else -> all(predicate)
    }
}
