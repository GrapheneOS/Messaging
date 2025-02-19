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

package com.android.messaging.datamodel.action;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.android.messaging.Factory;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.BugleNotifications;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DataModelException;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.ui.conversationlist.MultiSelectActionModeCallback.SelectedConversation;
import com.android.messaging.util.Assert;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.NotificationChannelUtil;
import com.android.messaging.widget.WidgetConversationProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Action used to delete a conversation.
 */
public class DeleteConversationAction extends Action implements Parcelable {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;

    static class TargetConversation implements Parcelable {
        final String mId;
        final long mCutoffTimestamp;

        TargetConversation(String conversationId, long cutoffTimestamp) {
            mId = conversationId;
            mCutoffTimestamp = cutoffTimestamp;
        }

        TargetConversation(Parcel in) {
            mId = in.readString();
            mCutoffTimestamp = in.readLong();
        }

        static TargetConversation fromSelectedConversation(SelectedConversation conversation) {
            return new TargetConversation(conversation.conversationId, conversation.timestamp);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(mId);
            dest.writeLong(mCutoffTimestamp);
        }

        public static final Parcelable.Creator<TargetConversation> CREATOR
                = new Parcelable.Creator<>() {
            @Override
            public TargetConversation createFromParcel(final Parcel in) {
                return new TargetConversation(in);
            }

            @Override
            public TargetConversation[] newArray(final int size) {
                return new TargetConversation[size];
            }
        };
    }

    public static void deleteConversation(final String conversationId, final long cutoffTimestamp) {
        final DeleteConversationAction action = new DeleteConversationAction(
                new TargetConversation[]{new TargetConversation(conversationId, cutoffTimestamp)});
        action.start();
    }

    public static void deleteConversations(final Collection<SelectedConversation> selectedConversations) {
        TargetConversation[] conversations = selectedConversations.stream()
                .map(TargetConversation::fromSelectedConversation)
                .toArray(TargetConversation[]::new);
        final DeleteConversationAction action = new DeleteConversationAction(conversations);
        action.start();
    }

    private static final String KEY_CONVERSATIONS = "conversations";

    private DeleteConversationAction(final TargetConversation[] conversations) {
        super();
        actionParameters.putParcelableArray(KEY_CONVERSATIONS, conversations);
    }

