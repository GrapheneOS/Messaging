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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.MessagingStyle;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.content.LocusIdCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.MessageNotificationState.Conversation;
import com.android.messaging.datamodel.action.MarkAsReadAction;
import com.android.messaging.datamodel.action.MarkAsSeenAction;
import com.android.messaging.datamodel.action.RedownloadMmsAction;
import com.android.messaging.datamodel.data.ConversationListItemData;
import com.android.messaging.datamodel.media.AvatarGroupRequestDescriptor;
import com.android.messaging.datamodel.media.AvatarRequestDescriptor;
import com.android.messaging.datamodel.media.ImageRequestDescriptor;
import com.android.messaging.datamodel.media.ImageResource;
import com.android.messaging.datamodel.media.MediaRequest;
import com.android.messaging.datamodel.media.MediaResourceManager;
import com.android.messaging.sms.MmsSmsUtils;
import com.android.messaging.sms.MmsUtils;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.Assert;
import com.android.messaging.util.AvatarUriUtil;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.NotificationChannelUtil;
import com.android.messaging.util.NotificationPlayer;
import com.android.messaging.util.PendingIntentConstants;
import com.android.messaging.util.PhoneUtils;
import com.android.messaging.util.RingtoneUtil;
import com.android.messaging.util.ThreadUtil;
import com.android.messaging.util.UriUtil;

import java.util.List;
import java.util.Optional;

/**
 * Handle posting, updating and removing all conversation notifications.<p>
 *
 * There are currently two main classes of notification and their rules: <p>
 * 1) Messages - {@link MessageNotificationState}. Only one message notification.
 * Unread messages across senders and conversations are coalesced.<p>
 * 2) Failed Messages - {@link MessageNotificationState#checkFailedMessages } Only one failed
 * message. Multiple failures are coalesced.<p>
 *
 * To add a new class of notifications, subclass the NotificationState and add commands which
 * create one and pass into general creation function.
 *
 */
public class BugleNotifications {
    // Logging
    public static final String TAG = LogUtil.BUGLE_NOTIFICATIONS_TAG;

    // Constants to use for update.
    public static final int UPDATE_NONE = 0;
    public static final int UPDATE_MESSAGES = 1;
    public static final int UPDATE_ERRORS = 2;
    public static final int UPDATE_ALL = UPDATE_MESSAGES + UPDATE_ERRORS;

    private static final String SMS_NOTIFICATION_TAG = ":sms:";
    private static final String SMS_ERROR_NOTIFICATION_TAG = ":error:";

    /**
     * This is the volume at which to play the observable-conversation notification sound,
     * expressed as a fraction of the system notification volume.
     */
    private static final float OBSERVABLE_CONVERSATION_NOTIFICATION_VOLUME = 0.25f;

    /**
     * Entry point for posting notifications.
     * Don't call this on the UI thread.
     * @param coverage Indicates which notification types should be checked. Valid values are
     * UPDATE_NONE, UPDATE_MESSAGES, UPDATE_ERRORS, or UPDATE_ALL
     */
    public static void update(final int coverage) {
        update(null /* conversationId */, coverage);
    }

    /**
     * Entry point for posting notifications.
     * Don't call this on the UI thread.
     * @param conversationId Conversation ID where a new message was received
     * @param coverage Indicates which notification types should be checked. Valid values are
     * UPDATE_NONE, UPDATE_MESSAGES, UPDATE_ERRORS, or UPDATE_ALL
     */
    public static void update(final String conversationId, final int coverage) {
        if (LogUtil.isLoggable(TAG, LogUtil.VERBOSE)) {
            LogUtil.v(TAG, "Update: conversationId = " + conversationId
                    + " coverage = " + coverage);
        }
    Assert.isNotMainThread();

        if (!shouldNotify()) {
            return;
        }
        if ((coverage & UPDATE_MESSAGES) != 0) {
            if (conversationId == null) {
                cancel(PendingIntentConstants.SMS_NOTIFICATION_ID);
            } else {
                createMessageNotification(conversationId);
            }
        }
        if ((coverage & UPDATE_ERRORS) != 0) {
            MessageNotificationState.checkFailedMessages();
        }
    }

