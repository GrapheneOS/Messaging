package com.android.messaging.ui.conversationpicker.viewmodel

import com.android.messaging.data.conversation.model.draft.ConversationDraft
import com.android.messaging.testutil.TEST_CONTACT_DESTINATION
import com.android.messaging.testutil.TEST_CONTACT_DISPLAY_NAME
import com.android.messaging.testutil.TEST_CONTACT_SECONDARY_TEXT
import com.android.messaging.testutil.conversationTarget
import com.android.messaging.testutil.syntheticPhoneItem
import com.android.messaging.ui.conversationpicker.model.ConversationPickerAction as Action
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ConversationPickerViewModelRoutingTest : BaseConversationPickerViewModelTest() {

    @Test
    fun selectionToggled_togglesSelectionOnTargetsDelegate() {
        val viewModel = createViewModel()
        val target = conversationTarget(conversationId = "1")

        viewModel.onAction(Action.SelectionToggled(target))

        verify { targetsDelegate.toggleSelection(target) }
    }

    @Test
    fun selectionCleared_clearsSelection() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.SelectionCleared)

        verify { targetsDelegate.clearSelection() }
    }

    @Test
    fun searchOpened_activatesSearch() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.SearchOpened)

        verify { targetsDelegate.setSearchActive(true) }
    }

    @Test
    fun searchClosed_deactivatesSearch() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.SearchClosed)

        verify { targetsDelegate.setSearchActive(false) }
        verify { recipientPickerDelegate.clearQuery() }
    }

    @Test
    fun searchQueryChanged_forwardsQuery() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.SearchQueryChanged("alex"))

        verify { targetsDelegate.setSearchQuery("alex") }
        verify { recipientPickerDelegate.onQueryChanged("alex") }
    }

    @Test
    fun contactDestinationToggled_mapsAndTogglesSelection() {
        val viewModel = createViewModel()
        val item = syntheticPhoneItem()

        viewModel.onAction(
            Action.ContactDestinationToggled(
                item = item,
                destination = TEST_CONTACT_DESTINATION,
            ),
        )

        verify {
            targetsDelegate.toggleSelection(
                TargetUiState.Contact(
                    destination = TEST_CONTACT_DESTINATION,
                    normalizedDestination = TEST_CONTACT_DESTINATION,
                    displayName = TEST_CONTACT_DISPLAY_NAME,
                    details = TEST_CONTACT_SECONDARY_TEXT,
                    avatarUri = null,
                ),
            )
        }
    }

    @Test
    fun loadMoreContacts_forwardsToDelegate() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.LoadMoreContacts)

        verify { recipientPickerDelegate.onLoadMore() }
    }

    @Test
    fun loadMoreRecent_forwardsToDelegate() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.LoadMoreRecent)

        verify { targetsDelegate.loadMoreRecent() }
    }

    @Test
    fun collapseRecent_forwardsToDelegate() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.CollapseRecent)

        verify { targetsDelegate.collapseRecent() }
    }

    @Test
    fun contactsPermissionGranted_forwardsToDelegate() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.ContactsPermissionGranted)

        verify { recipientPickerDelegate.refresh() }
    }

    @Test
    fun proceedToReviewClicked_entersReview() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.ProceedToReviewClicked)

        verify { draftDelegate.enterReview() }
    }

    @Test
    fun draftResolved_forwardsDraftToDelegate() {
        val viewModel = createViewModel()
        val draft = ConversationDraft(messageText = "shared")

        viewModel.onAction(Action.DraftResolved(draft))

        verify { draftDelegate.resolveDraft(draft) }
    }

    @Test
    fun draftResolved_withNull_forwardsNull() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.DraftResolved(null))

        verify { draftDelegate.resolveDraft(null) }
    }

    @Test
    fun draftTextChanged_forwardsText() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.DraftTextChanged("new text"))

        verify { draftDelegate.setDraftText("new text") }
    }

    @Test
    fun draftAttachmentRemoved_forwardsId() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.DraftAttachmentRemoved("content://a"))

        verify { draftDelegate.removeDraftAttachment("content://a") }
    }

    @Test
    fun draftSubjectCleared_forwardsToDelegate() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.DraftSubjectCleared)

        verify { draftDelegate.clearDraftSubject() }
    }

    @Test
    fun reviewDismissed_exitsReview() {
        val viewModel = createViewModel()

        viewModel.onAction(Action.ReviewDismissed)

        verify { draftDelegate.exitReview() }
    }
}
