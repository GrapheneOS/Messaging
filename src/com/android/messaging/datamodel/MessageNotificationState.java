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

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.MessagingStyle;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.datamodel.data.ConversationMessageData;
import com.android.messaging.datamodel.data.ConversationParticipantsData;
import com.android.messaging.datamodel.data.MessageData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.ParticipantData;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.Assert;
import com.android.messaging.util.AvatarUriUtil;
import com.android.messaging.util.ContentType;
import com.android.messaging.util.ConversationIdSet;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.PendingIntentConstants;
import com.android.messaging.util.UriUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Notification building class for conversation messages.
 * <p>
 * A three level structure is used to coalesce the data from the database. From bottom to top:
 * <p>
 * 1) {@link MessageLineInfo} - A single message that needs to be notified.
 * <p>
 * 2) {@link Conversation} - A list of {@link MessageLineInfo} in a single conversation.
 * <p>
 * 3) {@link ConversationsList} - A list of {@link Conversation} and the total number of messages.
 * <p>
 * The {@link MessageNotificationState#createConversationsList()} function performs the query and
 * creates the data structure.
 */
public class MessageNotificationState {
    // Logging
    static final String TAG = LogUtil.BUGLE_NOTIFICATIONS_TAG;

    private static final int REPLY_INTENT_REQUEST_CODE_OFFSET = 0;
    private static final int NUM_EXTRA_REQUEST_CODES_NEEDED = 1;

    private static final int CONTENT_INTENT_REQUEST_CODE_OFFSET = 0;
    private static final int CLEAR_INTENT_REQUEST_CODE_OFFSET = 1;
    private static final int NUM_REQUEST_CODES_NEEDED = 2;

    public boolean mCanceled;
    public int mType;
    public int mBaseRequestCode;

    public interface FailedMessageQuery {
        static final String FAILED_MESSAGES_WHERE_CLAUSE =
                "((" + DatabaseHelper.MessageColumns.STATUS + " = " +
                        MessageData.BUGLE_STATUS_OUTGOING_FAILED + " OR " +
                        DatabaseHelper.MessageColumns.STATUS + " = " +
                        MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED + ") AND " +
                        DatabaseHelper.MessageColumns.SEEN + " = 0)";

        static final String FAILED_ORDER_BY = DatabaseHelper.MessageColumns.CONVERSATION_ID + ", " +
                DatabaseHelper.MessageColumns.SENT_TIMESTAMP + " asc";
    }

    /**
     * Information on a single chat message which should be shown in a notification.
     */
    public static class MessageLineInfo {
        boolean mIsManualDownloadNeeded;
        final String mAuthorId;
        final String mMessageId;
        final String mName;
        final CharSequence mText;
        final long mTimestamp;
        final Uri mAvatarUri;
        final Uri mAttachmentUri;
        final String mAttachmentType;
        final String mContactUriString;

        MessageLineInfo(final String authorId, final String authorFullName,
                final String authorFirstName, final CharSequence text, final Uri attachmentUri,
                final String attachmentType, final boolean isManualDownloadNeeded,
                final Uri avatarUri, final String messageId, final long timestamp,
                final String contactUriString) {
            mAuthorId = authorId;
            mMessageId = messageId;
            mName = authorFullName == null ? authorFirstName : authorFullName;
            mTimestamp = timestamp;

            CharSequence messageText;
            boolean textEmpty = TextUtils.isEmpty(text);
            if (attachmentUri != null && textEmpty) {
                messageText = formatAttachmentTag(attachmentType);
            } else if (!textEmpty) {
                messageText = text;
            } else {
                Context context = Factory.get().getApplicationContext();
                messageText = context.getString(R.string.notification_unsupported_file);
            }
            mText = messageText;

            mIsManualDownloadNeeded = isManualDownloadNeeded;
            mAvatarUri = avatarUri;
            mAttachmentUri = attachmentUri;
            mAttachmentType = attachmentType;
            mContactUriString = contactUriString;
        }

        static CharSequence formatAttachmentTag(final String attachmentType) {
            final Context context = Factory.get().getApplicationContext();
            // The default attachment type is an image, since that's what was originally
            // supported. When there's no content type, assume it's an image.
            int messageId;
            if (ContentType.isAudioType(attachmentType)) {
                messageId = R.string.notification_audio;
            } else if (ContentType.isVideoType(attachmentType)) {
                messageId = R.string.notification_video;
            } else if (ContentType.isVCardType(attachmentType)) {
                messageId = R.string.notification_vcard;
            } else if (ContentType.isImageType(attachmentType)) {
                messageId = R.string.notification_picture;
            } else {
                messageId = R.string.notification_file;
            }
            SpannableString spannableString = new SpannableString(context.getString(messageId));
            spannableString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spannableString.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spannableString;
        }

        public Person createPerson() {
            Person.Builder builder = new Person.Builder()
                    .setKey(mAuthorId)
                    .setName(mName)
                    .setUri(mContactUriString);

            Context context = Factory.get().getApplicationContext();
            Bitmap avatarBitmap = BugleNotifications.getAvatarBitmap(context, mAvatarUri);
            if (avatarBitmap != null) {
                builder.setIcon(IconCompat.createWithBitmap(avatarBitmap));
            }

            return builder.build();
        }

        public MessagingStyle.Message createStyledMessage(Person person) {
            MessagingStyle.Message message =
                    new MessagingStyle.Message(mText, mTimestamp, person);
            if (mAttachmentUri != null && ContentType.isImageType(mAttachmentType)) {
                message.setData(mAttachmentType,
                        SharedMemoryImageProvider.Companion.buildUri(mAttachmentUri, mAttachmentType));
            }
            return message;
        }
    }

    /**
     * Information on all the notification messages within a single conversation.
     */
    public static class Conversation {
        // Conversation id of the latest message in the notification for this merged conversation.
        final String mConversationId;

        // True if this represents a group conversation.
        final boolean mIsGroup;

        // Name of the group conversation if available.
        final String mGroupConversationName;

        // True if this conversation's recipients includes one or more email address(es)
        // (see ConversationColumns.INCLUDE_EMAIL_ADDRESS)
        final boolean mIncludeEmailAddress;

        // Timestamp of the latest message
        final long mReceivedTimestamp;

        // Self participant id.
        final String mSelfParticipantId;

        // List of individual line notifications to be parsed later.
        final List<MessageLineInfo> mLineInfos;

        // Total number of messages. Might be different that mLineInfos.size() as the number of
        // line infos is capped.
        int mTotalMessageCount;

        // Custom ringtone if set
        final String mRingtoneUri;

        // Should notification be enabled for this conversation?
        final boolean mNotificationEnabled;

        // Should notifications vibrate for this conversation?
        final boolean mNotificationVibrate;

        // Subscription id.
        final int mSubId;

        // Number of participants
        final int mParticipantCount;

        final String mIconUri;

        protected Conversation(final String conversationId,
                final boolean isGroup,
                final String groupConversationName,
                final boolean includeEmailAddress,
                final long receivedTimestamp,
                final String selfParticipantId,
                final String ringtoneUri,
                final boolean notificationEnabled,
                final boolean notificationVibrate,
                final int subId,
                final int participantCount,
                final String iconUri) {
            mConversationId = conversationId;
            mIsGroup = isGroup;
            mGroupConversationName = groupConversationName;
            mIncludeEmailAddress = includeEmailAddress;
            mReceivedTimestamp = receivedTimestamp;
            mSelfParticipantId = selfParticipantId;
            mLineInfos = new ArrayList<>();
            mTotalMessageCount = 0;
            mRingtoneUri = ringtoneUri;
            mNotificationEnabled = notificationEnabled;
            mNotificationVibrate = notificationVibrate;
            mSubId = subId;
            mParticipantCount = participantCount;
            mIconUri = iconUri;
        }

        public String getLatestMessageId() {
            final MessageLineInfo messageLineInfo = getLatestMessageLineInfo();
            if (messageLineInfo == null) {
                return null;
            }
            return messageLineInfo.mMessageId;
        }

        public boolean getDoesLatestMessageNeedDownload() {
            final MessageLineInfo messageLineInfo = getLatestMessageLineInfo();
            if (messageLineInfo == null) {
                return false;
            }
            return messageLineInfo.mIsManualDownloadNeeded;
        }

        private MessageLineInfo getLatestMessageLineInfo() {
            // The latest message is stored at index zero of the message line infos.
            if (mLineInfos.size() > 0 && mLineInfos.get(0) instanceof MessageLineInfo) {
                return (MessageLineInfo) mLineInfos.get(0);
            }
            return null;
        }

        public String getTitle() {
            if (mIsGroup) {
                return mGroupConversationName;
            } else {
                return mLineInfos.getFirst().mName;
            }
        }
    }

    /**
     * Information on all the notification messages across all conversations.
     */
    public static class ConversationsList {
        final int mMessageCount;
        final List<Conversation> mConversations;
        public ConversationsList(final int count, final List<Conversation> conversations) {
            mMessageCount = count;
            mConversations = conversations;
        }
    }

    final ConversationsList mConversationsList;
    private long mLatestReceivedTimestamp;

    public MessageNotificationState(final ConversationsList conversations) {
        super();
        mConversationsList = conversations;
        mType = PendingIntentConstants.SMS_NOTIFICATION_ID;
        mLatestReceivedTimestamp = Long.MIN_VALUE;
        if (conversations != null) {
            for (final Conversation info : conversations.mConversations) {
                mLatestReceivedTimestamp = Math.max(mLatestReceivedTimestamp,
                        info.mReceivedTimestamp);
            }
        }
    }

    public long getLatestReceivedTimestamp() {
        return mLatestReceivedTimestamp;
    }

    public int getNumRequestCodesNeeded() {
        // Get additional request codes for the Reply PendingIntent
        // and the DND PendingIntent.
        return NUM_REQUEST_CODES_NEEDED + NUM_EXTRA_REQUEST_CODES_NEEDED;
    }

    private int getBaseExtraRequestCode() {
        return mBaseRequestCode + getNumRequestCodesNeeded();
    }

    public int getReplyIntentRequestCode() {
        return getBaseExtraRequestCode() + REPLY_INTENT_REQUEST_CODE_OFFSET;
    }

    public PendingIntent getClearIntent(String conversationId) {
        return UIIntents.get().getPendingIntentForClearingNotifications(
                    Factory.get().getApplicationContext(),
                    BugleNotifications.UPDATE_MESSAGES,
                    ConversationIdSet.createSet(conversationId),
                    getClearIntentRequestCode());
    }

    public int getContentIntentRequestCode() {
        return mBaseRequestCode + CONTENT_INTENT_REQUEST_CODE_OFFSET;
    }

    public int getClearIntentRequestCode() {
        return mBaseRequestCode + CLEAR_INTENT_REQUEST_CODE_OFFSET;
    }

    private static HashMap<String, Integer> scanFirstNames(final String conversationId) {
        final Context context = Factory.get().getApplicationContext();
        final Uri uri =
                MessagingContentProvider.buildConversationParticipantsUri(conversationId);
        final ConversationParticipantsData participantsData = new ConversationParticipantsData();

        try (final Cursor participantsCursor = context.getContentResolver().query(
                    uri, ParticipantData.ParticipantsQuery.PROJECTION, null, null, null)) {
            participantsData.bind(participantsCursor);
        }

        final Iterator<ParticipantData> iter = participantsData.iterator();

        final HashMap<String, Integer> firstNames = new HashMap<String, Integer>();
        boolean seenSelf = false;
        while (iter.hasNext()) {
            final ParticipantData participant = iter.next();
            // Make sure we only add the self participant once
            if (participant.isSelf()) {
                if (seenSelf) {
                    continue;
                } else {
                    seenSelf = true;
                }
            }

            final String firstName = participant.getFirstName();
            if (firstName == null) {
                continue;
            }

            final int currentCount = firstNames.containsKey(firstName)
                    ? firstNames.get(firstName)
                    : 0;
            firstNames.put(firstName, currentCount + 1);
        }
        return firstNames;
    }

    /**
     * Performs a query on the database.
     */
    private static ConversationsList createConversationsList() {
        // Map key is conversation id. We use LinkedHashMap to ensure that entries are iterated in
        // the same order they were originally added. We scan unseen messages from newest to oldest,
        // so the corresponding conversations are added in that order, too.
        final Map<String, Conversation> conversations = new LinkedHashMap<>();
        int messageCount = 0;

        Cursor convMessageCursor = null;
        try {
            final Context context = Factory.get().getApplicationContext();
            final DatabaseWrapper db = DataModel.get().getDatabase();

            convMessageCursor = db.rawQuery(
                    ConversationMessageData.getNotificationQuerySql(),
                    null);

            if (convMessageCursor != null && convMessageCursor.moveToFirst()) {
                if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
                    LogUtil.v(TAG, "MessageNotificationState: Found unseen message notifications.");
                }
                final ConversationMessageData convMessageData =
                        new ConversationMessageData();

                HashMap<String, Integer> firstNames = null;
                String conversationIdForFirstNames = null;
                String groupConversationName = null;

                do {
                    convMessageData.bind(convMessageCursor);

                    // First figure out if this is a valid message.
                    String authorFullName = convMessageData.getSenderFullName();
                    String authorFirstName = convMessageData.getSenderFirstName();
                    String authorId = convMessageData.getParticipantId();
                    final String messageText = convMessageData.getText();

                    final String convId = convMessageData.getConversationId();
                    final String messageId = convMessageData.getMessageId();
                    final long timestamp = convMessageData.getReceivedTimeStamp();

                    CharSequence text = messageText;
                    final boolean isManualDownloadNeeded = convMessageData.getIsMmsNotification();
                    if (isManualDownloadNeeded) {
                        // Don't try and convert the text from html if it's sms and not a sms push
                        // notification.
                        Assert.equals(MessageData.BUGLE_STATUS_INCOMING_YET_TO_MANUAL_DOWNLOAD,
                                convMessageData.getStatus());
                        text = context.getResources().getString(
                                R.string.message_title_manual_download);
                    }
                    Conversation conversation = conversations.get(convId);
                    final Uri avatarUri = AvatarUriUtil.createAvatarUri(
                            convMessageData.getSenderProfilePhotoUri(),
                            convMessageData.getSenderFullName(),
                            convMessageData.getSenderNormalizedDestination(),
                            convMessageData.getSenderContactLookupKey());
                    if (conversation == null) {
                        final ConversationListItemData convData =
                                ConversationListItemData.getExistingConversation(db, convId);
                        final int subId = BugleDatabaseOperations.getSelfSubscriptionId(db,
                                convData.getSelfId());
                        groupConversationName = convData.getName();
                        conversation = new Conversation(convId,
                                convData.getIsGroup(),
                                groupConversationName,
                                convData.getIncludeEmailAddress(),
                                timestamp,
                                convData.getSelfId(),
                                convData.getNotificationSoundUri(),
                                convData.getNotificationEnabled(),
                                convData.getNotifiationVibrate(),
                                subId,
                                convData.getParticipantCount(),
                                convMessageData.getIconUri());
                        conversations.put(convId, conversation);
                    }
                    // Prepare the message line
                    if (conversation.mIsGroup) {
                        if (authorFirstName == null) {
                            // authorFullName might be null as well. In that case, we won't
                            // show an author. That is better than showing all the group
                            // names again on the 2nd line.
                            authorFirstName = authorFullName;
                        }
                    } else {
                        // don't recompute this if we don't need to
                        if (!TextUtils.equals(conversationIdForFirstNames, convId)) {
                            firstNames = scanFirstNames(convId);
                            conversationIdForFirstNames = convId;
                        }
                        if (firstNames != null) {
                            final Integer count = firstNames.get(authorFirstName);
                            if (count != null && count > 1) {
                                authorFirstName = authorFullName;
                            }
                        }

                        if (authorFullName == null) {
                            authorFullName = groupConversationName;
                        }
                        if (authorFirstName == null) {
                            authorFirstName = groupConversationName;
                        }
                    }
                    final String subjectText = MmsUtils.cleanseMmsSubject(
                            context.getResources(),
                            convMessageData.getMmsSubject());
                    if (!TextUtils.isEmpty(subjectText)) {
                        final String subjectLabel =
                                context.getString(R.string.subject_label);
                        final SpannableStringBuilder spanBuilder =
                                new SpannableStringBuilder();

                        spanBuilder.append(context.getString(R.string.notification_subject,
                                subjectLabel, subjectText));
                        spanBuilder.setSpan(new TextAppearanceSpan(
                                        context, R.style.NotificationSubjectText), 0,
                                subjectLabel.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (!TextUtils.isEmpty(text)) {
                            // Now add the actual message text below the subject header.
                            spanBuilder.append(System.getProperty("line.separator") + text);
                        }
                        text = spanBuilder;
                    }
                    // If we've got attachments, find the best one. If one of the messages is
                    // a photo, save the url so we'll display a big picture notification.
                    // Otherwise, show the first one we find.
                    Uri attachmentUri = null;
                    String attachmentType = null;
                    final MessagePartData messagePartData =
                            getMostInterestingAttachment(convMessageData);
                    if (messagePartData != null) {
                        attachmentUri = messagePartData.getContentUri();
                        attachmentType = messagePartData.getContentType();
                    }

                    Uri contactUri = convMessageData.getSenderContactLookupUri();
                    String contactUriString = null;
                    if (contactUri != null) {
                        contactUriString = contactUri.toString();
                    }

                    conversation.mLineInfos.add(new MessageLineInfo(authorId,
                            authorFullName, authorFirstName, text,
                            attachmentUri, attachmentType, isManualDownloadNeeded, avatarUri,
                            messageId, timestamp, contactUriString));
                    messageCount++;
                    conversation.mTotalMessageCount++;
                } while (convMessageCursor.moveToNext());
            }
        } finally {
            if (convMessageCursor != null) {
                convMessageCursor.close();
            }
        }
        if (conversations.isEmpty()) {
            return null;
        } else {
            return new ConversationsList(messageCount, new ArrayList<>(conversations.values()));
        }
    }

    /**
     * Scans all the attachments for a message and returns the most interesting one that we'll
     * show in a notification. By order of importance, in case there are multiple attachments:
     *      1- an image (because we can show the image as a BigPictureNotification)
     *      2- a video (because we can show a video frame as a BigPictureNotification)
     *      3- a vcard
     *      4- an audio attachment
     * @return MessagePartData for the most interesting part. Can be null.
     */
    private static MessagePartData getMostInterestingAttachment(
            final ConversationMessageData convMessageData) {
        final List<MessagePartData> attachments = convMessageData.getAttachments();

        MessagePartData imagePart = null;
        MessagePartData audioPart = null;
        MessagePartData vcardPart = null;
        MessagePartData videoPart = null;

        // 99.99% of the time there will be 0 or 1 part, since receiving slideshows is so
        // uncommon.

        // Remember the first of each type of part.
        for (final MessagePartData messagePartData : attachments) {
            if (messagePartData.isImage() && imagePart == null) {
                imagePart = messagePartData;
            }
            if (messagePartData.isVideo() && videoPart == null) {
                videoPart = messagePartData;
            }
            if (messagePartData.isVCard() && vcardPart == null) {
                vcardPart = messagePartData;
            }
            if (messagePartData.isAudio() && audioPart == null) {
                audioPart = messagePartData;
            }
        }
        if (imagePart != null) {
            return imagePart;
        } else if (videoPart != null) {
            return videoPart;
        } else if (audioPart != null) {
            return audioPart;
        } else if (vcardPart != null) {
            return vcardPart;
        }
        return null;
    }

    /**
     * Scans the database for messages that need to go into notifications.
     * messages from one sender.
     * @return NotificationState for the notification created.
     */
    public static MessageNotificationState getNotificationState() {
        MessageNotificationState state = null;
        final ConversationsList convList = createConversationsList();

        if (convList == null || convList.mConversations.size() == 0) {
            if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
                LogUtil.v(TAG, "MessageNotificationState: No unseen notifications");
            }
        } else {
            state = new MessageNotificationState(convList);
        }
        if (state != null && LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "MessageNotificationState: Notification state created");
        }
        return state;
    }

    public String getRingtoneUri() {
        if (mConversationsList.mConversations.size() > 0) {
            return mConversationsList.mConversations.get(0).mRingtoneUri;
        }
        return null;
    }

    /*
    private static void updateAlertStatusMessages(final long thresholdDeltaMs) {
        // TODO may need this when supporting error notifications
        final EsDatabaseHelper helper = EsDatabaseHelper.getDatabaseHelper();
        final ContentValues values = new ContentValues();
        final long nowMicros = System.currentTimeMillis() * 1000;
        values.put(MessageColumns.ALERT_STATUS, "1");
        final String selection =
                MessageColumns.ALERT_STATUS + "=0 AND (" +
                MessageColumns.STATUS + "=" + EsProvider.MESSAGE_STATUS_FAILED_TO_SEND + " OR (" +
                MessageColumns.STATUS + "!=" + EsProvider.MESSAGE_STATUS_ON_SERVER + " AND " +
                MessageColumns.TIMESTAMP + "+" + thresholdDeltaMs*1000 + "<" + nowMicros + ")) ";

        final int updateCount = helper.getWritableDatabaseWrapper().update(
                EsProvider.MESSAGES_TABLE,
                values,
                selection,
                null);
        if (updateCount > 0) {
            EsConversationsData.notifyConversationsChanged();
        }
    }*/

    static CharSequence applyWarningTextColor(final Context context,
            final CharSequence text) {
        if (text == null) {
            return null;
        }
        final SpannableStringBuilder spanBuilder = new SpannableStringBuilder();
        spanBuilder.append(text);
        spanBuilder.setSpan(new ForegroundColorSpan(context.getResources().getColor(
                R.color.notification_warning_color)), 0, text.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanBuilder;
    }

    /**
     * Check for failed messages and post notifications as needed.
     */
    public static void checkFailedMessages() {
        final DatabaseWrapper db = DataModel.get().getDatabase();

        final Cursor messageDataCursor = db.query(DatabaseHelper.MESSAGES_TABLE,
            MessageData.getProjection(),
            FailedMessageQuery.FAILED_MESSAGES_WHERE_CLAUSE,
            null /*selectionArgs*/,
            null /*groupBy*/,
            null /*having*/,
            FailedMessageQuery.FAILED_ORDER_BY);

        try {
            final Context context = Factory.get().getApplicationContext();
            final Resources resources = context.getResources();
            final NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(context);
            if (messageDataCursor != null) {
                final MessageData messageData = new MessageData();

                final HashSet<String> conversationsWithFailedMessages = new HashSet<String>();

                // track row ids in case we want to display something that requires this
                // information
                final ArrayList<Integer> failedMessages = new ArrayList<Integer>();

                int cursorPosition = -1;
                final long when = 0;

                messageDataCursor.moveToPosition(-1);
                while (messageDataCursor.moveToNext()) {
                    messageData.bind(messageDataCursor);

                    final String conversationId = messageData.getConversationId();
                    if (DataModel.get().isNewMessageObservable(conversationId)) {
                        // Don't post a system notification for an observable conversation
                        // because we already show an angry red annotation in the conversation
                        // itself or in the conversation preview snippet.
                        continue;
                    }

                    cursorPosition = messageDataCursor.getPosition();
                    failedMessages.add(cursorPosition);
                    conversationsWithFailedMessages.add(conversationId);
                }

                if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
                    LogUtil.d(TAG, "Found " + failedMessages.size() + " failed messages");
                }
                if (failedMessages.size() > 0) {
                    final NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(context);

                    CharSequence line1;
                    CharSequence line2;
                    final boolean isRichContent = false;
                    ConversationIdSet conversationIds = null;
                    PendingIntent destinationIntent;
                    if (failedMessages.size() == 1) {
                        messageDataCursor.moveToPosition(cursorPosition);
                        messageData.bind(messageDataCursor);
                        final String conversationId =  messageData.getConversationId();

                        // We have a single conversation, go directly to that conversation.
                        destinationIntent = UIIntents.get()
                                .getPendingIntentForConversationActivity(context,
                                        conversationId,
                                        null /*draft*/);

                        conversationIds = ConversationIdSet.createSet(conversationId);

                        final String failedMessgeSnippet = messageData.getMessageText();
                        int failureStringId;
                        if (messageData.getStatus() ==
                                MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED) {
                            failureStringId =
                                    R.string.notification_download_failures_line1_singular;
                        } else {
                            failureStringId = R.string.notification_send_failures_line1_singular;
                        }
                        line1 = resources.getString(failureStringId);
                        line2 = failedMessgeSnippet;
                        // Set rich text for non-SMS messages or MMS push notification messages
                        // which we generate locally with rich text
                        // TODO- fix this
//                        if (messageData.isMmsInd()) {
//                            isRichContent = true;
//                        }
                    } else {
                        // We have notifications for multiple conversation, go to the conversation
                        // list.
                        destinationIntent = UIIntents.get()
                            .getPendingIntentForConversationListActivity(context);

                        int line1StringId;
                        int line2PluralsId;
                        if (messageData.getStatus() ==
                                MessageData.BUGLE_STATUS_INCOMING_DOWNLOAD_FAILED) {
                            line1StringId =
                                    R.string.notification_download_failures_line1_plural;
                            line2PluralsId = R.plurals.notification_download_failures;
                        } else {
                            line1StringId = R.string.notification_send_failures_line1_plural;
                            line2PluralsId = R.plurals.notification_send_failures;
                        }
                        line1 = resources.getString(line1StringId);
                        line2 = resources.getQuantityString(
                                line2PluralsId,
                                conversationsWithFailedMessages.size(),
                                failedMessages.size(),
                                conversationsWithFailedMessages.size());
                    }
                    line1 = applyWarningTextColor(context, line1);
                    line2 = applyWarningTextColor(context, line2);

                    final PendingIntent pendingIntentForDelete =
                            UIIntents.get().getPendingIntentForClearingNotifications(
                                    context,
                                    BugleNotifications.UPDATE_ERRORS,
                                    conversationIds,
                                    0);

                    builder
                        .setContentTitle(line1)
                        .setTicker(line1)
                        .setWhen(when > 0 ? when : System.currentTimeMillis())
                        .setSmallIcon(R.drawable.ic_failed_light)
                        .setDeleteIntent(pendingIntentForDelete)
                        .setContentIntent(destinationIntent)
                        .setSound(UriUtil.getUriForResourceId(context, R.raw.message_failure));
                    if (isRichContent && !TextUtils.isEmpty(line2)) {
                        final NotificationCompat.InboxStyle inboxStyle =
                                new NotificationCompat.InboxStyle(builder);
                        if (line2 != null) {
                            inboxStyle.addLine(Html.fromHtml(line2.toString()));
                        }
                        builder.setStyle(inboxStyle);
                    } else {
                        builder.setContentText(line2);
                    }

                    if (builder != null) {
                        notificationManager.notify(
                                BugleNotifications.buildNotificationTag(
                                        PendingIntentConstants.MSG_SEND_ERROR, null),
                                PendingIntentConstants.MSG_SEND_ERROR,
                                builder.build());
                    }
                } else {
                    notificationManager.cancel(
                            BugleNotifications.buildNotificationTag(
                                    PendingIntentConstants.MSG_SEND_ERROR, null),
                            PendingIntentConstants.MSG_SEND_ERROR);
                }
            }
        } finally {
            if (messageDataCursor != null) {
                messageDataCursor.close();
            }
        }
    }
}
