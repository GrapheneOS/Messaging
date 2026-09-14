package com.android.messaging.ui.debug.screen.mapper

import com.android.messaging.data.debugmmsconfig.model.DebugSim
import com.android.messaging.data.debugmmsconfig.model.MmsConfigEntry
import com.android.messaging.data.debugmmsconfig.model.MmsConfigKeyType
import com.android.messaging.ui.debug.screen.model.DebugSimUiState
import com.android.messaging.ui.debug.screen.model.MmsConfigItemUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface DebugMmsConfigUiStateMapper {
    fun toSims(sims: ImmutableList<DebugSim>): ImmutableList<DebugSimUiState>
    fun toItems(entries: ImmutableList<MmsConfigEntry>): ImmutableList<MmsConfigItemUiState>
}

internal class DebugMmsConfigUiStateMapperImpl @Inject constructor() :
    DebugMmsConfigUiStateMapper {

    override fun toSims(sims: ImmutableList<DebugSim>): ImmutableList<DebugSimUiState> {
        return sims
            .map { sim ->
                DebugSimUiState(
                    subId = sim.subId,
                    mccMnc = "(${sim.mcc}/${sim.mnc})",
                )
            }
            .toImmutableList()
    }

    override fun toItems(
        entries: ImmutableList<MmsConfigEntry>,
    ): ImmutableList<MmsConfigItemUiState> {
        return entries
            .map { entry ->
                when (entry.keyType) {
                    MmsConfigKeyType.BOOL -> MmsConfigItemUiState.Toggle(
                        key = entry.key,
                        checked = entry.value.toBoolean(),
                    )

                    MmsConfigKeyType.INT -> MmsConfigItemUiState.Editable(
                        key = entry.key,
                        value = entry.value,
                        isNumeric = true,
                    )

                    MmsConfigKeyType.STRING -> MmsConfigItemUiState.Editable(
                        key = entry.key,
                        value = entry.value,
                        isNumeric = false,
                    )
                }
            }
            .toImmutableList()
    }
}
