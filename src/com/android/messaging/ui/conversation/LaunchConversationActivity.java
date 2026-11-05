/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.ui.conversation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.binding.Binding;
import com.android.messaging.datamodel.binding.BindingBase;
import com.android.messaging.datamodel.data.LaunchConversationData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.UiUtils;
import com.android.messaging.util.UriUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Launches ConversationActivity for sending a message to, or viewing messages from, a specific
 * recipient.
 * <p>
 * (This activity should be marked noHistory="true" in AndroidManifest.xml)
 */
public class LaunchConversationActivity extends Activity implements
        LaunchConversationData.LaunchConversationDataListener {
    private static final int MAX_RECIPIENT_LENGTH = 100;
    static final String SMS_BODY = "sms_body";
    static final String ADDRESS = "address";
    /**
     * SIM slot to send from, set by telephony when it opens a conversation with emergency services
     * over satellite. The platform defines it as {@code TelephonyManager.EXTRA_SIM_SLOT_ID}, which
     * is hidden from the SDK.
     */
    static final String SIM_SLOT = "simSlot";
    final Binding<LaunchConversationData> mBinding = BindingBase.createBinding(this);
    String mSmsBody;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (UiUtils.redirectToOnboardingIfNeeded(this)) {
            return;
        }

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (Intent.ACTION_SENDTO.equals(action) || Intent.ACTION_VIEW.equals(action)) {
            String[] recipients = null;
            final String commaSeparatedRecipients =
                    UriUtil.parseRecipientsFromSmsMmsUri(intent.getData());
            if (commaSeparatedRecipients != null) {
                recipients = commaSeparatedRecipients.split(",");
            }
            final boolean haveAddress = !TextUtils.isEmpty(intent.getStringExtra(ADDRESS));
            final boolean haveEmail = !TextUtils.isEmpty(intent.getStringExtra(Intent.EXTRA_EMAIL));
            if (recipients == null && (haveAddress || haveEmail)) {
                if (haveAddress) {
                    recipients = new String[] { intent.getStringExtra(ADDRESS) };
                } else {
                    recipients = new String[] { intent.getStringExtra(Intent.EXTRA_EMAIL) };
                }
            }
            if (recipients != null) {
                recipients = trimInvalidRecipients(recipients);
            }
            mSmsBody = intent.getStringExtra(SMS_BODY);
            if (TextUtils.isEmpty(mSmsBody)) {
                // Used by intents sent from the web YouTube (and perhaps others).
                mSmsBody = getBody(intent.getData());
                if (TextUtils.isEmpty(mSmsBody)) {
                    // If that fails, try yet another method apps use to share text
                    if (ContentType.TEXT_PLAIN.equals(intent.getType())) {
                        mSmsBody = intent.getStringExtra(Intent.EXTRA_TEXT);
                    }
                }
            }
            if (recipients != null) {
                mBinding.bind(DataModel.get().createLaunchConversationData(this));
                mBinding.getData().getOrCreateConversation(mBinding, recipients,
                        getSelfSubId(getSystemService(SubscriptionManager.class),
                                getSystemService(TelephonyManager.class), recipients,
                                intent.getIntExtra(SIM_SLOT,
                                        SubscriptionManager.INVALID_SIM_SLOT_INDEX)));
            } else {
                // No recipients were specified in the intent.
                // Start a new conversation with contact picker. The new conversation will be
                // primed with the (optional) message in mSmsBody.
                onGetOrCreateNewConversation(null);
            }
        } else {
            LogUtil.w(LogUtil.BUGLE_TAG, "Unsupported conversation intent action : " + action);
        }
        // As of M, activities without a visible window must finish before onResume completes.
        finish();
    }

    /**
     * Returns the subscription in {@code slotIndex} to send from, or the default. Any app can set
     * the slot, so it is only honored for a conversation with emergency services, which is the
     * only case telephony sets it.
     */
    static int getSelfSubId(final SubscriptionManager subscriptionManager,
            final TelephonyManager telephonyManager, final String[] recipients,
            final int slotIndex) {
        if (slotIndex < 0 || subscriptionManager == null || telephonyManager == null) {
            return ParticipantData.DEFAULT_SELF_SUB_ID;
        }
        try {
            for (final String recipient : recipients) {
                if (!telephonyManager.isEmergencyNumber(recipient)) {
                    return ParticipantData.DEFAULT_SELF_SUB_ID;
                }
            }
            final SubscriptionInfo info =
                    subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slotIndex);
            return info != null ? info.getSubscriptionId() : ParticipantData.DEFAULT_SELF_SUB_ID;
        } catch (final SecurityException | IllegalStateException
                | UnsupportedOperationException e) {
            LogUtil.w(LogUtil.BUGLE_TAG, "Cannot resolve the subscription in SIM slot "
                    + slotIndex, e);
            return ParticipantData.DEFAULT_SELF_SUB_ID;
        }
    }

    private String[] trimInvalidRecipients(String[] recipients) {
        List<String> trimmedRecipients = new ArrayList<>();
        for (String recipient : recipients) {
            if (recipient.length() < MAX_RECIPIENT_LENGTH) {
                trimmedRecipients.add(recipient);
            }
        }
        if (trimmedRecipients.size() > 0) {
            return trimmedRecipients.toArray(new String[0]);
        } else {
            return null;
        }
    }

    private String getBody(final Uri uri) {
        if (uri == null) {
            return null;
        }
        String urlStr = uri.getSchemeSpecificPart();
        if (!urlStr.contains("?")) {
            return null;
        }
        urlStr = urlStr.substring(urlStr.indexOf('?') + 1);
        final String[] params = urlStr.split("&");
        for (final String p : params) {
            if (p.startsWith("body=")) {
                try {
                    return URLDecoder.decode(p.substring(5), "UTF-8");
                } catch (final UnsupportedEncodingException e) {
                    // Invalid URL, ignore
                }
            }
        }
        return null;
    }

    @Override
    public void onGetOrCreateNewConversation(final String conversationId) {
        final Context context = Factory.get().getApplicationContext();
        UIIntents.get().launchConversationActivityWithParentStack(context, conversationId,
                mSmsBody);
    }

    @Override
    public void onGetOrCreateNewConversationFailed() {
        UiUtils.showToastAtBottom(R.string.conversation_creation_failure);
    }
}
