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

package com.android.messaging.datamodel;

import android.provider.BaseColumns;
import android.provider.OpenableColumns;

import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.DatabaseHelper.PartColumns;
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns;
import com.android.messaging.util.ContentType;

/**
 * View for the image parts for the conversation. It is used to provide the photoviewer with a
 * a data source for all the photos in a conversation, so that the photoviewer can support paging
 * through all the photos of the conversation.
 */
public class ConversationImagePartsView {
    private static final String VIEW_NAME = "conversation_image_parts_view";

    private static final String CREATE_SQL = "CREATE VIEW " +
            VIEW_NAME + " AS SELECT "
            + DatabaseHelper.MESSAGES_TABLE + '.' + MessageColumns.CONVERSATION_ID
            + " as " + Columns.CONVERSATION_ID + ", "
            + DatabaseHelper.PARTS_TABLE + '.' + PartColumns.CONTENT_URI
            + " as " + Columns.URI + ", "
            + DatabaseHelper.PARTICIPANTS_TABLE + '.' + ParticipantColumns.FULL_NAME
            + " as " + Columns.SENDER_FULL_NAME + ", "
            + DatabaseHelper.PARTS_TABLE + '.' + PartColumns.CONTENT_URI
            + " as " + Columns.CONTENT_URI + ", "
            // Use NULL as the thumbnail uri
            + " NULL as " + Columns.THUMBNAIL_URI + ", "
            + DatabaseHelper.PARTS_TABLE + '.' + PartColumns.CONTENT_TYPE
            + " as " + Columns.CONTENT_TYPE + ", "
            //
            // Columns in addition to the base photo viewer columns
            //
            + DatabaseHelper.PARTICIPANTS_TABLE + '.' + ParticipantColumns.DISPLAY_DESTINATION
            + " as " + Columns.DISPLAY_DESTINATION + ", "
            + DatabaseHelper.MESSAGES_TABLE + '.' + MessageColumns.RECEIVED_TIMESTAMP
            + " as " + Columns.RECEIVED_TIMESTAMP + ", "
            + DatabaseHelper.MESSAGES_TABLE + '.' + MessageColumns.STATUS
            + " as " + Columns.STATUS + " "

            + " FROM " + DatabaseHelper.MESSAGES_TABLE + " LEFT JOIN " + DatabaseHelper.PARTS_TABLE
            + " ON (" + DatabaseHelper.MESSAGES_TABLE + "." + MessageColumns._ID
            + "=" + DatabaseHelper.PARTS_TABLE + "." + PartColumns.MESSAGE_ID + ") "
            + " LEFT JOIN " + DatabaseHelper.PARTICIPANTS_TABLE + " ON ("
            + DatabaseHelper.MESSAGES_TABLE + '.' +  MessageColumns.SENDER_PARTICIPANT_ID
            + '=' + DatabaseHelper.PARTICIPANTS_TABLE + '.' + ParticipantColumns._ID + ")"

            // "content_type like 'image/%'"
            + " WHERE " + DatabaseHelper.PARTS_TABLE + "." + PartColumns.CONTENT_TYPE
            + " like '" + ContentType.IMAGE_PREFIX + "%'"

            + " ORDER BY "
            + DatabaseHelper.MESSAGES_TABLE + '.' + MessageColumns.RECEIVED_TIMESTAMP + " ASC, "
            + DatabaseHelper.PARTS_TABLE + '.' + PartColumns._ID + " ASC";

    static class Columns implements BaseColumns {
        static final String CONVERSATION_ID = MessageColumns.CONVERSATION_ID;
        static final String URI = "uri";
        static final String SENDER_FULL_NAME = OpenableColumns.DISPLAY_NAME;
        static final String CONTENT_URI = "contentUri";
        static final String THUMBNAIL_URI = "thumbnailUri";
        static final String CONTENT_TYPE = "contentType";
        // Columns in addition to the base photo viewer columns
        static final String DISPLAY_DESTINATION = ParticipantColumns.DISPLAY_DESTINATION;
        static final String RECEIVED_TIMESTAMP = MessageColumns.RECEIVED_TIMESTAMP;
        static final String STATUS = MessageColumns.STATUS;
    }

    public interface PhotoViewQuery {
        public final String[] PROJECTION = {
            Columns.URI,
            Columns.SENDER_FULL_NAME,
            Columns.CONTENT_URI,
            Columns.THUMBNAIL_URI,
            Columns.CONTENT_TYPE,
            // Columns in addition to the base photo viewer columns
            Columns.DISPLAY_DESTINATION,
            Columns.RECEIVED_TIMESTAMP,
            Columns.STATUS,
        };

        public final int INDEX_URI = 0;
        public final int INDEX_SENDER_FULL_NAME = 1;
        public final int INDEX_CONTENT_URI = 2;
        public final int INDEX_THUMBNAIL_URI = 3;
        public final int INDEX_CONTENT_TYPE = 4;
        // Columns in addition to the base photo viewer columns
        public final int INDEX_DISPLAY_DESTINATION = 5;
        public final int INDEX_RECEIVED_TIMESTAMP = 6;
        public final int INDEX_STATUS = 7;
    }

    static final String getViewName() {
        return VIEW_NAME;
    }

    static final String getCreateSql() {
        return CREATE_SQL;
    }
}
