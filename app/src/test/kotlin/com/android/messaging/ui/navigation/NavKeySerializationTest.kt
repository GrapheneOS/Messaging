package com.android.messaging.ui.navigation

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.ui.conversation.navigation.AddParticipantsNavKey
import com.android.messaging.ui.conversation.navigation.ConversationNavKey
import com.android.messaging.ui.conversation.navigation.MessageDetailsNavKey
import com.android.messaging.ui.conversation.navigation.NewChatNavKey
import com.android.messaging.ui.conversationlist.navigation.ConversationListNavKey
import com.android.messaging.ui.conversationpicker.navigation.ForwardMessageNavKey
import com.android.messaging.ui.onboarding.navigation.OnboardingNavKey
import com.android.messaging.ui.vcarddetail.navigation.VCardDetailNavKey
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavKeySerializationTest {

    private val serializer = NavKeySerializer<NavKey>()

    @Test
    fun conversationListNavKey_roundTripsThroughSavedState() {
        assertRoundTrips(ConversationListNavKey)
    }

    @Test
    fun onboardingNavKey_roundTripsThroughSavedState() {
        assertRoundTrips(OnboardingNavKey)
    }

    @Test
    fun newChatNavKey_roundTripsThroughSavedState() {
        assertRoundTrips(NewChatNavKey)
    }

    @Test
    fun conversationNavKey_roundTripsWithTypedConversationId() {
        assertRoundTrips(ConversationNavKey(conversationId = ConversationId("c")))
    }

    @Test
    fun addParticipantsNavKey_roundTripsWithTypedConversationId() {
        assertRoundTrips(AddParticipantsNavKey(conversationId = ConversationId("c")))
    }

    @Test
    fun messageDetailsNavKey_roundTripsWithTypedIds() {
        assertRoundTrips(
            MessageDetailsNavKey(
                conversationId = ConversationId("c"),
                messageId = MessageId("m"),
            ),
        )
    }

    @Test
    fun vCardDetailNavKey_roundTripsWithUri() {
        assertRoundTrips(VCardDetailNavKey(uri = "content://scratch/contact.vcf"))
    }

    @Test
    fun forwardMessageNavKey_roundTripsWithTypedIds() {
        assertRoundTrips(
            ForwardMessageNavKey(
                conversationId = ConversationId("c"),
                messageId = MessageId("m"),
            ),
        )
    }

    private fun assertRoundTrips(navKey: NavKey) {
        val encoded = encodeToSavedState(
            serializer = serializer,
            value = navKey,
        )
        val restored = decodeFromSavedState(
            deserializer = serializer,
            savedState = encoded,
        )

        assertEquals(navKey, restored)
    }
}
