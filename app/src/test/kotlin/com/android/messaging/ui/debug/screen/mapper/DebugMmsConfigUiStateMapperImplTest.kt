package com.android.messaging.ui.debug.screen.mapper

import com.android.messaging.data.debugmmsconfig.model.DebugSim
import com.android.messaging.data.debugmmsconfig.model.MmsConfigEntry
import com.android.messaging.data.debugmmsconfig.model.MmsConfigKeyType
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.ui.debug.screen.model.DebugSimUiState
import com.android.messaging.ui.debug.screen.model.MmsConfigItemUiState
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Test

internal class DebugMmsConfigUiStateMapperImplTest {

    private val mapper = DebugMmsConfigUiStateMapperImpl()

    @Test
    fun toSims_formatsMccMnc() {
        val sims = persistentListOf(
            DebugSim(subId = SubId(1), mcc = 310, mnc = 260),
            DebugSim(subId = SubId(2), mcc = 208, mnc = 1),
        )

        val result = mapper.toSims(sims)
        assertEquals(
            persistentListOf(
                DebugSimUiState(subId = SubId(1), mccMnc = "(310/260)"),
                DebugSimUiState(subId = SubId(2), mccMnc = "(208/1)"),
            ),
            result,
        )
    }

    @Test
    fun toItems_boolTrue_mapsToCheckedToggle() {
        val entries = persistentListOf(
            MmsConfigEntry(key = "enabledMMS", keyType = MmsConfigKeyType.BOOL, value = "true"),
        )

        val result = mapper.toItems(entries)
        assertEquals(
            persistentListOf(
                MmsConfigItemUiState.Toggle(key = "enabledMMS", checked = true),
            ),
            result,
        )
    }

    @Test
    fun toItems_boolFalse_mapsToUncheckedToggle() {
        val entries = persistentListOf(
            MmsConfigEntry(key = "enabledMMS", keyType = MmsConfigKeyType.BOOL, value = "false"),
        )

        val result = mapper.toItems(entries)
        assertEquals(
            persistentListOf(
                MmsConfigItemUiState.Toggle(key = "enabledMMS", checked = false),
            ),
            result,
        )
    }

    @Test
    fun toItems_intEntry_mapsToNumericEditable() {
        val entries = persistentListOf(
            MmsConfigEntry(key = "maxMessageSize", keyType = MmsConfigKeyType.INT, value = "1024"),
        )

        val result = mapper.toItems(entries)
        assertEquals(
            persistentListOf(
                MmsConfigItemUiState.Editable(
                    key = "maxMessageSize",
                    value = "1024",
                    isNumeric = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun toItems_stringEntry_mapsToNonNumericEditable() {
        val entries = persistentListOf(
            MmsConfigEntry(key = "userAgent", keyType = MmsConfigKeyType.STRING, value = "Bugle"),
        )

        val result = mapper.toItems(entries)
        assertEquals(
            persistentListOf(
                MmsConfigItemUiState.Editable(
                    key = "userAgent",
                    value = "Bugle",
                    isNumeric = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun toSims_empty_returnsEmpty() {
        assertEquals(persistentListOf<DebugSimUiState>(), mapper.toSims(persistentListOf()))
    }

    @Test
    fun toItems_empty_returnsEmpty() {
        assertEquals(persistentListOf<MmsConfigItemUiState>(), mapper.toItems(persistentListOf()))
    }
}
