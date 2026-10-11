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

/**
 * SQL and column names backing the message text search exposed by
 * {@link MessagingContentProvider#SEARCH_URI}.
 */
public final class MessageSearchQuery {
    // Projection column names returned by SQL.
    public static final String MESSAGE_ID = "message_id";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String SNIPPET = "snippet";
    public static final String SORT_TIMESTAMP = "sort_timestamp";
    public static final String CONVERSATION_NAME = "conversation_name";
    public static final String SENDER_FULL_NAME = "sender_full_name";
    public static final String SENDER_FIRST_NAME = "sender_first_name";
    public static final String SENDER_DESTINATION = "sender_destination";

    /**
     * Finds messages whose plain-text part contains the supplied (escaped) LIKE pattern. The
     * pattern is bound as the single query argument and matched with {@code ESCAPE '\'}.
     */
    public static final String SQL =
            "SELECT "
            + "p.message_id AS " + MESSAGE_ID + ", "
            + "p.conversation_id AS " + CONVERSATION_ID + ", "
            + "p.text AS " + SNIPPET + ", "
            + "MAX(p.timestamp) AS " + SORT_TIMESTAMP + ", "
            + "MAX(m.sent_timestamp) AS sent_timestamp, "
            + "MAX(m.received_timestamp) AS received_timestamp, "
            + "MAX(m.mms_subject) AS mms_subject, "
            + "MAX(c.name) AS " + CONVERSATION_NAME + ", "
            + "MAX(pr.full_name) AS " + SENDER_FULL_NAME + ", "
            + "MAX(pr.first_name) AS " + SENDER_FIRST_NAME + ", "
            + "MAX(pr.display_destination) AS " + SENDER_DESTINATION + " "
            + "FROM " + DatabaseHelper.PARTS_TABLE + " AS p "
            + "LEFT JOIN " + DatabaseHelper.MESSAGES_TABLE + " AS m "
            + "ON p." + DatabaseHelper.PartColumns.MESSAGE_ID + " = m." + DatabaseHelper.MessageColumns._ID + " "
            + "LEFT JOIN " + DatabaseHelper.CONVERSATIONS_TABLE + " AS c "
            + "ON p." + DatabaseHelper.PartColumns.CONVERSATION_ID + " = c." + DatabaseHelper.ConversationColumns._ID + " "
            + "LEFT JOIN " + DatabaseHelper.PARTICIPANTS_TABLE + " AS pr "
            + "ON m." + DatabaseHelper.MessageColumns.SENDER_PARTICIPANT_ID + " = pr." + DatabaseHelper.ParticipantColumns._ID + " "
            + "WHERE p." + DatabaseHelper.PartColumns.CONTENT_TYPE + " = 'text/plain' "
            + "AND p." + DatabaseHelper.PartColumns.TEXT + " LIKE ? ESCAPE '\\' "
            + "GROUP BY p." + DatabaseHelper.PartColumns.MESSAGE_ID + " "
            + "ORDER BY " + SORT_TIMESTAMP + " DESC "
            + "LIMIT 100";

    private MessageSearchQuery() {
    }
}
