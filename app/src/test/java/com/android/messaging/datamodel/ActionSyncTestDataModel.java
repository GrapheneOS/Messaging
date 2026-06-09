package com.android.messaging.datamodel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.android.messaging.datamodel.action.ActionService;
import com.android.messaging.datamodel.action.BackgroundWorker;
import com.android.messaging.datamodel.data.BlockedParticipantsData;
import com.android.messaging.datamodel.data.ContactListItemData;
import com.android.messaging.datamodel.data.ContactPickerData;
import com.android.messaging.datamodel.data.ConversationData;
import com.android.messaging.datamodel.data.ConversationListData;
import com.android.messaging.datamodel.data.DraftMessageData;
import com.android.messaging.datamodel.data.GalleryGridItemData;
import com.android.messaging.datamodel.data.LaunchConversationData;
import com.android.messaging.datamodel.data.MediaPickerData;
import com.android.messaging.datamodel.data.MessagePartData;
import com.android.messaging.datamodel.data.VCardContactItemData;
import com.android.messaging.util.Assert;

public class ActionSyncTestDataModel extends DataModel {
    private DatabaseWrapper mDatabase;
    private final SyncManager mSyncManager = new SyncManager();
    private final ActionService mActionService = new ActionService();
    private final BackgroundWorker mBackgroundWorker = new BackgroundWorker();

    public void setDatabase(final DatabaseWrapper database) {
        mDatabase = database;
    }

    @Override
    public ConversationListData createConversationListData(final Context context,
            final ConversationListData.ConversationListDataListener listener,
            final boolean archivedMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConversationData createConversationData(final Context context,
            final ConversationData.ConversationDataListener listener,
            final String conversationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContactListItemData createContactListItemData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContactPickerData createContactPickerData(final Context context,
            final ContactPickerData.ContactPickerDataListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MediaPickerData createMediaPickerData(final Context context) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GalleryGridItemData createGalleryGridItemData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LaunchConversationData createLaunchConversationData(
            final LaunchConversationData.LaunchConversationDataListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VCardContactItemData createVCardContactItemData(final Context context,
            final MessagePartData data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VCardContactItemData createVCardContactItemData(final Context context,
            final Uri vCardUri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockedParticipantsData createBlockedParticipantsData(final Context context,
            final BlockedParticipantsData.BlockedParticipantsDataListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DraftMessageData createDraftMessageData(final String conversationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ActionService getActionService() {
        return mActionService;
    }

    @Override
    public BackgroundWorker getBackgroundWorkerForActionService() {
        return mBackgroundWorker;
    }

    @Override
    public DatabaseWrapper getDatabase() {
        Assert.isNotMainThread();
        return mDatabase;
    }

    @Override
    public void onActivityResume() {
    }

    @Override
    void onCreateTables(final SQLiteDatabase db) {
    }

    @Override
    public void onApplicationCreated() {
    }

    @Override
    public SyncManager getSyncManager() {
        return mSyncManager;
    }
}
