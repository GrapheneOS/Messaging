package com.android.messaging.ui.conversation.messages.mapper

import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentMetadata
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.conversation.attachment.mapper.ConversationVCardAttachmentUiModelMapperImpl
import com.android.messaging.ui.conversation.attachment.model.ConversationVCardAttachmentUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationVCardAttachmentUiModelMapperImplTest {

    private val mapper = ConversationVCardAttachmentUiModelMapperImpl()

    @Test
    fun map_loading_returnsDefaultContactLoadingUiModel() {
        val uiModel = mapper.map(
            metadata = ConversationVCardAttachmentMetadata.Loading,
        )

        assertEquals(
            ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.loading_vcard,
            ),
            uiModel,
        )
    }

    @Test
    fun map_loadedContact_usesContactTextsAndFallbackSubtitleResource() {
        val uiModel = mapper.map(
            metadata = ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                displayName = "Sam Rivera",
                details = null,
                locationAddress = null,
            ),
        )

        assertEquals(
            ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = "Sam Rivera",
                titleTextResId = null,
                subtitleText = null,
                subtitleTextResId = R.string.vcard_tap_hint,
            ),
            uiModel,
        )
    }

    @Test
    fun map_loadedLocation_prefersLocationAddressAndFallbackLocationTitle() {
        val uiModel = mapper.map(
            metadata = ConversationVCardAttachmentMetadata.Loaded(
                type = ConversationVCardAttachmentType.LOCATION,
                avatarUri = null,
                displayName = null,
                details = "New York",
                locationAddress = "25 11th Ave New York NY 10011 United States",
            ),
        )

        assertEquals(
            ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.LOCATION,
                titleText = null,
                titleTextResId = R.string.notification_location,
                subtitleText = "25 11th Ave New York NY 10011 United States",
                subtitleTextResId = null,
            ),
            uiModel,
        )
    }

    @Test
    fun map_failed_returnsFailedSubtitleResource() {
        val uiModel = mapper.map(
            metadata = ConversationVCardAttachmentMetadata.Failed,
        )

        assertEquals(
            ConversationVCardAttachmentUiModel(
                type = ConversationVCardAttachmentType.CONTACT,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.failed_loading_vcard,
            ),
            uiModel,
        )
    }
}
