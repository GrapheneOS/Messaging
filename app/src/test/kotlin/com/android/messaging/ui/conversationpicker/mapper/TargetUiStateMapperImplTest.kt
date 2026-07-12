package com.android.messaging.ui.conversationpicker.mapper

import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.conversationpicker.model.TargetConversation
import com.android.messaging.data.phone.formatter.PhoneNumberFormatter
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TargetUiStateMapperImplTest {

    private val phoneNumberFormatter = mockk<PhoneNumberFormatter> {
        every { formatForDisplay(any()) } answers { "formatted:${firstArg<String>()}" }
    }

    private val contactDestinationFormatter = mockk<ContactDestinationFormatter> {
        every { canonicalize(any()) } answers { "canonical:${firstArg<String>()}" }
    }

    private val resolveAvatarUri = mockk<ResolveAvatarUri>()

    private val mapper = TargetUiStateMapperImpl(
        contactDestinationFormatter = contactDestinationFormatter,
        phoneNumberFormatter = phoneNumberFormatter,
        resolveAvatarUri = resolveAvatarUri,
    )

    @Before
    fun setUp() {
        every { resolveAvatarUri(any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun map_mapsOneToOneConversationWithFormattedAndCanonicalizedDestination() {
        val result = mapper.map(
            persistentListOf(
                conversation(
                    conversationId = "1",
                    name = "Name",
                    normalizedDestination = "+15550100",
                    isGroup = false,
                ),
            ),
        ).single()

        val conversation = result as TargetUiState.Conversation
        assertEquals("1", conversation.conversationId)
        assertEquals("Name", conversation.displayName)
        assertEquals("canonical:+15550100", conversation.normalizedDestination)
        assertEquals("formatted:+15550100", conversation.details)
        assertFalse(conversation.isGroup)
    }

    @Test
    fun map_ignoresDestinationForGroupConversation() {
        val result = mapper.map(
            persistentListOf(
                conversation(
                    conversationId = "2",
                    name = "Name",
                    normalizedDestination = "+15550100",
                    isGroup = true,
                ),
            ),
        ).single()

        val conversation = result as TargetUiState.Conversation
        assertTrue(conversation.isGroup)
        assertNull(conversation.normalizedDestination)
        assertNull(conversation.details)
    }

    @Test
    fun map_setsNullNormalizedDestinationWhenCanonicalizeReturnsEmpty() {
        every { contactDestinationFormatter.canonicalize("+15550100") } returns ""

        val result = mapper.map(
            persistentListOf(
                conversation(
                    normalizedDestination = "+15550100",
                    isGroup = false,
                ),
            ),
        ).single()

        val conversation = result as TargetUiState.Conversation
        assertNull(conversation.normalizedDestination)
    }

    @Test
    fun map_setsNullDestinationFieldsWhenConversationHasNoDestination() {
        val result = mapper.map(
            persistentListOf(
                conversation(normalizedDestination = null, isGroup = false),
            ),
        ).single()

        val conversation = result as TargetUiState.Conversation
        assertNull(conversation.normalizedDestination)
        assertNull(conversation.details)
    }

    @Test
    fun map_usesResolvedAvatarUri() {
        every { resolveAvatarUri("content://icon") } returns "content://resolved"

        val result = mapper.map(
            persistentListOf(conversation(icon = "content://icon")),
        ).single()

        assertEquals("content://resolved", result.avatarUri)
    }

    @Test
    fun map_setsNullAvatarWhenResolverReturnsNull() {
        val result = mapper.map(
            persistentListOf(conversation(icon = "content://icon")),
        ).single()

        assertNull(result.avatarUri)
    }

    @Test
    fun map_setsNullDetailsWhenFormattedDestinationMatchesName() {
        val result = mapper.map(
            persistentListOf(
                conversation(
                    name = "formatted:+15550100",
                    normalizedDestination = "+15550100",
                    isGroup = false,
                ),
            ),
        ).single()

        assertNull(result.details)
    }

    @Test
    fun map_preservesConversationOrder() {
        val result = mapper.map(
            persistentListOf(
                conversation(
                    conversationId = "1",
                    name = "First",
                ),
                conversation(
                    conversationId = "2",
                    name = "Second",
                ),
            ),
        )

        assertEquals(listOf("First", "Second"), result.map { it.displayName })
    }

    @Test
    fun map_returnsEmptyListForEmptyInput() {
        assertEquals(emptyList<TargetUiState>(), mapper.map(persistentListOf()))
    }

    private fun conversation(
        conversationId: String = "1",
        name: String = "Name",
        icon: String? = null,
        normalizedDestination: String? = null,
        isGroup: Boolean = false,
    ): TargetConversation {
        return TargetConversation(
            conversationId = conversationId,
            name = name,
            icon = icon,
            normalizedDestination = normalizedDestination,
            isGroup = isGroup,
        )
    }
}
