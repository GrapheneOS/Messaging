package com.android.messaging.datamodel.action;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.messaging.Factory;
import com.android.messaging.R;
import com.android.messaging.datamodel.BugleDatabaseOperations;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.datamodel.DatabaseHelper;
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns;
import com.android.messaging.datamodel.DatabaseWrapper;
import com.android.messaging.datamodel.MessagingContentProvider;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Action to automatically delete old conversations that have been in deleted state
 * for longer than the configured retention period.
 */
public class AutoDeleteOldConversationsAction extends Action implements Parcelable {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;
    
    /**
     * Schedule auto-delete to run. This should be called daily.
     */
    public static void scheduleAutoDelete() {
        final AutoDeleteOldConversationsAction action = new AutoDeleteOldConversationsAction();
        action.start();
    }
    
    private AutoDeleteOldConversationsAction() {
        super();
    }
    
    @Override
    protected Object executeAction() {
        // Get the configured retention days from preferences
        final BuglePrefs prefs = BuglePrefs.getApplicationPrefs();
        final Context context = Factory.get().getApplicationContext();
        final String autoDeleteDaysKey = context.getString(R.string.auto_delete_days_pref_key);
        // Get default from resources (defined in constants.xml)
        final int defaultDays = context.getResources().getInteger(R.integer.auto_delete_days_default);
        // Now stored as integer in preferences
        final int retentionDays = prefs.getInt(autoDeleteDaysKey, defaultDays);
        
        final DatabaseWrapper db = DataModel.get().getDatabase();
        final List<String> conversationsToDelete = new ArrayList<>();
        
        // Find conversations marked as deleted where the retention period has expired
        // Using the same logic as getDaysUntilAutoDelete: 
        // days_since_deleted >= retention_days means it should be deleted
        Cursor cursor = null;
        try {
            // Get all deleted conversations
            cursor = db.query(DatabaseHelper.CONVERSATIONS_TABLE,
                    new String[] { ConversationColumns._ID, ConversationColumns.DELETED_TIMESTAMP },
                    ConversationColumns.DELETED_STATUS + " = 1 AND " +
                    ConversationColumns.DELETED_TIMESTAMP + " > 0",
                    null, null, null, null);
                    
            final long currentTime = System.currentTimeMillis();
            while (cursor.moveToNext()) {
                final String conversationId = cursor.getString(0);
                final long deletedTimestamp = cursor.getLong(1);
                
                // Calculate days since deletion (same as in getDaysUntilAutoDelete)
                final long millisSinceDeleted = currentTime - deletedTimestamp;
                final int daysSinceDeleted = (int) (millisSinceDeleted / (24L * 60L * 60L * 1000L));
                
                // If days since deleted >= retention days, delete it
                if (daysSinceDeleted >= retentionDays) {
                    conversationsToDelete.add(conversationId);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        // Permanently delete the old conversations
        int deletedCount = 0;
        for (String conversationId : conversationsToDelete) {
            if (BugleDatabaseOperations.deleteConversation(db, conversationId, Long.MAX_VALUE)) {
                deletedCount++;
                LogUtil.i(TAG, "Auto-deleted old conversation: " + conversationId);
            }
        }
        
        if (deletedCount > 0) {
            LogUtil.i(TAG, "Auto-deleted " + deletedCount + " old conversations");
            MessagingContentProvider.notifyConversationListChanged();
        }
        
        return null;
    }
    
    protected AutoDeleteOldConversationsAction(final Parcel in) {
        super(in);
    }
    
    public static final Parcelable.Creator<AutoDeleteOldConversationsAction> CREATOR
            = new Parcelable.Creator<AutoDeleteOldConversationsAction>() {
        @Override
        public AutoDeleteOldConversationsAction createFromParcel(final Parcel in) {
            return new AutoDeleteOldConversationsAction(in);
        }
        
        @Override
        public AutoDeleteOldConversationsAction[] newArray(final int size) {
            return new AutoDeleteOldConversationsAction[size];
        }
    };
    
    @Override
    public void writeToParcel(final Parcel parcel, final int flags) {
        writeActionToParcel(parcel, flags);
    }
}