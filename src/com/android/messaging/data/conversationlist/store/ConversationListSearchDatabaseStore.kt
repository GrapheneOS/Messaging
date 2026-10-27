package com.android.messaging.data.conversationlist.store

import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.data.conversationlist.model.SearchableParticipant
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.util.db.ext.getStringOrNull
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

internal interface ConversationListSearchDatabaseStore {

    fun readParticipants(): ImmutableMap<ConversationId, ImmutableList<SearchableParticipant>>
}

internal class ConversationListSearchDatabaseStoreImpl @Inject constructor() :
    ConversationListSearchDatabaseStore {

    override fun readParticipants():
        ImmutableMap<ConversationId, ImmutableList<SearchableParticipant>> {
        val participantsByConversation =
            LinkedHashMap<ConversationId, MutableList<SearchableParticipant>>()

        DataModel.get().database.rawQuery(PARTICIPANTS_SQL, null).use { cursor ->
            while (cursor.moveToNext()) {
                val conversationId = ConversationId.fromOrNull(
                    value = cursor.getStringOrNull(ConversationParticipantsColumns.CONVERSATION_ID),
                ) ?: continue

                participantsByConversation
                    .getOrPut(conversationId, ::mutableListOf)
                    .add(
                        SearchableParticipant(
                            fullName = cursor.getStringOrNull(ParticipantColumns.FULL_NAME),
                            firstName = cursor.getStringOrNull(ParticipantColumns.FIRST_NAME),
                            displayDestination = cursor.getStringOrNull(
                                ParticipantColumns.DISPLAY_DESTINATION,
                            ),
                            normalizedDestination = cursor.getStringOrNull(
                                ParticipantColumns.NORMALIZED_DESTINATION,
                            ),
                        ),
                    )
            }
        }

        return participantsByConversation
            .mapValues { (_, participants) -> participants.toImmutableList() }
            .toImmutableMap()
    }

    private companion object {
        private val PARTICIPANTS_SQL = """
            SELECT member.${ConversationParticipantsColumns.CONVERSATION_ID},
                participant.${ParticipantColumns.FULL_NAME},
                participant.${ParticipantColumns.FIRST_NAME},
                participant.${ParticipantColumns.DISPLAY_DESTINATION},
                participant.${ParticipantColumns.NORMALIZED_DESTINATION}
            FROM ${DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE} AS member
            JOIN ${DatabaseHelper.PARTICIPANTS_TABLE} AS participant
                ON participant.${ParticipantColumns._ID} =
                    member.${ConversationParticipantsColumns.PARTICIPANT_ID}
            ORDER BY member.${ConversationParticipantsColumns._ID}
        """.trimIndent()
    }
}
