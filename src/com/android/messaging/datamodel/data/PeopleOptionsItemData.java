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
package com.android.messaging.datamodel.data;

import android.content.Context;
import android.database.Cursor;

import com.android.messaging.R;
import com.android.messaging.datamodel.data.ConversationListItemData.ConversationListViewColumns;
import com.android.messaging.util.Assert;
import com.android.messaging.util.NotificationChannelUtil;

public class PeopleOptionsItemData {
    public static final String[] PROJECTION = {
        ConversationListViewColumns._ID,
        ConversationListViewColumns.NAME,
        ConversationListViewColumns.NOTIFICATION_ENABLED,
        ConversationListViewColumns.NOTIFICATION_SOUND_URI,
        ConversationListViewColumns.NOTIFICATION_VIBRATION,
    };

    // Column index for query projection.
    public static final int INDEX_CONVERSATION_ID = 0;
    public static final int INDEX_CONVERSATION_NAME = 1;
    private static final int INDEX_NOTIFICATION_ENABLED = 2;
    private static final int INDEX_NOTIFICATION_SOUND_URI = 3;
    private static final int INDEX_NOTIFICATION_VIBRATION = 4;

    // Identification for each setting that's surfaced to the UI layer.
    public static final int SETTING_NOTIFICATIONS = 0;
    public static final int SETTING_BLOCKED = 1;
    public static final int SETTINGS_COUNT = 2;

    // Type of UI switch to show for the toggle button.
    public static final int TOGGLE_TYPE_CHECKBOX = 0;
    public static final int TOGGLE_TYPE_SWITCH = 1;

    private String mTitle;
    private String mSubtitle;
    private boolean mCheckable;
    private boolean mChecked;
    private boolean mEnabled;
    private int mItemId;
    private ParticipantData mOtherParticipant;
    private String mConversationTitle;
    private boolean mLegacyNotificationEnabled;
    private String mLegacyRingtoneString;
    private boolean mLegacyVibrationEnabled;

    private final Context mContext;

    public PeopleOptionsItemData(final Context context) {
        mContext = context;
    }

    /**
     * Bind to a specific setting column on conversation metadata cursor. (Note
     * that it binds to columns because it treats individual columns of the cursor as
     * separate options to display for the conversation, e.g. notification settings).
     */
    public void bind(
            final Cursor cursor, final ParticipantData otherParticipant, final int settingType) {
        mSubtitle = null;
        mCheckable = true;
        mEnabled = true;
        mItemId = settingType;
        mOtherParticipant = otherParticipant;
        mConversationTitle = cursor.getString(INDEX_CONVERSATION_NAME);
        mLegacyVibrationEnabled = cursor.getInt(INDEX_NOTIFICATION_ENABLED) == 1;
        mLegacyRingtoneString = cursor.getString(INDEX_NOTIFICATION_SOUND_URI);
        mLegacyVibrationEnabled = cursor.getInt(INDEX_NOTIFICATION_VIBRATION) == 1;

        switch (settingType) {
            case SETTING_NOTIFICATIONS:
                mTitle = mContext.getString(R.string.notifications_enabled_conversation_pref_title);
                mCheckable = false;
                break;

            case SETTING_BLOCKED:
                Assert.notNull(otherParticipant);
                final int resourceId = otherParticipant.isBlocked() ?
                        R.string.unblock_contact_title : R.string.block_contact_title;
                mTitle = mContext.getString(resourceId, otherParticipant.getDisplayDestination());
                mCheckable = false;
                break;

             default:
                 Assert.fail("Unsupported conversation option type!");
        }
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSubtitle() {
        return mSubtitle;
    }

    public boolean getCheckable() {
        return mCheckable;
    }

    public boolean getChecked() {
        return mChecked;
    }

    public boolean getEnabled() {
        return mEnabled;
    }

    public int getItemId() {
        return mItemId;
    }

    public String getConversationTitle() {
        return mConversationTitle;
    }

    public boolean getLegacyNotificationEnabled() {
        return mLegacyNotificationEnabled;
    }

    public String getLegacyRingtoneString() {
        return mLegacyRingtoneString;
    }

    public boolean getLegacyVibrationEnabled() {
        return mLegacyVibrationEnabled;
    }

    public ParticipantData getOtherParticipant() {
        return mOtherParticipant;
    }
}
