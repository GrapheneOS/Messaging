package com.android.messaging.ui.shareintent.screen.mapper

import android.net.Uri
import com.android.messaging.data.contact.formatter.ContactDestinationFormatter
import com.android.messaging.data.shareintent.model.ShareTargetConversation
import com.android.messaging.ui.shareintent.screen.formatter.ShareTargetTextFormatter
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import com.android.messaging.util.PhoneUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ShareTargetUiStateMapperImplTest {

    private val phoneUtilsInstance = mockk<PhoneUtils>(relaxed = true)

    private val contactDestinationFormatter = mockk<ContactDestinationFormatter> {
        every { canonicalize(any()) } answers { "canonical:${firstArg<String>()}" }
    }

    private val textFormatter = mockk<ShareTargetTextFormatter> {
        every { wrap(any()) } answers { "wrapped:${firstArg<String>()}" }
        every { detailsOrNull(any(), any()) } answers {
            secondArg<String?>()?.let { "details:$it" }
        }
    }

    private val mapper = ShareTargetUiStateMapperImpl(
        contactDestinationFormatter = contactDestinationFormatter,
        textFormatter = textFormatter,
    )

    @Before
    fun setUp() {
        mockkStatic(PhoneUtils::class)
        every { PhoneUtils.getDefault() } returns phoneUtilsInstance
        every { phoneUtilsInstance.formatForDisplay(any()) } answers {
            "formatted:${firstArg<String>()}"
        }
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

        val conversation = result as ShareTargetUiState.Conversation
        assertEquals("1", conversation.conversationId)
        assertEquals("wrapped:Name", conversation.displayName)
        assertEquals("canonical:+15550100", conversation.normalizedDestination)
        assertEquals("details:formatted:+15550100", conversation.details)
        assertEquals(false, conversation.isGroup)
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

        val conversation = result as ShareTargetUiState.Conversation
        assertEquals(true, conversation.isGroup)
        assertEquals(null, conversation.normalizedDestination)
        assertEquals(null, conversation.details)
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

        val conversation = result as ShareTargetUiState.Conversation
        assertEquals(null, conversation.normalizedDestination)
    }

    @Test
    fun map_setsNullDestinationFieldsWhenConversationHasNoDestination() {
        val result = mapper.map(
            persistentListOf(
                conversation(normalizedDestination = null, isGroup = false),
            ),
        ).single()

        val conversation = result as ShareTargetUiState.Conversation
        assertEquals(null, conversation.normalizedDestination)
        assertEquals(null, conversation.details)
    }

    @Test
    fun map_resolvesPrimaryUriWhenIconIsAvatarUri() {
        val avatarIcon = avatarUri(primaryUri = "content://primary")

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

        assertEquals(null, result.avatarUri)
    }

    @Test
    fun map_usesRawIconWhenIconIsNotAvatarUri() {
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

        assertEquals(null, result.avatarUri)
    }

    @Test
    fun map_setsNullAvatarWhenIconIsBlank() {
        val result = mapper.map(
            persistentListOf(conversation(icon = "   ")),
        ).single()

        assertEquals(null, result.avatarUri)
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

        assertEquals(null, result.details)
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
        assertEquals(emptyList<ShareTargetUiState>(), mapper.map(persistentListOf()))
    }

    private fun conversation(
        conversationId: String = "1",
        name: String = "Name",
        icon: String? = null,
        normalizedDestination: String? = null,
        isGroup: Boolean = false,
    ): ShareTargetConversation {
        return ShareTargetConversation(
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
