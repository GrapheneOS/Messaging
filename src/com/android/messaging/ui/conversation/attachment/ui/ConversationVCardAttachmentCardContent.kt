package com.android.messaging.ui.conversation.attachment.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.messaging.data.conversation.model.attachment.ConversationVCardAttachmentType
import com.android.messaging.ui.common.components.attachment.VCardAttachmentCard
import com.android.messaging.ui.common.components.attachment.VCardAttachmentKind

@Composable
internal fun ConversationVCardAttachmentCardContent(
    modifier: Modifier = Modifier,
    type: ConversationVCardAttachmentType,
    avatarUri: String?,
    titleText: String?,
    titleTextResId: Int?,
    subtitleText: String?,
    subtitleTextResId: Int?,
) {
    VCardAttachmentCard(
        modifier = modifier,
        kind = type.toVCardAttachmentKind(),
        avatarUri = avatarUri,
        title = resolveVCardText(
            text = titleText,
            textResId = titleTextResId,
        ).orEmpty(),
        subtitle = resolveVCardText(
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
internal fun resolveVCardText(
    text: String?,
    textResId: Int?,
): String? {
    return text ?: textResId?.let { resId ->
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
                avatarUri = null,
                titleText = "Ada Lovelace",
                titleTextResId = null,
                subtitleText = "+31 6 2222 3333",
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = PREVIEW_LOCAL_AVATAR_URI,
                titleText = "Marina Silva",
                titleTextResId = null,
                subtitleText = "marina@example.com",
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
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
                avatarUri = null,
                titleText = PREVIEW_LONG_CONTACT_TITLE,
                titleTextResId = null,
                subtitleText = PREVIEW_LONG_CONTACT_SUBTITLE,
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                modifier = Modifier.width(width = 220.dp),
                type = ConversationVCardAttachmentType.LOCATION,
                avatarUri = null,
                titleText = PREVIEW_LONG_LOCATION_TITLE,
                titleTextResId = null,
                subtitleText = PREVIEW_LONG_LOCATION_SUBTITLE,
                subtitleTextResId = null,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
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
                avatarUri = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.loading_vcard,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.CONTACT,
                avatarUri = null,
                titleText = null,
                titleTextResId = R.string.notification_vcard,
                subtitleText = null,
                subtitleTextResId = R.string.failed_loading_vcard,
            )

            PreviewConversationVCardAttachmentCardContent(
                type = ConversationVCardAttachmentType.LOCATION,
                avatarUri = null,
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
        avatarUri = uiModel.avatarUri,
        titleText = uiModel.titleText,
        titleTextResId = uiModel.titleTextResId,
        subtitleText = uiModel.subtitleText,
        subtitleTextResId = uiModel.subtitleTextResId,
    )
}

@Composable
private fun PreviewConversationVCardAttachmentCardContent(
    modifier: Modifier = Modifier,
    type: ConversationVCardAttachmentType,
    avatarUri: String?,
    titleText: String?,
    titleTextResId: Int?,
    subtitleText: String?,
    subtitleTextResId: Int?,
) {
    ConversationVCardAttachmentCardContent(
        modifier = modifier,
        type = type,
        avatarUri = avatarUri,
        titleText = titleText,
        titleTextResId = titleTextResId,
        subtitleText = subtitleText,
        subtitleTextResId = subtitleTextResId,
    )
}
