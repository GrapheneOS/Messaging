package com.android.messaging.testutil

import org.junit.Assert.assertEquals

@JvmInline
value class TypedAssert<T>(
    private val actual: T,
) {
    fun isEqualTo(expected: T) {
        assertEquals(expected, actual)
    }
}

fun <T> assertThat(actual: T): TypedAssert<T> {
    return TypedAssert(actual)
}
