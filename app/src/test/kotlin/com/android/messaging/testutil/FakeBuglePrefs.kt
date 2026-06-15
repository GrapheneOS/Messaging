package com.android.messaging.testutil

import com.android.messaging.util.BuglePrefs

internal class FakeBuglePrefs(
    private val sharedPreferencesName: String = SHARED_PREFERENCES_NAME,
) : BuglePrefs() {

    private val values = mutableMapOf<String, Any?>()

    override fun getSharedPreferencesName(): String {
        return sharedPreferencesName
    }

    override fun onUpgrade(oldVersion: Int, newVersion: Int) {
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return values[key] as? Int ?: defaultValue
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return values[key] as? Long ?: defaultValue
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defaultValue
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return values[key] as? String ?: defaultValue
    }

    override fun getBytes(key: String): ByteArray? {
        return values[key] as? ByteArray
    }

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun putString(key: String, value: String?) {
        values[key] = value
    }

    override fun putBytes(key: String, value: ByteArray?) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }
}
