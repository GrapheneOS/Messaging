package com.android.messaging.ui.navigation

import android.net.Uri
import androidx.core.net.toUri
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversation.model.MessageId
import com.android.messaging.data.subscription.model.SubId
import com.android.messaging.testutil.TEST_CONVERSATION_ID as CONVERSATION_ID
import com.android.messaging.testutil.TEST_CONVERSATION_ID_VALUE as CONVERSATION_ID_VALUE
import com.android.messaging.testutil.assertThat
import com.android.messaging.ui.appsettings.navigation.SubscriptionSettingsNavKey
import com.android.messaging.ui.appsettings.navigation.subscriptionSettingsDefaultArgs
import com.android.messaging.ui.appsettings.subscription.SUBSCRIPTION_SETTINGS_SUB_ID_ARG
import com.android.messaging.ui.conversation.messagedetails.MESSAGE_DETAILS_CONVERSATION_ID_ARG
import com.android.messaging.ui.conversation.messagedetails.MESSAGE_DETAILS_MESSAGE_ID_ARG
import com.android.messaging.ui.conversation.navigation.MessageDetailsNavKey
import com.android.messaging.ui.conversation.navigation.messageDetailsDefaultArgs
import com.android.messaging.ui.conversationpicker.host.forward.FORWARD_CONVERSATION_ID_ARG
import com.android.messaging.ui.conversationpicker.host.forward.FORWARD_MESSAGE_ID_ARG
import com.android.messaging.ui.conversationpicker.navigation.ForwardMessageNavKey
import com.android.messaging.ui.conversationpicker.navigation.forwardMessageDefaultArgs
import com.android.messaging.ui.conversationsettings.navigation.ConversationSettingsNavKey
import com.android.messaging.ui.conversationsettings.navigation.conversationSettingsDefaultArgs
import com.android.messaging.ui.conversationsettings.screen.CONVERSATION_SETTINGS_CONVERSATION_ID_ARG
import com.android.messaging.ui.vcarddetail.navigation.VCardDetailNavKey
import com.android.messaging.ui.vcarddetail.navigation.vCardDetailDefaultArgs
import com.android.messaging.ui.vcarddetail.screen.VCARD_DETAIL_URI_ARG
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavDefaultArgsTest {

    @Test
    fun messageDetailsDefaultArgs_seedsBothIdsUnderTheKeysTheViewModelReads() {
        val defaultArgs = messageDetailsDefaultArgs(
            navKey = MessageDetailsNavKey(
                conversationId = CONVERSATION_ID,
                messageId = MessageId(MESSAGE_ID_VALUE),
            ),
        )

        assertThat(
            ConversationId.fromOrNull(defaultArgs.getString(MESSAGE_DETAILS_CONVERSATION_ID_ARG)),
        ).isEqualTo(CONVERSATION_ID)

        assertThat(
            MessageId.fromOrNull(defaultArgs.getString(MESSAGE_DETAILS_MESSAGE_ID_ARG))
        ).isEqualTo(MessageId(MESSAGE_ID_VALUE))
    }

    @Test
    fun forwardMessageDefaultArgs_seedsBothIdsUnderTheKeysTheViewModelReads() {
        val defaultArgs = forwardMessageDefaultArgs(
            navKey = ForwardMessageNavKey(
                conversationId = CONVERSATION_ID,
                messageId = MessageId(MESSAGE_ID_VALUE),
            ),
        )

        assertThat(
            ConversationId.fromOrNull(defaultArgs.getString(FORWARD_CONVERSATION_ID_ARG))
        ).isEqualTo(CONVERSATION_ID)

        assertThat(
            MessageId.fromOrNull(defaultArgs.getString(FORWARD_MESSAGE_ID_ARG))
        ).isEqualTo(MessageId(MESSAGE_ID_VALUE))
    }

    @Test
    fun conversationSettingsDefaultArgs_seedsConversationIdUnderTheKeyTheViewModelReads() {
        val defaultArgs = conversationSettingsDefaultArgs(
            navKey = ConversationSettingsNavKey(conversationId = CONVERSATION_ID),
        )

        assertThat(
            ConversationId.fromOrNull(
                defaultArgs.getString(CONVERSATION_SETTINGS_CONVERSATION_ID_ARG),
            ),
        ).isEqualTo(CONVERSATION_ID)
    }

    @Test
    fun conversationScopedDefaultArgs_seedTheUnboxedStringNotTheValueClass() {
        val defaultArgs = conversationSettingsDefaultArgs(
            navKey = ConversationSettingsNavKey(conversationId = CONVERSATION_ID),
        )

        assertThat(
            defaultArgs.getString(CONVERSATION_SETTINGS_CONVERSATION_ID_ARG)
        ).isEqualTo(CONVERSATION_ID_VALUE)
    }

    @Test
    fun subscriptionSettingsDefaultArgs_seedsSubIdAsIntUnderTheKeyTheViewModelReads() {
        val defaultArgs = subscriptionSettingsDefaultArgs(
            navKey = SubscriptionSettingsNavKey(
                subId = SubId(SUB_ID_VALUE),
                title = "SIM 1",
            ),
        )

        assertThat(defaultArgs.getInt(SUBSCRIPTION_SETTINGS_SUB_ID_ARG)).isEqualTo(SUB_ID_VALUE)

        assertThat(
            SubId(defaultArgs.getInt(SUBSCRIPTION_SETTINGS_SUB_ID_ARG))
        ).isEqualTo(SubId(SUB_ID_VALUE))
    }

    @Test
    fun subscriptionSettingsDefaultArgs_doesNotSeedSubIdAsString() {
        val defaultArgs = subscriptionSettingsDefaultArgs(
            navKey = SubscriptionSettingsNavKey(
                subId = SubId(SUB_ID_VALUE),
                title = "SIM 1",
            ),
        )

        assertNull(defaultArgs.getString(SUBSCRIPTION_SETTINGS_SUB_ID_ARG))
    }

    @Test
    fun vCardDetailDefaultArgs_seedsUriAsParcelableUnderTheKeyTheViewModelReads() {
        val defaultArgs = vCardDetailDefaultArgs(
            navKey = VCardDetailNavKey(uri = VCARD_URI_VALUE),
        )

        assertThat(
            defaultArgs.getParcelable(VCARD_DETAIL_URI_ARG, Uri::class.java)
        ).isEqualTo(VCARD_URI_VALUE.toUri())
    }

    @Test
    fun vCardDetailDefaultArgs_doesNotSeedUriAsString() {
        val defaultArgs = vCardDetailDefaultArgs(
            navKey = VCardDetailNavKey(uri = VCARD_URI_VALUE),
        )

        assertNull(defaultArgs.getString(VCARD_DETAIL_URI_ARG))
    }

    private companion object {
        const val MESSAGE_ID_VALUE = "message-1"
        const val SUB_ID_VALUE = 7
        const val VCARD_URI_VALUE = "content://scratch/contact.vcf"
    }
}
