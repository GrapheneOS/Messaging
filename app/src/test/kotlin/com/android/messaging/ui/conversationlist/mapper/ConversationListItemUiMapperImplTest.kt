package com.android.messaging.ui.conversationlist.mapper

import android.content.Context
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.domain.conversation.usecase.participant.CanShowOrAddContact
import com.android.messaging.domain.conversation.usecase.participant.IsContactSaved
import com.android.messaging.domain.conversation.usecase.telephony.CanPlacePhoneCall
import com.android.messaging.ui.conversationlist.conversationItem
import com.android.messaging.ui.conversationlist.model.ConversationListAvatarUiModel
import com.android.messaging.util.OsUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

internal class ConversationListItemUiMapperImplTest {

    private val phoneNumberFormatter = mockk<PhoneNumberFormatter> {
        every { formatForDisplay(NORMALIZED_DESTINATION) } returns DISPLAY_DESTINATION
    }

    private val mapper = ConversationListItemUiMapperImpl(
        context = mockk<Context>(relaxed = true),
        canPlacePhoneCall = mockk<CanPlacePhoneCall>(relaxed = true),
        canShowOrAddContact = mockk<CanShowOrAddContact>(relaxed = true),
        isContactSaved = mockk<IsContactSaved>(relaxed = true),
        phoneNumberFormatter = phoneNumberFormatter,
        resolveAvatarUri = mockk<ResolveAvatarUri>(relaxed = true),
    )

    @Before
    fun setUp() {
        mockkStatic(OsUtil::class)
        every { OsUtil.isSecondaryUser() } returns false
    }

    @After
    fun tearDown() {
        unmockkStatic(OsUtil::class)
    }

    @Test
    fun map_oneOnOne_formatsSubtitleAndKeepsNormalizedDestinationCanonical() {
        val avatar = mapAvatar(isGroup = false)

        assertEquals(DISPLAY_DESTINATION, avatar.subtitle)
        assertEquals(NORMALIZED_DESTINATION, avatar.normalizedDestination)
    }

    @Test
    fun map_group_hasNoSubtitle() {
        assertNull(mapAvatar(isGroup = true).subtitle)
    }

    @Test
    fun map_unsavedNumberTitledByItsOwnDestination_hasNoSubtitle() {
        assertNull(mapAvatar(isGroup = false, title = DISPLAY_DESTINATION).subtitle)
    }

    private fun mapAvatar(
        isGroup: Boolean,
        title: String? = null,
    ): ConversationListAvatarUiModel {
        val item = conversationItem(conversationId = CONVERSATION_ID)

        return mapper
            .map(
                item = item.copy(
                    title = title ?: item.title,
                    participant = item.participant.copy(
                        otherNormalizedDestination = NORMALIZED_DESTINATION,
                        isGroup = isGroup,
                    ),
                ),
                isSelected = false,
                isOpened = false,
            )
            .avatar
    }

    private companion object {
        private val CONVERSATION_ID = ConversationId("conversation-1")
        private const val NORMALIZED_DESTINATION = "+15550123"
        private const val DISPLAY_DESTINATION = "+1 555-0123"
    }
}
