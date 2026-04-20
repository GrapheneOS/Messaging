package com.android.messaging.debug

import com.android.messaging.util.BuglePrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DebugSimEmulationMode {
    DEFAULT,
    SINGLE,
    DUAL,
}

internal interface DebugSimEmulationSource {
    val mode: StateFlow<DebugSimEmulationMode>
}

object DebugSimEmulationStore : DebugSimEmulationSource {

    private const val PREF_KEY = "debug_sim_emulation_mode"

    private val _mode: MutableStateFlow<DebugSimEmulationMode> by lazy {
        MutableStateFlow(value = loadPersistedMode())
    }

    override val mode = _mode.asStateFlow()

    @JvmStatic
    fun getCurrentMode(): DebugSimEmulationMode {
        return _mode.value
    }

    @JvmStatic
    fun setMode(mode: DebugSimEmulationMode) {
        if (_mode.value == mode) {
            return
        }

        _mode.value = mode
        BuglePrefs.getApplicationPrefs().putString(PREF_KEY, mode.name)
    }

    private fun loadPersistedMode(): DebugSimEmulationMode {
        val stored = BuglePrefs
            .getApplicationPrefs()
            .getString(PREF_KEY, DebugSimEmulationMode.DEFAULT.name)

        return runCatching { DebugSimEmulationMode.valueOf(value = stored) }
            .getOrDefault(defaultValue = DebugSimEmulationMode.DEFAULT)
    }
}
