package com.android.messaging.util.core.extension

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

inline fun <T> typedFlow(
    crossinline block: suspend FlowCollector<T>.() -> T,
): Flow<T> {
    return flow {
        val value = block()

        emit(value)
    }
}

inline fun unitFlow(
    crossinline block: suspend FlowCollector<Unit>.() -> Unit,
): Flow<Unit> {
    return flow {
        block()
        emit(Unit)
    }
}
