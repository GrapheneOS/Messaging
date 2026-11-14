package com.android.messaging.ui.conversationlist.chats.mapper

import com.android.messaging.data.appsettings.model.ConversationSwipeOption
import com.android.messaging.data.appsettings.model.ConversationSwipeSettings
import com.android.messaging.ui.conversationlist.common.item.ConversationSwipeKind
import com.android.messaging.ui.conversationlist.common.list.ConversationListSwipeSpec
import org.junit.Assert.assertEquals
import org.junit.Test

internal class ConversationListSwipeSpecMapperTest {

    @Test
    fun toSwipeSpec_defaultSettings_matchesDefaultSpec() {
        assertEquals(
            ConversationListSwipeSpec.Default,
            ConversationSwipeSettings.Default.toSwipeSpec(),
        )
    }

    @Test
    fun toSwipeSpec_mapsEachOptionToMatchingKind() {
        val expectedKinds = mapOf(
            ConversationSwipeOption.ToggleRead to ConversationSwipeKind.ToggleRead,
            ConversationSwipeOption.Archive to ConversationSwipeKind.Archive,
            ConversationSwipeOption.Delete to ConversationSwipeKind.Delete,
        )

        ConversationSwipeOption.entries.forEach { option ->
            val spec = ConversationSwipeSettings(
                startToEnd = option,
                endToStart = option,
            ).toSwipeSpec()

            assertEquals(expectedKinds.getValue(option), spec.startToEnd)
            assertEquals(expectedKinds.getValue(option), spec.endToStart)
        }
    }
}