    private static void createMessageNotification(final String conversationId) {
        final MessageNotificationState state = MessageNotificationState.getNotificationState();
        final boolean softSound = DataModel.get().isNewMessageObservable(conversationId);
        if (state == null) {
            if (softSound && !TextUtils.isEmpty(conversationId)) {
                final Uri ringtoneUri = getNotificationRingtoneUriForConversationId(conversationId);
                playObservableConversationNotificationSound(ringtoneUri);
            }
            return;
        }

        // Send per-conversation notifications (if there are multiple conversations).
        Optional<Conversation> conversation =
                state.mConversationsList.mConversations.stream().findFirst();
        conversation.ifPresent(conv -> processAndSend(state, conv));
    }

    /**
     * Cancel all notifications of a certain type.
     *
     * @param type Message or error notifications from Constants.
     */
    private static synchronized void cancel(final int type) {
        cancel(type, null);
    }

    /**
     * Cancel all notifications of a certain type.
     *
     * @param type Message or error notifications from Constants.
     * @param conversationId If set, cancel the notification for this
     *            conversation only. For message notifications, this only works
     *            if the notifications are bundled (group children).
     */
    public static synchronized void cancel(final int type, final String conversationId) {
        final String notificationTag = buildNotificationTag(type, conversationId);
        final NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(Factory.get().getApplicationContext());

        if (conversationId == null || conversationId.isBlank()) {
            notificationManager.getActiveNotifications().forEach(notification -> {
                String activeTag = notification.getTag();
                if (activeTag.contains(notificationTag)) {
                    notificationManager.cancel(activeTag, type);
                }
            });
        } else {
            notificationManager.cancel(notificationTag, type);
        }

        if (LogUtil.isLoggable(TAG, LogUtil.DEBUG)) {
            LogUtil.d(TAG, "Canceled notifications of type " + type);
        }
    }

    /**
     * Returns {@code true} if incoming notifications should display a
     * notification, {@code false} otherwise.
     *
     * @return true if the notification should occur
     */
    private static boolean shouldNotify() {
        // If we're not the default sms app, don't put up any notifications.
        return PhoneUtils.getDefault().isDefaultSmsApp();
    }

    private static Uri getNotificationRingtoneUriForConversationId(final String conversationId) {
        final DatabaseWrapper db = DataModel.get().getDatabase();
        final ConversationListItemData convData =
                ConversationListItemData.getExistingConversation(db, conversationId);
        return RingtoneUtil.getNotificationRingtoneUri(conversationId,
                convData != null ? convData.getNotificationSoundUri() : null);
    }

    /**
     * Returns a unique tag to identify a notification.
     *
     * @param name The tag name (in practice, the type)
     * @param conversationId The conversation id (optional)
     */
    private static String buildNotificationTag(final String name,
            final String conversationId) {
        final Context context = Factory.get().getApplicationContext();
        if (conversationId != null) {
            return context.getPackageName() + name + ":" + conversationId;
        } else {
            return context.getPackageName() + name;
        }
    }

    /**
     * Returns a unique tag to identify a notification.
     *
     * @param type One of the constants in {@link PendingIntentConstants}
     * @param conversationId The conversation id (where applicable)
     */
    static String buildNotificationTag(final int type, final String conversationId) {
        String tag = null;
        switch(type) {
            case PendingIntentConstants.SMS_NOTIFICATION_ID:
                tag = buildNotificationTag(SMS_NOTIFICATION_TAG, conversationId);
                break;
            case PendingIntentConstants.MSG_SEND_ERROR:
                tag = buildNotificationTag(SMS_ERROR_NOTIFICATION_TAG, null);
                break;
        }
        return tag;
    }

