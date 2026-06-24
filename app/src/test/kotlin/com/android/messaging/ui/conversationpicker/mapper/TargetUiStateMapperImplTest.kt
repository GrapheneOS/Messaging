package com.android.messaging.ui.conversationpicker.mapper

import android.net.Uri
import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.conversationpicker.model.TargetConversation
import com.android.messaging.domain.conversation.usecase.avatar.ResolveAvatarUri
import com.android.messaging.ui.conversationpicker.formatter.TargetTextFormatter
import com.android.messaging.ui.conversationpicker.model.TargetUiState
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
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

    private val phoneUtilsInstance = mockk<PhoneUtils>(relaxed = true)

    private val contactDestinationFormatter = mockk<ContactDestinationFormatter> {
        every { canonicalize(any()) } answers { "canonical:${firstArg<String>()}" }
    }

    private val textFormatter = mockk<TargetTextFormatter> {
        every { wrap(any()) } answers { "wrapped:${firstArg<String>()}" }
        every { detailsOrNull(any(), any()) } answers {
            secondArg<String?>()?.let { "details:$it" }
        }
    }

    private val resolveAvatarUri = mockk<ResolveAvatarUri>()

    private val mapper = TargetUiStateMapperImpl(
        contactDestinationFormatter = contactDestinationFormatter,
        resolveAvatarUri = resolveAvatarUri,
        textFormatter = textFormatter,
    )

    @Before
    fun setUp() {
        mockkStatic(PhoneUtils::class)
        every { PhoneUtils.getDefault() } returns phoneUtilsInstance
        every { phoneUtilsInstance.formatForDisplay(any()) } answers {
            "formatted:${firstArg<String>()}"
        }
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

        verify { textFormatter.detailsOrNull(name = "Name", value = "formatted:+15550100") }

        val conversation = result as TargetUiState.Conversation
        assertEquals("1", conversation.conversationId)
        assertEquals("wrapped:Name", conversation.displayName)
        assertEquals("canonical:+15550100", conversation.normalizedDestination)
        assertEquals("details:formatted:+15550100", conversation.details)
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
    fun map_resolvesPrimaryUriWhenIconIsAvatarUri() {
        val avatarIcon = avatarUri(primaryUri = "content://primary")
        every { resolveAvatarUri(avatarIcon) } returns "content://primary"

        val result = mapper.map(
            persistentListOf(conversation(icon = avatarIcon)),
        ).single()

        assertEquals("content://primary", result.avatarUri)
    }

    @Test
    fun map_setsNullAvatarWhenAvatarUriHasNoPrimary() {
        val avatarIcon = avatarUri(primaryUri = null)

        val result = mapper.map(
            persistentListOf(conversation(icon = avatarIcon)),
        ).single()

        assertNull(result.avatarUri)
    }

    @Test
    fun map_usesRawIconWhenIconIsNotAvatarUri() {
        every { resolveAvatarUri("content://plain") } returns "content://plain"

        val result = mapper.map(
            persistentListOf(conversation(icon = "content://plain")),
        ).single()

        assertEquals("content://plain", result.avatarUri)
    }

    @Test
    fun map_setsNullAvatarWhenIconIsNull() {
        val result = mapper.map(
            persistentListOf(conversation(icon = null)),
        ).single()

        assertNull(result.avatarUri)
    }

    @Test
    fun map_setsNullAvatarWhenIconIsBlank() {
        val result = mapper.map(
            persistentListOf(conversation(icon = "   ")),
        ).single()

        assertNull(result.avatarUri)
    }

    @Test
    fun map_setsNullDetailsWhenFormatterReturnsNull() {
        every { textFormatter.detailsOrNull(any(), any()) } returns null

        val result = mapper.map(
            persistentListOf(
                conversation(
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

        assertEquals(listOf("wrapped:First", "wrapped:Second"), result.map { it.displayName })
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

    private fun avatarUri(primaryUri: String?): String {
        return Uri.Builder()
            .scheme(AVATAR_SCHEME)
            .authority(AVATAR_AUTHORITY)
            .apply {
                primaryUri?.let { appendQueryParameter(AVATAR_PRIMARY_URI_PARAM, it) }
            }
            .build()
            .toString()
    }

    private companion object {
        private const val AVATAR_SCHEME = "messaging"
        private const val AVATAR_AUTHORITY = "avatar"
        private const val AVATAR_PRIMARY_URI_PARAM = "m"
    }
}
