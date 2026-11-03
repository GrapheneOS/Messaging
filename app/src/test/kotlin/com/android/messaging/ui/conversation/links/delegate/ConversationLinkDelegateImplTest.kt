package com.android.messaging.ui.conversation.links.delegate

import app.cash.turbine.test
import com.android.messaging.data.appsettings.model.LinkOpeningMode
import com.android.messaging.data.appsettings.repository.AppSettingsRepository
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.testutil.MainDispatcherRule
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.ui.conversation.links.model.ConversationLinkConfirmationUiState
import com.android.messaging.ui.conversation.screen.model.ConversationScreenEffect
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class ConversationLinkDelegateImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appSettingsRepository = mockk<AppSettingsRepository>()

    @Test
    fun onExternalUriClicked_askBeforeOpening_showsConfirmationWithoutOpening() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenLinkOpeningMode(mode = LinkOpeningMode.AskBeforeOpening)
            val delegate = createBoundDelegate()

            delegate.effects.test {
                delegate.onExternalUriClicked(uri = LINK_URI)
                runCurrent()

                assertEquals(LINK_CONFIRMATION, delegate.state.value)
                expectNoEvents()
            }
        }
    }

    @Test
    fun confirmation_webLinkWithUserInfo_showsRealHost() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val uri = "https://bank.example@attacker.example/login"

            assertEquals(
                ConversationLinkConfirmationUiState(
                    uri = uri,
                    destination = "attacker.example",
                    showsFullUri = true,
                ),
                confirmationFor(uri = uri),
            )
        }
    }

    @Test
    fun confirmation_phoneLink_showsDecodedNumberOnly() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            assertEquals(
                ConversationLinkConfirmationUiState(
                    uri = PHONE_URI,
                    destination = "+31 6 2222 3333",
                    showsFullUri = false,
                ),
                confirmationFor(uri = PHONE_URI),
            )
        }
    }

    @Test
    fun confirmation_emailLink_showsDecodedAddressOnly() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            assertEquals(
                ConversationLinkConfirmationUiState(
                    uri = EMAIL_URI,
                    destination = "preview@example.com",
                    showsFullUri = false,
                ),
                confirmationFor(uri = EMAIL_URI),
            )
        }
    }

    @Test
    fun confirmation_addressLink_showsDecodedQueryOnly() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            assertEquals(
                ConversationLinkConfirmationUiState(
                    uri = ADDRESS_URI,
                    destination = "1600 Amphitheatre Parkway, Mountain View, CA",
                    showsFullUri = false,
                ),
                confirmationFor(uri = ADDRESS_URI),
            )
        }
    }

    @Test
    fun confirmation_linkWithoutReadableTarget_fallsBackToUri() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val uri = "tel:"

            assertEquals(
                ConversationLinkConfirmationUiState(
                    uri = uri,
                    destination = uri,
                    showsFullUri = false,
                ),
                confirmationFor(uri = uri),
            )
        }
    }

    @Test
    fun onExternalUriClicked_openOnTap_opensWithoutConfirmation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenLinkOpeningMode(mode = LinkOpeningMode.OpenOnTap)
            val delegate = createBoundDelegate()

            delegate.effects.test {
                delegate.onExternalUriClicked(uri = LINK_URI)

                assertEquals(ConversationScreenEffect.OpenExternalUri(uri = LINK_URI), awaitItem())
                assertNull(delegate.state.value)
            }
        }
    }

    @Test
    fun onExternalUriClicked_doNotOpen_ignoresClick() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenLinkOpeningMode(mode = LinkOpeningMode.DoNotOpen)
            val delegate = createBoundDelegate()

            delegate.effects.test {
                delegate.onExternalUriClicked(uri = LINK_URI)
                runCurrent()

                assertNull(delegate.state.value)
                expectNoEvents()
            }
        }
    }

    @Test
    fun onExternalUriClicked_readsCurrentModeOnEveryClick() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            coEvery { appSettingsRepository.getLinkOpeningMode() } returnsMany listOf(
                LinkOpeningMode.OpenOnTap,
                LinkOpeningMode.AskBeforeOpening,
            )
            val delegate = createBoundDelegate()

            delegate.effects.test {
                delegate.onExternalUriClicked(uri = LINK_URI)
                assertEquals(ConversationScreenEffect.OpenExternalUri(uri = LINK_URI), awaitItem())

                delegate.onExternalUriClicked(uri = LINK_URI)
                runCurrent()

                assertEquals(LINK_CONFIRMATION, delegate.state.value)
                expectNoEvents()
            }
        }
    }

    @Test
    fun confirmOpenLink_opensConfirmedUriOnceAndClearsConfirmation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenLinkOpeningMode(mode = LinkOpeningMode.AskBeforeOpening)
            val delegate = createBoundDelegate()
            delegate.onExternalUriClicked(uri = LINK_URI)
            runCurrent()

            delegate.effects.test {
                delegate.confirmOpenLink(confirmation = LINK_CONFIRMATION)
                delegate.confirmOpenLink(confirmation = LINK_CONFIRMATION)

                assertEquals(ConversationScreenEffect.OpenExternalUri(uri = LINK_URI), awaitItem())
                assertNull(delegate.state.value)
                runCurrent()
                expectNoEvents()
            }
        }
    }

    @Test
    fun confirmOpenLink_ofAReplacedConfirmation_opensNothing() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenLinkOpeningMode(mode = LinkOpeningMode.AskBeforeOpening)
            val delegate = createBoundDelegate()
            delegate.onExternalUriClicked(uri = LINK_URI)
            runCurrent()

            delegate.effects.test {
                delegate.confirmOpenLink(confirmation = OTHER_LINK_CONFIRMATION)
                runCurrent()

                assertEquals(LINK_CONFIRMATION, delegate.state.value)
                expectNoEvents()
            }
        }
    }

    @Test
    fun onExternalUriClicked_newerClickCancelsAPendingOne() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val pendingRead = CompletableDeferred<LinkOpeningMode>()
            coEvery {
                appSettingsRepository.getLinkOpeningMode()
            } coAnswers { pendingRead.await() } andThen LinkOpeningMode.AskBeforeOpening
            val delegate = createBoundDelegate()
            delegate.onExternalUriClicked(uri = OTHER_LINK_URI)
            runCurrent()

            delegate.onExternalUriClicked(uri = LINK_URI)
            runCurrent()
            assertEquals(LINK_CONFIRMATION, delegate.state.value)
            delegate.dismissOpenLinkConfirmation()
            pendingRead.complete(value = LinkOpeningMode.AskBeforeOpening)
            runCurrent()

            assertNull(delegate.state.value)
        }
    }

    @Test
    fun dismissOpenLinkConfirmation_cancelsAPendingClick() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val pendingRead = givenPendingLinkOpeningModeRead()
            val delegate = createBoundDelegate()
            delegate.onExternalUriClicked(uri = LINK_URI)
            runCurrent()

            delegate.dismissOpenLinkConfirmation()
            pendingRead.complete(value = LinkOpeningMode.AskBeforeOpening)
            runCurrent()

            assertNull(delegate.state.value)
        }
    }

    @Test
    fun dismissOpenLinkConfirmation_clearsConfirmationWithoutOpening() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenLinkOpeningMode(mode = LinkOpeningMode.AskBeforeOpening)
            val delegate = createBoundDelegate()
            delegate.onExternalUriClicked(uri = LINK_URI)
            runCurrent()

            delegate.effects.test {
                delegate.dismissOpenLinkConfirmation()
                delegate.confirmOpenLink(confirmation = LINK_CONFIRMATION)
                runCurrent()

                assertNull(delegate.state.value)
                expectNoEvents()
            }
        }
    }

    @Test
    fun conversationChange_clearsPendingConfirmation() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            givenLinkOpeningMode(mode = LinkOpeningMode.AskBeforeOpening)
            val conversationIdFlow = MutableStateFlow<ConversationId?>(value = CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            delegate.onExternalUriClicked(uri = LINK_URI)
            runCurrent()
            assertEquals(LINK_CONFIRMATION, delegate.state.value)

            conversationIdFlow.value = OTHER_CONVERSATION_ID
            runCurrent()

            assertNull(delegate.state.value)
        }
    }

    @Test
    fun conversationChange_cancelsAPendingClick() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val pendingRead = givenPendingLinkOpeningModeRead()
            val conversationIdFlow = MutableStateFlow<ConversationId?>(value = CONVERSATION_ID)
            val delegate = createBoundDelegate(conversationIdFlow = conversationIdFlow)
            delegate.onExternalUriClicked(uri = LINK_URI)
            runCurrent()

            conversationIdFlow.value = OTHER_CONVERSATION_ID
            runCurrent()
            pendingRead.complete(value = LinkOpeningMode.AskBeforeOpening)
            runCurrent()

            assertNull(delegate.state.value)
        }
    }

    @Test
    fun onExternalUriClicked_beforeBind_doesNotReadModeOrOpen() {
        runTest(context = mainDispatcherRule.testDispatcher) {
            val delegate = createDelegate()

            delegate.effects.test {
                delegate.onExternalUriClicked(uri = LINK_URI)
                runCurrent()

                assertNull(delegate.state.value)
                expectNoEvents()
            }
            coVerify(exactly = 0) { appSettingsRepository.getLinkOpeningMode() }
        }
    }

    private fun TestScope.confirmationFor(uri: String): ConversationLinkConfirmationUiState? {
        givenLinkOpeningMode(mode = LinkOpeningMode.AskBeforeOpening)
        val delegate = createBoundDelegate()
        delegate.onExternalUriClicked(uri = uri)
        runCurrent()
        return delegate.state.value
    }

    private fun givenLinkOpeningMode(mode: LinkOpeningMode) {
        coEvery { appSettingsRepository.getLinkOpeningMode() } returns mode
    }

    private fun givenPendingLinkOpeningModeRead(): CompletableDeferred<LinkOpeningMode> {
        val pendingRead = CompletableDeferred<LinkOpeningMode>()
        coEvery { appSettingsRepository.getLinkOpeningMode() } coAnswers { pendingRead.await() }
        return pendingRead
    }

    private fun createDelegate(): ConversationLinkDelegateImpl {
        return ConversationLinkDelegateImpl(
            appSettingsRepository = appSettingsRepository,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    private fun TestScope.createBoundDelegate(
        conversationIdFlow: MutableStateFlow<ConversationId?> =
            MutableStateFlow(value = CONVERSATION_ID),
    ): ConversationLinkDelegateImpl {
        return createDelegate().also { delegate ->
            delegate.bind(scope = backgroundScope, conversationIdFlow = conversationIdFlow)
            runCurrent()
        }
    }

    private companion object {
        private const val ADDRESS_URI =
            "geo:0,0?q=1600%20Amphitheatre%20Parkway%2C%20Mountain%20View%2C%20CA"
        private const val EMAIL_URI = "mailto:preview%40example.com"
        private const val PHONE_URI = "tel:%2B31%206%202222%203333"
        private const val LINK_URI = "https://example.com/account/verify"
        private const val OTHER_LINK_URI = "https://example.org/"
        private val LINK_CONFIRMATION = ConversationLinkConfirmationUiState(
            uri = LINK_URI,
            destination = "example.com",
            showsFullUri = true,
        )
        private val OTHER_LINK_CONFIRMATION = ConversationLinkConfirmationUiState(
            uri = OTHER_LINK_URI,
            destination = "example.org",
            showsFullUri = true,
        )
        private val OTHER_CONVERSATION_ID = ConversationId("other-conversation")
    }
}
