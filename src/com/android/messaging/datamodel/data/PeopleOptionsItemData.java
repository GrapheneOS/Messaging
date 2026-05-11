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

import com.android.messaging.datamodel.data.ConversationListItemData.ConversationListViewColumns;

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
    public static final int INDEX_NOTIFICATION_ENABLED = 2;
    public static final int INDEX_NOTIFICATION_SOUND_URI = 3;
    public static final int INDEX_NOTIFICATION_VIBRATION = 4;
}