    private static void processAndSend(final MessageNotificationState state, final Conversation conversation) {
        final Context context = Factory.get().getApplicationContext();
        final String conversationId = conversation.mConversationId;
        final NotificationCompat.Builder notifBuilder =
                new NotificationCompat.Builder(context, conversationId);
        notifBuilder.setCategory(Notification.CATEGORY_MESSAGE);

        NotificationChannelUtil.INSTANCE.createConversationChannel(
                conversationId,
                conversation.getTitle(),
                conversation.mNotificationEnabled,
                state.getRingtoneUri(),
                conversation.mNotificationVibrate
        );

        state.mBaseRequestCode = state.mType;

        final PendingIntent clearIntent = state.getClearIntent(conversationId);
        notifBuilder.setDeleteIntent(clearIntent);

        // Set the content intent
        PendingIntent contentIntent = UIIntents.get()
                .getPendingIntentForConversationActivity(context, conversationId, null /*draft*/);
        notifBuilder.setContentIntent(contentIntent);

        MessagingStyle style = null;
        Notification activeNotification = NotificationChannelUtil.INSTANCE.getActiveNotification(conversationId);
        long oldestExistingTimestamp = Long.MIN_VALUE;
        if (activeNotification != null) {
            style = MessagingStyle.extractMessagingStyleFromNotification(activeNotification);
            if (style != null) {
                List<MessagingStyle.Message> messages = style.getMessages();
                if (!messages.isEmpty()) {
                    oldestExistingTimestamp = messages.getLast().getTimestamp();
                } else {
                    LogUtil.e(TAG, "MessageNotificationState: Notification has no messages");
                    return;
                }
            }
        }

        // It's possible for the NotificationManager to give us a reference to an "active"
        // notification which has actually been dismissed and will have a null style. Make sure that
        // we create a style here if that happens or if the manager never found a notification.
        if (style == null) {
            style = new MessagingStyle(
                    new Person.Builder().setName(context.getString(R.string.unknown_self_participant)).build()
            );
        }

        Person latestPerson = null;
        List<MessageNotificationState.MessageLineInfo> reversedLineInfos = conversation.mLineInfos.reversed();
        for (MessageNotificationState.MessageLineInfo messageLineInfo : reversedLineInfos) {
            // Add people associated with this notification
            Person currentPerson = messageLineInfo.createPerson();
            notifBuilder.addPerson(currentPerson);

            // Don't repeat messages by checking the timestamp
            if (messageLineInfo.mTimestamp <= oldestExistingTimestamp) {
                if (reversedLineInfos.getLast() == messageLineInfo) {
                    // No changes were made to this notification
                    return;
                }
                continue;
            }

            MessagingStyle.Message message = messageLineInfo.createStyledMessage(currentPerson);
            style.addMessage(message);
            style.setGroupConversation(conversation.mIsGroup);
            latestPerson = currentPerson;
        }
        notifBuilder.setWhen(conversation.mReceivedTimestamp);

        if (conversation.mIsGroup) {
            Person groupPerson = createGroupPerson(context, conversation);
            setUpShortcuts(context, conversation, notifBuilder, groupPerson);
        } else {
            setUpShortcuts(context, conversation, notifBuilder, latestPerson);
        }

        final PendingIntent markAsReadPendingIntent =
                UIIntents.get().getPendingIntentForMarkingAsRead(context, conversationId);
        final NotificationCompat.Action markAsReadActionBuilder =
                new NotificationCompat.Action.Builder(0,
                        context.getString(R.string.mark_as_read), markAsReadPendingIntent).build();
        notifBuilder.addAction(markAsReadActionBuilder);

        final String selfId = conversation.mSelfParticipantId;

        final boolean requiresMms =
                MmsSmsUtils.getRequireMmsForEmailAddress(
                        conversation.mIncludeEmailAddress, conversation.mSubId) ||
                        (conversation.mIsGroup && MmsUtils.groupMmsEnabled(conversation.mSubId));

        final int requestCode = state.getReplyIntentRequestCode();
        final PendingIntent replyPendingIntent = UIIntents.get()
                .getPendingIntentForSendingMessageToConversation(context,
                        conversationId, selfId, requiresMms, requestCode);

        final NotificationCompat.Action.Builder replyActionBuilder =
                new NotificationCompat.Action.Builder(0,
                        context.getString(R.string.notification_reply_prompt), replyPendingIntent);
        final RemoteInput remoteInput = new RemoteInput.Builder(Intent.EXTRA_TEXT).setLabel(
                        context.getString(R.string.notification_reply_prompt))
                .build();
        replyActionBuilder.addRemoteInput(remoteInput);
        notifBuilder.addAction(replyActionBuilder.build());

        final String messageId = conversation.getLatestMessageId();
        if (conversation.getDoesLatestMessageNeedDownload() && messageId != null) {
            final PendingIntent downloadPendingIntent =
                    RedownloadMmsAction.getPendingIntentForRedownloadMms(context, messageId);

            final NotificationCompat.Action.Builder actionBuilder =
                    new NotificationCompat.Action.Builder(R.drawable.ic_file_download_light,
                            context.getString(R.string.notification_download_mms),
                            downloadPendingIntent);
            final NotificationCompat.Action downloadAction = actionBuilder.build();
            notifBuilder.addAction(downloadAction);
        }

        notifBuilder
                .setSmallIcon(R.drawable.ic_sms_light)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        notifBuilder.setStyle(style);

        // Mark the notification as finished
        state.mCanceled = true;

        final int type = state.mType;
        final NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(Factory.get().getApplicationContext());
        final String notificationTag = buildNotificationTag(type, conversationId);

        Notification notification = notifBuilder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(notificationTag, type, notification);

        LogUtil.i(TAG, "Notifying for conversation " + conversationId + "; "
                + "tag = " + notificationTag + ", type = " + type);
    }