    @Override
    protected Bundle doBackgroundWork() throws DataModelException {
        final DatabaseWrapper db = DataModel.get().getDatabase();

        final TargetConversation[] conversations =
                actionParameters.getParcelableArray(KEY_CONVERSATIONS, TargetConversation.class);

        if (conversations == null || conversations.length == 0) {
            return null;
        }

        int successCount = 0;
        int failCount = 0;
        for (TargetConversation conversation : conversations) {
            if (deleteConversationInternal(db, conversation)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        if (failCount > 0) {
            BugleActionToasts.onFailedToDeleteConversations(failCount);
        }
        if (successCount > 0) {
            BugleActionToasts.onConversationsDeleted(successCount);
        }

        // Remove notifications if necessary
        BugleNotifications.update(null /* conversationId */,
                BugleNotifications.UPDATE_MESSAGES);

        return null;
    }

    // Delete conversation from both the local DB and telephony in the background so sync cannot
    // run concurrently and incorrectly try to recreate the conversation's messages locally. The
    // telephony database can sometimes be quite slow to delete conversations, so we delete from
    // the local DB first, notify the UI, and then delete from telephony.
    private boolean deleteConversationInternal(DatabaseWrapper db, final TargetConversation conversation) {
        String conversationId = conversation.mId;
        long cutoffTimestamp = conversation.mCutoffTimestamp;

        NotificationChannelUtil.INSTANCE.deleteChannel(conversationId);

        if (!TextUtils.isEmpty(conversationId)) {
            // First find the thread id for this conversation.
            final long threadId = BugleDatabaseOperations.getThreadId(db, conversationId);

            if (BugleDatabaseOperations.deleteConversation(db, conversationId, cutoffTimestamp)) {
                LogUtil.i(TAG, "DeleteConversationAction: Deleted local conversation "
                        + conversationId);

                // We have changed the conversation list
                MessagingContentProvider.notifyConversationListChanged();

                // Notify the widget the conversation is deleted so it can go into its configure state.
                WidgetConversationProvider.notifyConversationDeleted(
                        Factory.get().getApplicationContext(),
                        conversationId);
            } else {
                LogUtil.w(TAG, "DeleteConversationAction: Could not delete local conversation "
                        + conversationId);
                return false;
            }

            // Now delete from telephony DB. MmsSmsProvider throws an exception if the thread id is
            // less than 0. If it's greater than zero, it will delete all messages with that thread
            // id, even if there's no corresponding row in the threads table.
            if (threadId >= 0) {
                final int count = MmsUtils.deleteThread(threadId, cutoffTimestamp);
                if (count > 0) {
                    LogUtil.i(TAG, "DeleteConversationAction: Deleted telephony thread "
                            + threadId + " (cutoffTimestamp = " + cutoffTimestamp + ")");
                } else {
                    LogUtil.w(TAG, "DeleteConversationAction: Could not delete thread from "
                            + "telephony: conversationId = " + conversationId + ", thread id = "
                            + threadId);
                    return false;
                }
            } else {
                LogUtil.w(TAG, "DeleteConversationAction: Local conversation " + conversationId
                        + " has an invalid telephony thread id; will delete messages individually");
                return deleteConversationMessagesFromTelephony(conversationId);
            }

            return true;
        } else {
            LogUtil.e(TAG, "DeleteConversationAction: conversationId is empty");
            return false;
        }
    }

    /**
     * Deletes all the telephony messages for the local conversation being deleted.
     * <p>
     * This is a fallback used when the conversation is not associated with any telephony thread,
     * or its thread id is invalid (e.g. negative). This is not common, but can happen sometimes
     * (e.g. the Unknown Sender conversation). In the usual case of deleting a conversation, we
     * don't need this because the telephony provider automatically deletes messages when a thread
     * is deleted.
     */
    private boolean deleteConversationMessagesFromTelephony(String conversationId) {
        final DatabaseWrapper db = DataModel.get().getDatabase();
        Assert.notNull(conversationId);

        final List<Uri> messageUris = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.MESSAGES_TABLE,
                    new String[] { MessageColumns.SMS_MESSAGE_URI },
                    MessageColumns.CONVERSATION_ID + "=?",
                    new String[] { conversationId },
                    null, null, null);
            while (cursor.moveToNext()) {
                String messageUri = cursor.getString(0);
                try {
                    messageUris.add(Uri.parse(messageUri));
                } catch (Exception e) {
                    LogUtil.e(TAG, "DeleteConversationAction: Could not parse message uri "
                            + messageUri);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        int deletedMessages = 0;
        for (Uri messageUri : messageUris) {
            int count = MmsUtils.deleteMessage(messageUri);
            if (count > 0) {
                if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
                    LogUtil.d(TAG, "DeleteConversationAction: Deleted telephony message "
                            + messageUri);
                }
                deletedMessages++;
            } else {
                LogUtil.w(TAG, "DeleteConversationAction: Could not delete telephony message "
                        + messageUri);
            }
        }
        return messageUris.size() == deletedMessages;
    }

    @Override
    protected Object executeAction() {
        requestBackgroundWork();
        return null;
    }

    private DeleteConversationAction(final Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<DeleteConversationAction> CREATOR
            = new Parcelable.Creator<DeleteConversationAction>() {
        @Override
        public DeleteConversationAction createFromParcel(final Parcel in) {
            return new DeleteConversationAction(in);
        }

        @Override
        public DeleteConversationAction[] newArray(final int size) {
            return new DeleteConversationAction[size];
        }
    };

    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}
