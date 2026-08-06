package com.android.messaging.ui.subscription.component

import com.android.messaging.data.conversation.model.ParticipantId

internal const val SIM_SELECTOR_CHIP_TEST_TAG = "sim_selector_chip"
internal const val SIM_SELECTOR_DROPDOWN_TEST_TAG = "sim_selector_dropdown"

internal fun simSelectorItemTestTag(id: ParticipantId): String {
    return "sim_selector_item_${id.value}"
}
