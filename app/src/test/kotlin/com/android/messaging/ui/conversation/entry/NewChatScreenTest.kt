package com.android.messaging.ui.conversation.entry

import androidx.activity.ComponentActivity
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.android.messaging.ui.common.components.ListDetailPaneSide
import com.android.messaging.ui.common.components.LocalIsListDetailPane
import com.android.messaging.ui.common.components.LocalListDetailPaneSide
import com.android.messaging.ui.conversation.NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG
import com.android.messaging.ui.conversation.entry.model.NewChatUiState
import com.android.messaging.ui.conversation.navigation.NewChatNavKey
import com.android.messaging.ui.conversation.navigation.conversationDetailPaneMetadata
import com.android.messaging.ui.conversation.navigation.conversationListPaneMetadata
import com.android.messaging.ui.conversation.newChatContactRowTestTag
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.navigation.AppNavDisplay
import com.android.messaging.ui.navigation.rememberAppSceneStrategies
import com.android.messaging.ui.recipientselection.component.row.CONTACT_ID
import com.android.messaging.ui.recipientselection.component.row.MOBILE_NORMALIZED_DESTINATION
import com.android.messaging.ui.recipientselection.component.row.contactItem
import com.android.messaging.ui.recipientselection.component.row.selectedRecipient
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class NewChatScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val defaultContactRowTestTag = newChatContactRowTestTag(
        contactId = "contact:$CONTACT_ID",
    )

    @Test
    fun contactClick_forwardsNormalizedDestinationToScreenModel() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(defaultContactRowTestTag)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onContactClicked(destination = MOBILE_NORMALIZED_DESTINATION)
            }
        }
    }

    @Test
    fun queryAndLoadMore_forwardToScreenModel() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = contactItems(count = 30),
                    canLoadMore = true,
                ),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNode(matcher = hasSetTextAction())
            .performTextInput("Ada")
        composeTestRule
            .onNode(matcher = hasScrollToIndexAction())
            .performScrollToIndex(index = 30)
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onQueryChanged(query = "Ada")
                screenModel.onLoadMore()
            }
        }
    }

    @Test
    fun createGroupButton_forwardsRequestToScreenModel() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(testTag = NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG)
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onCreateGroupRequested()
            }
        }
    }

    @Test
    fun createGroupMode_togglesRecipientsAndConfirmsSelection() {
        val selectedRecipient = selectedRecipient()
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                isCreatingGroup = true,
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
                selectedGroupRecipients = persistentListOf(selectedRecipient),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(defaultContactRowTestTag)
            .assertIsSelected()
            .performClick()
        composeTestRule
            .onNodeWithTag(NEW_CHAT_CREATE_GROUP_NEXT_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onCreateGroupRecipientClicked(recipient = selectedRecipient)
                screenModel.onCreateGroupConfirmed()
            }
        }
    }

    @Test
    fun longPress_forwardsSelectedRecipientToScreenModel() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(contactItem()),
                ),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNodeWithTag(defaultContactRowTestTag)
            .performSemanticsAction(SemanticsActions.OnLongClick)

        composeTestRule.runOnIdle {
            verify(exactly = 1) {
                screenModel.onContactLongClicked(recipient = selectedRecipient())
            }
        }
    }

    @Test
    fun resolvingState_showsRowProgressIndicatorOnlyForMatchingRecipient() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                isResolvingConversation = true,
                isResolvingConversationIndicatorVisible = true,
                recipientPickerUiState = RecipientPickerUiState(
                    items = persistentListOf(
                        contactItem(
                            id = 1,
                            normalizedDestination = MOBILE_NORMALIZED_DESTINATION,
                        ),
                        contactItem(
                            id = 2,
                            displayName = "Grace Hopper",
                            destination = "+1 555 0200",
                            normalizedDestination = "+15550200",
                        ),
                    ),
                ),
                resolvingRecipientDestination = MOBILE_NORMALIZED_DESTINATION,
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onAllNodesWithTag(NEW_CHAT_CONTACT_RESOLVING_INDICATOR_TEST_TAG)
            .assertCountEquals(expectedSize = 1)
    }

    @Test
    fun createGroupButton_staysVisibleWhileTheContactListScrolls() {
        val screenModel = createScreenModel(
            initialUiState = NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = contactItems(count = 30),
                ),
            ),
        )

        setContent(screenModel = screenModel)

        composeTestRule
            .onNode(matcher = hasScrollToIndexAction())
            .performScrollToIndex(index = 29)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(testTag = NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun leavingCreateGroupMode_showsTheCreateGroupButtonAgain() {
        val uiState = MutableStateFlow(
            NewChatUiState(
                recipientPickerUiState = RecipientPickerUiState(
                    items = contactItems(count = 30),
                ),
            ),
        )

        setContent(screenModel = createScreenModel(uiStateFlow = uiState))

        uiState.value = uiState.value.copy(isCreatingGroup = true)
        composeTestRule.waitForIdle()
        uiState.value = uiState.value.copy(isCreatingGroup = false)
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(testTag = NEW_CHAT_CREATE_GROUP_BUTTON_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun opening_inSinglePane_focusesTheRecipientField() {
        setContent(screenModel = createScreenModel(initialUiState = NewChatUiState()))

        composeTestRule
            .onNode(matcher = hasSetTextAction())
            .assertIsFocused()
    }

    @Test
    fun opening_inListDetailPane_focusesTheRecipientField() {
        setContent(
            screenModel = createScreenModel(initialUiState = NewChatUiState()),
            isListDetailPane = true,
        )

        composeTestRule
            .onNode(matcher = hasSetTextAction())
            .assertIsFocused()
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Config(qualifiers = TABLET_QUALIFIERS)
    @Test
    fun paneCountChange_keepsTheRecipientFieldFocused() {
        val screenModel = createScreenModel(initialUiState = NewChatUiState())
        val backStack = mutableStateListOf<NavKey>(ConversationListNavKey, NewChatNavKey)
        var showsTwoPanes by mutableStateOf(false)

        composeTestRule.setContent {
            val maxHorizontalPartitions = when {
                showsTwoPanes -> TWO_PANES
                else -> SINGLE_PANE
            }

            AppTheme {
                AppNavDisplay(
                    backStack = backStack,
                    entryProvider = entryProvider {
                        entry<ConversationListNavKey>(metadata = conversationListPaneMetadata()) {}
                        entry<NewChatNavKey>(metadata = conversationDetailPaneMetadata()) {
                            NewChatScreen(
                                screenModel = screenModel,
                                effectHandler = mockk(relaxed = true),
                                onNavigateBack = {},
                                onNavigateToConversation = { _, _ -> },
                                autoFocusQueryKey = LocalNavAnimatedContentScope.current,
                            )
                        }
                    },
                    onBack = {},
                    sceneStrategies = rememberAppSceneStrategies(
                        additionalStrategies = listOf(
                            rememberListDetailSceneStrategy(
                                backNavigationBehavior = BackNavigationBehavior.PopLatest,
                                directive = PaneScaffoldDirective.Default.copy(
                                    maxHorizontalPartitions = maxHorizontalPartitions,
                                ),
                            ),
                        ),
                    ),
                    showsTwoPanes = showsTwoPanes,
                )
            }
        }

        for (twoPanes in listOf(true, false)) {
            showsTwoPanes = twoPanes

            composeTestRule
                .onNode(matcher = hasSetTextAction())
                .assertIsFocused()
        }
    }

    private fun setContent(
        screenModel: NewChatScreenModel,
        isListDetailPane: Boolean = false,
    ) {
        composeTestRule.setContent {
            AppTheme {
                CompositionLocalProvider(
                    LocalIsListDetailPane provides isListDetailPane,
                    LocalListDetailPaneSide provides ListDetailPaneSide.End,
                ) {
                    NewChatScreen(
                        screenModel = screenModel,
                        effectHandler = mockk(relaxed = true),
                        onNavigateBack = {},
                        onNavigateToConversation = { _, _ -> },
                    )
                }
            }
        }
    }

    private fun contactItems(count: Int): ImmutableList<RecipientPickerListItem.Contact> {
        return persistentListOf(
            *Array(size = count) { index ->
                val suffix = index.toString().padStart(length = 4, padChar = '0')

                contactItem(
                    id = index.toLong(),
                    displayName = "Contact $index",
                    destination = "+1 555 $suffix",
                    normalizedDestination = "+1555$suffix",
                )
            },
        )
    }

    private fun createScreenModel(initialUiState: NewChatUiState): NewChatScreenModel {
        return createScreenModel(uiStateFlow = MutableStateFlow(value = initialUiState))
    }

    private fun createScreenModel(
        uiStateFlow: MutableStateFlow<NewChatUiState>,
    ): NewChatScreenModel {
        return mockk<NewChatScreenModel>(relaxed = true) {
            every { effects } returns MutableSharedFlow()
            every { navigationEvents } returns MutableSharedFlow()
            every { uiState } returns uiStateFlow
        }
    }

    private companion object {
        private const val SINGLE_PANE = 1
        private const val TWO_PANES = 2
        private const val TABLET_QUALIFIERS = "w1280dp-h800dp"
    }
}