    /**
     * Play the observable conversation notification sound (it's the regular notification sound, but
     * played at half-volume)
     */
    private static void playObservableConversationNotificationSound(final Uri ringtoneUri) {
        final Context context = Factory.get().getApplicationContext();
        final AudioManager audioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);
        final boolean silenced =
                audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL;
        if (silenced) {
             return;
        }

        final NotificationPlayer player = new NotificationPlayer(LogUtil.BUGLE_TAG);
        player.play(ringtoneUri, false,
                AudioManager.STREAM_NOTIFICATION,
                OBSERVABLE_CONVERSATION_NOTIFICATION_VOLUME);

        // Stop the sound after five seconds to handle continuous ringtones
        ThreadUtil.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                player.stop();
            }
        }, 5000);
    }

    /**
     * When we go to the conversation list, call this to mark all messages as seen. That means
     * we won't show a notification again for the same message.
     */
    public static void markAllMessagesAsSeen() {
        MarkAsSeenAction.markAllAsSeen();
    }

    /**
     * When we open a particular conversation, call this to mark all messages as read.
     */
    public static void markMessagesAsRead(final String conversationId, final boolean cancelNotification) {
        MarkAsReadAction.markAsRead(conversationId, cancelNotification);
    }

    public static void notifyEmergencySmsFailed(final String emergencyNumber,
            final String conversationId) {
        final Context context = Factory.get().getApplicationContext();

        final CharSequence line1 = MessageNotificationState.applyWarningTextColor(context,
                context.getString(R.string.notification_emergency_send_failure_line1,
                emergencyNumber));
        final String line2 = context.getString(R.string.notification_emergency_send_failure_line2,
                emergencyNumber);
        final PendingIntent destinationIntent = UIIntents.get()
                .getPendingIntentForConversationActivity(context, conversationId, null /* draft */);

        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, NotificationChannelUtil.ALERTS_CHANNEL);
        builder.setTicker(line1)
                .setContentTitle(line1)
                .setContentText(line2)
                .setStyle(new NotificationCompat.BigTextStyle(builder).bigText(line2))
                .setSmallIcon(R.drawable.ic_failed_light)
                .setContentIntent(destinationIntent)
                .setSound(UriUtil.getUriForResourceId(context, R.raw.message_failure));

        final String tag = context.getPackageName() + ":emergency_sms_error";
        NotificationManagerCompat.from(context).notify(
                tag,
                PendingIntentConstants.MSG_SEND_ERROR,
                builder.build());
    }

    /**
     * Gets a {@link Bitmap} for an avatar {@link Uri}.
     * @param context {@link Context} for building the image request.
     * @param avatarUri {@link Uri} for the avatar.
     * @return The requested {@link Bitmap} and null if the request failed.
     */
    public static Bitmap getAvatarBitmap(Context context, Uri avatarUri) {
        final int iconSize = (int) context.getResources()
                .getDimension(R.dimen.contact_icon_view_normal_size);

        ImageRequestDescriptor descriptor;
        final String avatarType = AvatarUriUtil.getAvatarType(avatarUri);
        if (AvatarUriUtil.TYPE_GROUP_URI.equals(avatarType)) {
            descriptor = new AvatarGroupRequestDescriptor(avatarUri, iconSize, iconSize);
        } else {
            descriptor = new AvatarRequestDescriptor(avatarUri, iconSize, iconSize, true);
        }

        final MediaRequest<ImageResource> imageRequest = descriptor.buildSyncMediaRequest(
                context);
        final ImageResource avatarImage =
                MediaResourceManager.get().requestMediaResourceSync(imageRequest);

        if (avatarImage != null) {
            // We have to make copies of the bitmaps to hand to the NotificationManager
            // because the bitmap in the ImageResource is managed and will automatically
            // get released.
            Bitmap shareableAvatarBitmap = Bitmap.createBitmap(avatarImage.getBitmap());
            avatarImage.release();
            return shareableAvatarBitmap;
        }

        return null;
    }

    public static void updateWithInlineReply(final String conversationId, final String message) {
        Context context = Factory.get().getApplicationContext();
        Notification activeNotification =
                NotificationChannelUtil.INSTANCE.getActiveNotification(conversationId);
        if (activeNotification != null) {
            MessagingStyle activeStyle =
                    MessagingStyle.extractMessagingStyleFromNotification(activeNotification);
            if (activeStyle != null) {
                NotificationCompat.Builder recoveredBuilder =
                        new NotificationCompat.Builder(context, activeNotification);

                String selfString = context.getString(R.string.unknown_self_participant);
                activeStyle.addMessage(new NotificationCompat.MessagingStyle.Message(message,
                        System.currentTimeMillis(),
                        new Person.Builder().setName(selfString).build()));
                recoveredBuilder.setStyle(activeStyle);
                recoveredBuilder.setOnlyAlertOnce(true);

                String tag = buildNotificationTag(PendingIntentConstants.SMS_NOTIFICATION_ID, conversationId);
                NotificationManagerCompat.from(context)
                        .notify(tag, PendingIntentConstants.SMS_NOTIFICATION_ID, recoveredBuilder.build());
            }
        }
    }

    private static void setUpShortcuts(Context context, Conversation conversation,
                                       NotificationCompat.Builder notifBuilder, Person person) {
        Intent conversationActivityIntent = UIIntents.get().getShortcutIntentForConversationActivity(
                context, conversation.mConversationId);
        ShortcutInfoCompat.Builder shortcutBuilder =
                new ShortcutInfoCompat.Builder(context, conversation.mConversationId)
                        .setShortLabel(conversation.getTitle())
                        .setLocusId(new LocusIdCompat(conversation.mConversationId))
                        .setLongLived(true)
                        .setPerson(person)
                        .setIntent(conversationActivityIntent);

        IconCompat icon;
        IconCompat personIcon = person.getIcon();
        if (personIcon != null) {
            icon = personIcon;
        } else {
            icon = IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground);
        }
        shortcutBuilder.setIcon(icon);

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcutBuilder.build());
        notifBuilder.setShortcutId(conversation.mConversationId);

        NotificationCompat.BubbleMetadata.Builder bubbleBuilder =
                new NotificationCompat.BubbleMetadata.Builder(conversation.mConversationId);
        bubbleBuilder.setDesiredHeightResId(R.dimen.max_bubbled_activity_height);
        notifBuilder.setBubbleMetadata(bubbleBuilder.build());
    }

    private static Person createGroupPerson(Context context, Conversation conversation) {
        Person.Builder personBuilder = new Person.Builder()
                .setName(conversation.getTitle())
                .setKey(conversation.mConversationId);

        if (conversation.mIconUri != null) {
            Bitmap groupIconBitmap = getAvatarBitmap(context, Uri.parse(conversation.mIconUri));
            if (groupIconBitmap != null) {
                personBuilder.setIcon(IconCompat.createWithBitmap(groupIconBitmap));
            }
        }

        return personBuilder.build();
    }
}

