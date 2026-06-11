package com.android.messaging.ui.conversationpicker.delegate.targetsdelegate

import com.android.messaging.data.conversationpicker.model.TargetConversation
import com.android.messaging.data.conversationpicker.repository.TargetsRepository
import com.android.messaging.ui.conversationpicker.delegate.TargetsDelegateImpl
import com.android.messaging.ui.conversationpicker.mapper.TargetUiStateMapper
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseTargetsDelegateTest {

    protected val repository = mockk<TargetsRepository>()
    protected val conversationMapper = mockk<TargetUiStateMapper>()

    @Before
    fun setUpDefaultStubs() {
        every { repository.observeTargets() } returns flowOf(persistentListOf())
        every { conversationMapper.map(any()) } answers {
            firstArg<ImmutableList<TargetConversation>>()
                .map(::conversationUiState)
                .toImmutableList()
        }
    }

    protected fun TestScope.createDelegate(): TargetsDelegateImpl {
        return TargetsDelegateImpl(
            repository = repository,
            conversationMapper = conversationMapper,
            defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
    }

    protected fun givenRecents(conversations: List<TargetConversation>) {
        every { repository.observeTargets() } returns flowOf(conversations.toImmutableList())
    }

    protected fun givenRecentsSource(): MutableSharedFlow<ImmutableList<TargetConversation>> {
        val source = MutableSharedFlow<ImmutableList<TargetConversation>>(
            replay = 1,
            extraBufferCapacity = 1,
        )
        every { repository.observeTargets() } returns source
        return source
    }

    protected fun shareTargetConversation(
        conversationId: String,
        name: String,
        normalizedDestination: String? = null,
        isGroup: Boolean = false,
        icon: String? = null,
    ): TargetConversation {
        return TargetConversation(
            conversationId = conversationId,
            name = name,
            icon = icon,
            normalizedDestination = normalizedDestination,
            isGroup = isGroup,
        )
    }

    private fun conversationUiState(
        conversation: TargetConversation,
    ): TargetUiState.Conversation {
        return TargetUiState.Conversation(
            conversationId = conversation.conversationId,
            normalizedDestination = conversation.normalizedDestination,
            displayName = conversation.name,
            details = conversation.normalizedDestination,
            avatarUri = conversation.icon,
            isGroup = conversation.isGroup,
        )
    }
}
