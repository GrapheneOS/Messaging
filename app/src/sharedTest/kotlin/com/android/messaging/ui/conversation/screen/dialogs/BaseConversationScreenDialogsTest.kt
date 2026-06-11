package com.android.messaging.ui.conversation.screen.dialogs

import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.composer.model.ConversationComposerUiState
import com.android.messaging.ui.conversation.screen.BaseConversationScreenTest
import com.android.messaging.ui.conversation.screen.ConversationScreenDialogs
import com.android.messaging.ui.conversation.screen.ConversationScreenModel
import com.android.messaging.ui.conversation.screen.model.ConversationAttachmentLimitWarning
import com.android.messaging.ui.conversation.screen.model.ConversationMessageDeleteConfirmationUiState
import com.android.messaging.ui.conversation.screen.model.ConversationMessageSelectionUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.core.AppTheme
import io.mockk.mockk

internal abstract class BaseConversationScreenDialogsTest : BaseConversationScreenTest() {

    protected fun setDialogsContent(
        uiState: ConversationScreenScaffoldUiState,
        screenModel: ConversationScreenModel = mockk(relaxed = true),
    ): ConversationScreenModel {
        composeTestRule.setContent {
            AppTheme {
                ConversationScreenDialogs(
                    uiState = uiState,
                    screenModel = screenModel,
                )
            }
        }
        return screenModel
    }

    protected fun createDialogUiState(
        attachmentLimitWarning: ConversationAttachmentLimitWarning? = null,
        deleteConfirmation: ConversationMessageDeleteConfirmationUiState? = null,
        isDeleteConversationConfirmationVisible: Boolean = false,
        isSubjectDialogVisible: Boolean = false,
        subjectText: String = "",
    ): ConversationScreenScaffoldUiState {
        return ConversationScreenScaffoldUiState(
            attachmentLimitWarning = attachmentLimitWarning,
            isDeleteConversationConfirmationVisible = isDeleteConversationConfirmationVisible,
            isSubjectDialogVisible = isSubjectDialogVisible,
            composer = ConversationComposerUiState(
                subjectText = subjectText,
            ),
            selection = ConversationMessageSelectionUiState(
                deleteConfirmation = deleteConfirmation,
            ),
        )
    }

    protected fun text(resourceId: Int): String {
        return targetContext.getString(resourceId)
    }

    protected fun quantityText(
        resourceId: Int,
        quantity: Int,
    ): String {
        return targetContext.resources.getQuantityString(resourceId, quantity, quantity)
    }

    protected fun okText(): String {
        return targetContext.getString(android.R.string.ok)
    }

    protected fun cancelText(): String {
        return targetContext.getString(android.R.string.cancel)
    }

    protected fun deleteConversationTitle(): String {
        return targetContext.resources.getQuantityString(
            R.plurals.delete_conversations_confirmation_dialog_title,
            1,
            1,
        )
    }
}
