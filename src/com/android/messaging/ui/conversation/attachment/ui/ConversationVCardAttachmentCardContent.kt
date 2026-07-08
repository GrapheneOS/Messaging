package com.android.messaging.ui.conversation.attachment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.android.messaging.R
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.data.vcard.model.VCardAvatarPhoto
import com.android.messaging.ui.common.components.attachment.VCardAttachmentCard
import com.android.messaging.ui.common.components.attachment.VCardAttachmentKind
import com.android.messaging.ui.common.components.participant.ParticipantAvatarImage
import com.android.messaging.ui.common.text.asLtrText
import com.android.messaging.ui.conversation.preview.previewVCardUiModel
import com.android.messaging.ui.core.MessagingPreviewColumn

private const val PREVIEW_LONG_CONTACT_TITLE =
    "Alexandria Cassandra Montgomery-Washington from International Partnerships"
private const val PREVIEW_LONG_CONTACT_SUBTITLE =
    "mobile +1 415 555 0198, work +1 415 555 0134, alexandria.montgomery-washington@example.com"
private const val PREVIEW_LONG_LOCATION_TITLE =
    "Northwest corner entrance near the visitor center and underground parking"
private const val PREVIEW_LONG_LOCATION_SUBTITLE =
    "1600 Amphitheatre Parkway, Building 43, Mountain View, California 94043, United States"

@Composable
internal fun ConversationVCardAttachmentCardContent(
    modifier: Modifier = Modifier,
    type: ConversationVCardAttachmentType,
    avatarPhoto: VCardAvatarPhoto?,
    titleText: String?,
    titleTextResId: Int?,
    normalizedDestination: String?,
    subtitleText: String?,
    subtitleTextResId: Int?,
) {
    val avatarImage = remember(avatarPhoto) {
        avatarPhoto
            ?.asReadOnlyByteBuffer()
            ?.let(ParticipantAvatarImage::Bytes)
    }

    VCardAttachmentCard(
        modifier = modifier,
        kind = type.toVCardAttachmentKind(),
        avatarImage = avatarImage,
        displayName = titleText,
        normalizedDestination = normalizedDestination,
        title = resolveLtrVCardText(
            text = titleText,
            textResId = titleTextResId,
        ).orEmpty(),
        subtitle = resolveLtrVCardText(
            text = subtitleText,
            textResId = subtitleTextResId,
        ),
    )
}

internal fun ConversationVCardAttachmentType.toVCardAttachmentKind(): VCardAttachmentKind {
    return when (this) {
        ConversationVCardAttachmentType.CONTACT -> VCardAttachmentKind.Contact
        ConversationVCardAttachmentType.LOCATION -> VCardAttachmentKind.Location
    }
}

@Composable
internal fun resolveLtrVCardText(
    text: String?,
    textResId: Int?,
): String? {
    return text?.asLtrText() ?: textResId?.let { resId ->
        stringResource(resId)
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardAttachmentCardContentLoadedPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            PreviewLoadedConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
            )
            PreviewLoadedConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.LOCATION,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardAttachmentCardContentContactVisualPreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                titleText = "Ada Lovelace",
                titleTextResId = null,
                subtitleText = "+31 6 2222 3333",
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                titleText = "Marina Silva",
                titleTextResId = null,
                subtitleText = "marina@example.com",
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.vcard_tap_hint,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardAttachmentCardContentTextStatePreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            PreviewConversationVCardAttachmentCardContent(
                modifier = Modifier.width(width = 220.dp),
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                titleText = PREVIEW_LONG_CONTACT_TITLE,
                titleTextResId = null,
                subtitleText = PREVIEW_LONG_CONTACT_SUBTITLE,
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                modifier = Modifier.width(width = 220.dp),
                type = ConversationVCardAttachmentType.LOCATION,
                avatarPhoto = null,
                titleText = PREVIEW_LONG_LOCATION_TITLE,
                titleTextResId = null,
                subtitleText = PREVIEW_LONG_LOCATION_SUBTITLE,
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                titleText = "Kai",
                titleTextResId = null,
                subtitleText = null,
                subtitleTextResId = null,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ConversationVCardAttachmentCardContentMetadataStatePreview() {
    MessagingPreviewColumn {
        Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.loading_vcard,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarPhoto = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.failed_loading_vcard,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.LOCATION,
                avatarPhoto = null,
                titleText = null,
                titleTextResId = R.string.notification_location,
                subtitleText = "Shared map pin",
                subtitleTextResId = null,
            )
        }
    }
}

@Composable
private fun PreviewLoadedConversationVCardAttachmentCardContent(
    type: ConversationVCardAttachmentType,
) {
    val uiModel = previewVCardUiModel(type = type)
    PreviewConversationVCardAttachmentCardContent(
        type = uiModel.type,
        avatarPhoto = uiModel.avatarPhoto,
        titleText = uiModel.titleText,
        titleTextResId = uiModel.titleTextResId,
        subtitleText = uiModel.subtitleText,
        subtitleTextResId = uiModel.subtitleTextResId,
        normalizedDestination = uiModel.normalizedDestination,
    )
}

@Composable
private fun PreviewConversationVCardAttachmentCardContent(
    modifier: Modifier = Modifier,
    type: ConversationVCardAttachmentType,
    avatarPhoto: VCardAvatarPhoto?,
    titleText: String?,
    titleTextResId: Int?,
    subtitleText: String?,
    subtitleTextResId: Int?,
    normalizedDestination: String? = null,
) {
    ConversationVCardAttachmentCardContent(
        modifier = modifier,
        type = type,
        avatarPhoto = avatarPhoto,
        titleText = titleText,
        titleTextResId = titleTextResId,
        normalizedDestination = normalizedDestination,
        subtitleText = subtitleText,
        subtitleTextResId = subtitleTextResId,
    )
}
