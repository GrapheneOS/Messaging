package com.android.messaging.data.conversationlist.store

import com.android.messaging.data.secondaryuser.SecondaryUserNotifier
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.action.MarkAsSeenAction
import javax.inject.Inject

internal interface ConversationListStatusStore {
    fun hasFirstSyncCompleted(): Boolean
    fun setNewestConversationVisible(isVisible: Boolean)
}

internal class ConversationListStatusStoreImpl @Inject constructor(
    private val secondaryUserNotifier: SecondaryUserNotifier,
) : ConversationListStatusStore {

    override fun hasFirstSyncCompleted(): Boolean {
        val dataModel = DataModel.get()
        return dataModel.syncManager.hasFirstSyncCompleted
    }

    override fun setNewestConversationVisible(isVisible: Boolean) {
        val dataModel = DataModel.get()
        dataModel.isConversationListScrolledToNewestConversation = isVisible

        if (isVisible) {
            // False keeps posted notifications because bubbles live on them.
            MarkAsSeenAction.markAllAsSeen(false)
            secondaryUserNotifier.cancel()
        }
    }
}
