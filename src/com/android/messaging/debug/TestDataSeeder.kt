@file:JvmName("TestDataSeeder")

package com.android.messaging.debug

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.ConversationParticipantsColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.PartColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.DatabaseWrapper
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import com.android.messaging.datamodel.data.ParticipantData
import com.android.messaging.util.ContentType
import com.android.messaging.util.LogUtil
import com.android.messaging.util.db.ext.withTransaction
import java.io.File
import java.io.FileOutputStream

private const val TAG = "TestDataSeeder"
private const val TEST_PHONE_PREFIX = "+15550"
private const val TEST_YOUTUBE_VIDEO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
private const val MEDIA_SCRATCH_FILE_EXTENSION_QUERY_PARAMETER = "ext"
private const val SEED_IMAGE_1_FILE_ID = "800001"
private const val SEED_IMAGE_2_FILE_ID = "800002"
private const val SEED_IMAGE_3_FILE_ID = "800003"
private const val SEED_VCARD_FILE_ID = "800004"
private const val SEED_VIDEO_FILE_ID = "800005"

private const val MINUTES = 60 * 1000L
private const val HOURS = 60 * MINUTES
private const val DAYS = 24 * HOURS

fun seedTestData(context: Context) {
    clearSeededTestData(context = context)

    val db = DataModel.get().getDatabase()

    val selfId = findSelfParticipantId(db) ?: run {
        LogUtil.w(TAG, "No self participant found — open the app at least once before seeding")
        return
    }

    val testImages = buildTestImages(context)
    val testVideo = buildTestVideo(context)
    val testVCard = buildTestVCard(context)
    val now = System.currentTimeMillis()

    db.withTransaction {
        val alice = upsertParticipant(db, "${TEST_PHONE_PREFIX}001234", "Alice Wonderland", "Alice")
        val bob = upsertParticipant(db, "${TEST_PHONE_PREFIX}005678", "Bob Baker", "Bob")
        val carol = upsertParticipant(db, "${TEST_PHONE_PREFIX}002345", "Carol Chen", "Carol")
        val dave = upsertParticipant(db, "${TEST_PHONE_PREFIX}003456", "Dave Diaz", "Dave")
        val eve = upsertParticipant(db, "${TEST_PHONE_PREFIX}004567", "Eve Evans", "Eve")
        val frank = upsertParticipant(db, "${TEST_PHONE_PREFIX}006789", "Frank Ford", "Frank")
        val grace = upsertParticipant(db, "${TEST_PHONE_PREFIX}007890", "Grace Green", "Grace")
        val henry = upsertParticipant(db, "${TEST_PHONE_PREFIX}008901", "Henry Hall", "Henry")
        val iris = upsertParticipant(db, "${TEST_PHONE_PREFIX}009012", "Iris Ingram", "Iris")
        val jack = upsertParticipant(db, "${TEST_PHONE_PREFIX}010123", "Jack Johnson", "Jack")

        seedScenarioA(db, selfId, alice, now)
        seedScenarioB(db, selfId, bob, now)
        seedScenarioC(db, selfId, carol, dave, eve, now)
        seedScenarioD(db, selfId, frank, now)
        seedScenarioE(db, selfId, grace, now)
        seedScenarioF(db, selfId, henry, now)
        seedScenarioG(db, selfId, iris, testImages, now)
        seedScenarioH(db, selfId, jack, carol, testImages, testVideo, testVCard, now)
        seedScenarioI(db, selfId, carol, dave, eve, now)
    }

    MessagingContentProvider.notifyConversationListChanged()
    LogUtil.d(TAG, "Test data seeded successfully")
}

fun clearSeededTestData(context: Context) {
    val db = DataModel.get().getDatabase()
    val seededAttachmentUris = mutableSetOf<String>()

    db.withTransaction {
        val participantIds = mutableListOf<String>()
        db.query(
            DatabaseHelper.PARTICIPANTS_TABLE,
            arrayOf(ParticipantColumns._ID),
            "${ParticipantColumns.NORMALIZED_DESTINATION} LIKE ?",
            arrayOf("$TEST_PHONE_PREFIX%"),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) participantIds.add(cursor.getString(0))
        }

        if (participantIds.isEmpty()) {
            db.setTransactionSuccessful()
            return
        }

        val pPlaceholders = participantIds.joinToString(",") { "?" }
        val pArgs = participantIds.toTypedArray()

        val conversationIds = mutableListOf<String>()
        db.query(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            arrayOf(ConversationParticipantsColumns.CONVERSATION_ID),
            "${ConversationParticipantsColumns.PARTICIPANT_ID} IN ($pPlaceholders)",
            pArgs,
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) conversationIds.add(cursor.getString(0))
        }

        if (conversationIds.isNotEmpty()) {
            val conversationIdArgs = conversationIds.toTypedArray()
            db.query(
                DatabaseHelper.PARTS_TABLE,
                arrayOf(PartColumns.CONTENT_URI),
                "${PartColumns.CONVERSATION_ID} IN (${conversationIds.joinToString(",") { "?" }})" +
                    " AND ${PartColumns.CONTENT_URI} IS NOT NULL",
                conversationIdArgs,
                null,
                null,
                null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.let { attachmentUri ->
                        seededAttachmentUris.add(attachmentUri)
                    }
                }
            }

            // ON DELETE CASCADE handles messages and parts
            db.delete(
                DatabaseHelper.CONVERSATIONS_TABLE,
                "${ConversationColumns._ID} IN (${conversationIds.joinToString(",") { "?" }})",
                conversationIdArgs,
            )
        }

        db.delete(
            DatabaseHelper.PARTICIPANTS_TABLE,
            "${ParticipantColumns._ID} IN ($pPlaceholders)",
            pArgs,
        )
    }

    // Also clean up the image files from cache
    for (i in 1..3) {
        File(context.cacheDir, "seed_img_$i.jpg").delete()
    }
    File(context.cacheDir, "seed_video.mp4").delete()
    File(context.cacheDir, "seed_contact.vcf").delete()
    deleteSeedScratchFile(fileId = SEED_IMAGE_1_FILE_ID, fileExtension = "jpg")
    deleteSeedScratchFile(fileId = SEED_IMAGE_2_FILE_ID, fileExtension = "jpg")
    deleteSeedScratchFile(fileId = SEED_IMAGE_3_FILE_ID, fileExtension = "jpg")
    deleteSeedScratchFile(fileId = SEED_VCARD_FILE_ID, fileExtension = "vcf")
    deleteSeedScratchFile(fileId = SEED_VIDEO_FILE_ID, fileExtension = "mp4")
    deleteSeedScratchFile(fileId = SEED_IMAGE_1_FILE_ID)
    deleteSeedScratchFile(fileId = SEED_IMAGE_2_FILE_ID)
    deleteSeedScratchFile(fileId = SEED_IMAGE_3_FILE_ID)
    deleteSeedScratchFile(fileId = SEED_VCARD_FILE_ID)
    deleteSeedScratchFile(fileId = SEED_VIDEO_FILE_ID)
    deleteSeededAttachmentScratchFiles(attachmentUris = seededAttachmentUris)

    MessagingContentProvider.notifyConversationListChanged()
    LogUtil.d(TAG, "Seeded test data cleared")
}

private fun buildTestImages(context: Context): List<String> {
    val specs = listOf(
        SeedImageSpec(
            fileId = SEED_IMAGE_1_FILE_ID,
            fileExtension = "jpg",
            backgroundColor = Color.rgb(100, 149, 237),
            label = "Photo 1",
        ),
        SeedImageSpec(
            fileId = SEED_IMAGE_2_FILE_ID,
            fileExtension = "jpg",
            backgroundColor = Color.rgb(144, 238, 144),
            label = "Photo 2",
        ),
        SeedImageSpec(
            fileId = SEED_IMAGE_3_FILE_ID,
            fileExtension = "jpg",
            backgroundColor = Color.rgb(255, 160, 122),
            label = "Photo 3",
        ),
    )

    return specs.map { spec ->
        val imageUri = buildSeedScratchUri(
            fileId = spec.fileId,
            fileExtension = spec.fileExtension,
        )
        val file = MediaScratchFileProvider.getFileFromUri(imageUri)
        val bmp = createBitmap(400, 300)
        val canvas = Canvas(bmp)
        canvas.drawColor(spec.backgroundColor)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            isAntiAlias = true
            isFakeBoldText = true
        }
        file.parentFile?.mkdirs()
        canvas.drawText(spec.label, 130f, 165f, paint)
        FileOutputStream(file).use { outputStream ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        }
        bmp.recycle()
        imageUri.toString()
    }
}

private fun buildTestVCard(context: Context): String {
    val vCardUri = buildSeedScratchUri(
        fileId = SEED_VCARD_FILE_ID,
        fileExtension = "vcf",
    )
    val file = MediaScratchFileProvider.getFileFromUri(vCardUri)
    file.parentFile?.mkdirs()
    file.writeText(
        """
        BEGIN:VCARD
        VERSION:3.0
        FN:Sam Rivera
        N:Rivera;Sam;;;
        TEL;TYPE=CELL:+15550001111
        EMAIL:sam.rivera@example.com
        END:VCARD
        """.trimIndent(),
    )

    MediaScratchFileProvider.addUriToDisplayNameEntry(vCardUri, "Sam Rivera")
    return vCardUri.toString()
}

private fun buildTestVideo(context: Context): String {
    val videoUri = buildSeedScratchUri(
        fileId = SEED_VIDEO_FILE_ID,
        fileExtension = "mp4",
    )
    val file = MediaScratchFileProvider.getFileFromUri(videoUri)
    file.parentFile?.mkdirs()
    context.assets.open("seed_video.mp4").use { inputStream ->
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    MediaScratchFileProvider.addUriToDisplayNameEntry(videoUri, "seed_video.mp4")
    return videoUri.toString()
}

private fun buildSeedScratchUri(
    fileId: String,
    fileExtension: String? = null,
): Uri {
    val uriBuilder = MediaScratchFileProvider.getUriBuilder()
        .appendPath(fileId)
    if (!fileExtension.isNullOrBlank()) {
        uriBuilder.appendQueryParameter(
            MEDIA_SCRATCH_FILE_EXTENSION_QUERY_PARAMETER,
            fileExtension,
        )
    }
    return uriBuilder.build()
}

private fun deleteSeedScratchFile(
    fileId: String,
    fileExtension: String? = null,
) {
    val seedScratchUri = buildSeedScratchUri(
        fileId = fileId,
        fileExtension = fileExtension,
    )
    MediaScratchFileProvider.getFileFromUri(seedScratchUri).delete()
}

private fun deleteSeededAttachmentScratchFiles(
    attachmentUris: Set<String>,
) {
    attachmentUris.forEach { attachmentUri ->
        val uri = attachmentUri.toUri()
        if (!MediaScratchFileProvider.isMediaScratchSpaceUri(uri)) {
            return@forEach
        }

        MediaScratchFileProvider.getFileFromUri(uri).delete()
    }
}

private data class SeedImageSpec(
    val fileId: String,
    val fileExtension: String,
    val backgroundColor: Int,
    val label: String,
)

private fun findSelfParticipantId(db: DatabaseWrapper): String? = db.query(
    DatabaseHelper.PARTICIPANTS_TABLE,
    arrayOf(ParticipantColumns._ID),
    "${ParticipantColumns.SUB_ID} != ?",
    arrayOf(ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
    null,
    null,
    "${ParticipantColumns._ID} ASC",
    "1",
)?.use { cursor ->
    if (cursor.moveToFirst()) cursor.getString(0) else null
}

private fun upsertParticipant(
    db: DatabaseWrapper,
    phone: String,
    fullName: String,
    firstName: String,
): String {
    db.insertWithOnConflict(
        DatabaseHelper.PARTICIPANTS_TABLE,
        null,
        ContentValues().apply {
            put(ParticipantColumns.SUB_ID, ParticipantData.OTHER_THAN_SELF_SUB_ID)
            put(ParticipantColumns.NORMALIZED_DESTINATION, phone)
            put(ParticipantColumns.SEND_DESTINATION, phone)
            put(ParticipantColumns.DISPLAY_DESTINATION, phone)
            put(ParticipantColumns.FULL_NAME, fullName)
            put(ParticipantColumns.FIRST_NAME, firstName)
        },
        SQLiteDatabase.CONFLICT_IGNORE,
    )

    return db
        .query(
            DatabaseHelper.PARTICIPANTS_TABLE,
            arrayOf(ParticipantColumns._ID),
            "${ParticipantColumns.NORMALIZED_DESTINATION} = ? AND ${ParticipantColumns.SUB_ID} = ?",
            arrayOf(phone, ParticipantData.OTHER_THAN_SELF_SUB_ID.toString()),
            null,
            null,
            null,
        )
        ?.use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }
        .let(::requireNotNull)
}

private fun createConversation(
    db: DatabaseWrapper,
    name: String,
    selfId: String,
    participantIds: List<String>,
    sortTimestamp: Long,
    previewUri: String? = null,
    previewContentType: String? = null,
): Long {
    val conversationId = db.insert(
        DatabaseHelper.CONVERSATIONS_TABLE,
        null,
        ContentValues().apply {
            put(ConversationColumns.NAME, name)
            put(ConversationColumns.CURRENT_SELF_ID, selfId)
            put(ConversationColumns.PARTICIPANT_COUNT, participantIds.size)
            put(ConversationColumns.SORT_TIMESTAMP, sortTimestamp)
            put(ConversationColumns.ARCHIVE_STATUS, 0)
            put(ConversationColumns.NOTIFICATION_ENABLED, 1)
            put(ConversationColumns.NOTIFICATION_VIBRATION, 1)
            if (previewUri != null) put(ConversationColumns.PREVIEW_URI, previewUri)
            if (previewContentType !=
                null
            ) {
                put(ConversationColumns.PREVIEW_CONTENT_TYPE, previewContentType)
            }
        },
    )
    for (participantId in participantIds) {
        db.insert(
            DatabaseHelper.CONVERSATION_PARTICIPANTS_TABLE,
            null,
            ContentValues().apply {
                put(ConversationParticipantsColumns.CONVERSATION_ID, conversationId)
                put(ConversationParticipantsColumns.PARTICIPANT_ID, participantId)
            },
        )
    }
    return conversationId
}

private fun insertTextMessage(
    db: DatabaseWrapper,
    conversationId: Long,
    senderId: String,
    selfId: String,
    text: String,
    status: Int,
    protocol: Int,
    timestamp: Long,
    seen: Boolean = true,
    read: Boolean = true,
    mmsSubject: String? = null,
): Long {
    val messageId = insertMessageRow(
        db, conversationId, senderId, selfId,
        status, protocol, timestamp, seen, read, mmsSubject,
    )
    db.insert(
        DatabaseHelper.PARTS_TABLE,
        null,
        ContentValues().apply {
            put(PartColumns.MESSAGE_ID, messageId)
            put(PartColumns.CONVERSATION_ID, conversationId)
            put(PartColumns.TEXT, text)
            put(PartColumns.CONTENT_TYPE, ContentType.TEXT_PLAIN)
        },
    )
    return messageId
}

private fun insertImageMessage(
    db: DatabaseWrapper,
    conversationId: Long,
    senderId: String,
    selfId: String,
    imageUri: String,
    status: Int,
    timestamp: Long,
    seen: Boolean = true,
    read: Boolean = true,
): Long {
    return insertAttachmentMessage(
        db = db,
        conversationId = conversationId,
        senderId = senderId,
        selfId = selfId,
        contentType = ContentType.IMAGE_JPEG,
        attachmentUri = imageUri,
        status = status,
        timestamp = timestamp,
        width = 400,
        height = 300,
        seen = seen,
        read = read,
    )
}

private fun insertMixedMessage(
    db: DatabaseWrapper,
    conversationId: Long,
    senderId: String,
    selfId: String,
    text: String,
    imageUri: String,
    status: Int,
    timestamp: Long,
    seen: Boolean = true,
    read: Boolean = true,
): Long {
    val messageId = insertAttachmentMessage(
        db = db,
        conversationId = conversationId,
        senderId = senderId,
        selfId = selfId,
        contentType = ContentType.IMAGE_JPEG,
        attachmentUri = imageUri,
        status = status,
        timestamp = timestamp,
        width = 400,
        height = 300,
        seen = seen,
        read = read,
    )
    db.insert(
        DatabaseHelper.PARTS_TABLE,
        null,
        ContentValues().apply {
            put(PartColumns.MESSAGE_ID, messageId)
            put(PartColumns.CONVERSATION_ID, conversationId)
            put(PartColumns.TEXT, text)
            put(PartColumns.CONTENT_TYPE, ContentType.TEXT_PLAIN)
        },
    )
    return messageId
}

private fun insertAttachmentMessage(
    db: DatabaseWrapper,
    conversationId: Long,
    senderId: String,
    selfId: String,
    contentType: String,
    attachmentUri: String,
    status: Int,
    timestamp: Long,
    width: Int = 0,
    height: Int = 0,
    seen: Boolean = true,
    read: Boolean = true,
): Long {
    val messageId = insertMessageRow(
        db = db,
        conversationId = conversationId,
        senderId = senderId,
        selfId = selfId,
        status = status,
        protocol = MessageData.PROTOCOL_MMS,
        timestamp = timestamp,
        seen = seen,
        read = read,
        mmsSubject = null,
    )
    db.insert(
        DatabaseHelper.PARTS_TABLE,
        null,
        ContentValues().apply {
            put(PartColumns.MESSAGE_ID, messageId)
            put(PartColumns.CONVERSATION_ID, conversationId)
            put(PartColumns.CONTENT_TYPE, contentType)
            put(PartColumns.CONTENT_URI, attachmentUri)
            put(PartColumns.WIDTH, width)
            put(PartColumns.HEIGHT, height)
        },
    )
    return messageId
}

private fun insertVCardMessage(
    db: DatabaseWrapper,
    conversationId: Long,
    senderId: String,
    selfId: String,
    vCardUri: String,
    status: Int,
    timestamp: Long,
    seen: Boolean = true,
    read: Boolean = true,
): Long {
    return insertAttachmentMessage(
        db = db,
        conversationId = conversationId,
        senderId = senderId,
        selfId = selfId,
        contentType = ContentType.TEXT_VCARD,
        attachmentUri = vCardUri,
        status = status,
        timestamp = timestamp,
        seen = seen,
        read = read,
    )
}

private fun insertVideoMessage(
    db: DatabaseWrapper,
    conversationId: Long,
    senderId: String,
    selfId: String,
    videoUri: String,
    status: Int,
    timestamp: Long,
    seen: Boolean = true,
    read: Boolean = true,
): Long {
    return insertAttachmentMessage(
        db = db,
        conversationId = conversationId,
        senderId = senderId,
        selfId = selfId,
        contentType = ContentType.VIDEO_MP4,
        attachmentUri = videoUri,
        status = status,
        timestamp = timestamp,
        width = 400,
        height = 300,
        seen = seen,
        read = read,
    )
}

private fun insertMessageRow(
    db: DatabaseWrapper,
    conversationId: Long,
    senderId: String,
    selfId: String,
    status: Int,
    protocol: Int,
    timestamp: Long,
    seen: Boolean,
    read: Boolean,
    mmsSubject: String?,
): Long = db.insert(
    DatabaseHelper.MESSAGES_TABLE,
    null,
    ContentValues().apply {
        put(MessageColumns.CONVERSATION_ID, conversationId)
        put(MessageColumns.SENDER_PARTICIPANT_ID, senderId)
        put(MessageColumns.SELF_PARTICIPANT_ID, selfId)
        put(MessageColumns.STATUS, status)
        put(MessageColumns.PROTOCOL, protocol)
        put(MessageColumns.SENT_TIMESTAMP, timestamp)
        put(MessageColumns.RECEIVED_TIMESTAMP, timestamp)
        put(MessageColumns.SEEN, if (seen) 1 else 0)
        put(MessageColumns.READ, if (read) 1 else 0)
        if (mmsSubject != null) put(MessageColumns.MMS_SUBJECT, mmsSubject)
    },
)

private fun finalizeConversation(
    db: DatabaseWrapper,
    conversationId: Long,
    latestMessageId: Long,
    latestTimestamp: Long,
    snippetText: String,
    previewUri: String? = null,
    previewContentType: String? = null,
) {
    db.update(
        DatabaseHelper.CONVERSATIONS_TABLE,
        ContentValues().apply {
            put(ConversationColumns.LATEST_MESSAGE_ID, latestMessageId)
            put(ConversationColumns.SORT_TIMESTAMP, latestTimestamp)
            put(ConversationColumns.SNIPPET_TEXT, snippetText)
            if (previewUri != null) put(ConversationColumns.PREVIEW_URI, previewUri)
            if (previewContentType !=
                null
            ) {
                put(ConversationColumns.PREVIEW_CONTENT_TYPE, previewContentType)
            }
        },
        "${ConversationColumns._ID} = ?",
        arrayOf(conversationId.toString()),
    )
}

/**
 * 1:1 SMS thread with Alice, 40 messages.
 */
private fun seedScenarioA(db: DatabaseWrapper, selfId: String, aliceId: String, now: Long) {
    val baseTime = now - 4 * DAYS
    val convId = createConversation(db, "Alice Wonderland", selfId, listOf(aliceId), baseTime)

    val texts = listOf(
        "Hey, are you free this weekend?",
        "I was thinking we could grab coffee",
        "There's a new place downtown that opened last week",
        "Yeah sounds good! What time works for you?",
        "How about 10am Saturday?",
        "Perfect, see you then",
        "Can you also bring the book you mentioned?",
        "Sure, I'll bring it",
        "Did you see the game last night?",
        "No I missed it, who won?",
        "It went to overtime, crazy finish",
        "I'll have to watch the highlights",
        "Just got back from the gym",
        "Nice, how was it?",
        "Pretty good, trying the new routine",
        "Let me know how it goes",
        "Will do",
        "See you Saturday!",
        "Looking forward to it",
        "Don't forget to bring the book",
    )

    var latestMsgId = 0L
    var latestTime = baseTime
    for (i in 0 until 40) {
        // 3-message clusters 2 min apart; 38 min gap between clusters
        val clusterIndex = i / 3
        val withinCluster = i % 3
        val msgTime = baseTime + clusterIndex * 38 * MINUTES + withinCluster * 2 * MINUTES
        val isIncoming = withinCluster != 1 // pattern: in, out, in, in, out, in, ...
        val senderId = if (isIncoming) aliceId else selfId
        val status = if (isIncoming) {
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE
        } else {
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
        }
        latestMsgId = insertTextMessage(
            db,
            convId,
            senderId,
            selfId,
            texts[i % texts.size],
            status,
            MessageData.PROTOCOL_SMS,
            msgTime,
        )
        latestTime = msgTime
    }

    finalizeConversation(db, convId, latestMsgId, latestTime, "Don't forget to bring the book")
}

/**
 * 1:1 SMS thread with Bob containing a failed message and one retrying.
 */
private fun seedScenarioB(db: DatabaseWrapper, selfId: String, bobId: String, now: Long) {
    val baseTime = now - 2 * DAYS
    val convId = createConversation(db, "Bob Baker", selfId, listOf(bobId), baseTime)

    // (text, isIncoming, status)
    val messages = listOf(
        Triple("Did you get the report?", false, MessageData.BUGLE_STATUS_OUTGOING_COMPLETE),
        Triple("Yeah got it, looks good", true, MessageData.BUGLE_STATUS_INCOMING_COMPLETE),
        Triple(
            "Can you send the updated version?",
            false,
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE,
        ),
        Triple("Sure, give me a minute", true, MessageData.BUGLE_STATUS_INCOMING_COMPLETE),
        Triple("Here you go", true, MessageData.BUGLE_STATUS_INCOMING_COMPLETE),
        Triple("Thanks! One more thing...", false, MessageData.BUGLE_STATUS_OUTGOING_COMPLETE),
        Triple("What is it?", true, MessageData.BUGLE_STATUS_INCOMING_COMPLETE),
        Triple("Can we meet tomorrow at 3pm?", false, MessageData.BUGLE_STATUS_OUTGOING_FAILED),
        Triple("OK let me check", true, MessageData.BUGLE_STATUS_INCOMING_COMPLETE),
        Triple("3pm works for me", true, MessageData.BUGLE_STATUS_INCOMING_COMPLETE),
        Triple("Can we meet tomorrow at 3pm?", false, MessageData.BUGLE_STATUS_OUTGOING_COMPLETE),
        Triple("Great, see you then", true, MessageData.BUGLE_STATUS_INCOMING_COMPLETE),
        Triple("Perfect", false, MessageData.BUGLE_STATUS_OUTGOING_COMPLETE),
        Triple("Don't be late!", true, MessageData.BUGLE_STATUS_INCOMING_COMPLETE),
        Triple("Never", false, MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY),
    )

    var latestMsgId = 0L
    var latestTime = baseTime
    for ((idx, m) in messages.withIndex()) {
        val (text, isIncoming, status) = m
        val msgTime = baseTime + idx * 8 * MINUTES
        val senderId = if (isIncoming) bobId else selfId
        latestMsgId = insertTextMessage(
            db,
            convId,
            senderId,
            selfId,
            text,
            status,
            MessageData.PROTOCOL_SMS,
            msgTime,
        )
        latestTime = msgTime
    }

    finalizeConversation(db, convId, latestMsgId, latestTime, "Never")
}

/**
 * Group MMS thread "Team Chat" with Carol, Dave, Eve (30 messages)
 */
private fun seedScenarioC(
    db: DatabaseWrapper,
    selfId: String,
    carolId: String,
    daveId: String,
    eveId: String,
    now: Long,
) {
    val baseTime = now - 3 * DAYS
    val convId = createConversation(
        db,
        "Team Chat",
        selfId,
        listOf(carolId, daveId, eveId),
        baseTime,
    )

    val senders = listOf(carolId, daveId, eveId, selfId)
    val texts = listOf(
        "Hey everyone!", "Hi there", "Hello!", "What time are we meeting?",
        "How about 2pm?", "Works for me", "Can't do 2pm, how about 3?", "3pm is fine",
        "Let's do 3pm then", "Should we bring anything?", "Just yourselves", "I'll bring snacks",
        "Nice!", "See you all tomorrow", "Can't wait", "It's going to be great",
        "Agreed", "Anyone need a ride?", "I'm good, thanks", "I could use one actually",
        "I got you Carol", "Thanks Dave!", "Ok see everyone at 3", "See you there!",
        "Don't forget it's at the usual place", "Got it", "See you all soon!",
        "This is going to be fun", "Definitely", "On my way!",
    )

    var latestMsgId = 0L
    var latestTime = baseTime
    for (i in texts.indices) {
        val sender = senders[i % senders.size]
        val status = if (sender == selfId) {
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
        } else {
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE
        }
        val msgTime = baseTime + i * 5 * MINUTES
        latestMsgId = insertTextMessage(
            db,
            convId,
            sender,
            selfId,
            texts[i],
            status,
            MessageData.PROTOCOL_MMS,
            msgTime,
        )
        latestTime = msgTime
    }

    finalizeConversation(db, convId, latestMsgId, latestTime, "On my way!")
}

/**
 * MMS thread with Frank where every message has a subject line
 */
private fun seedScenarioD(db: DatabaseWrapper, selfId: String, frankId: String, now: Long) {
    val baseTime = now - 5 * DAYS
    val convId = createConversation(db, "Frank Ford", selfId, listOf(frankId), baseTime)

    val messages = listOf(
        Pair("Are you still up for hiking Saturday?", false),
        Pair("Yes! Super excited", true),
        Pair("Great, let's meet at the trailhead at 8am", false),
        Pair("Works for me. Which trail?", true),
        Pair("The ridge trail, it has the best views", false),
        Pair("Oh I love that trail!", true),
        Pair("Bring sunscreen, it'll be hot", false),
        Pair("Good call", true),
        Pair("See you Saturday!", false),
        Pair("Can't wait!", true),
    )

    var latestMsgId = 0L
    var latestTime = baseTime
    for ((idx, m) in messages.withIndex()) {
        val (text, isIncoming) = m
        val msgTime = baseTime + idx * 10 * MINUTES
        val senderId = if (isIncoming) frankId else selfId
        val status = if (isIncoming) {
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE
        } else {
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
        }
        latestMsgId = insertTextMessage(
            db, convId, senderId, selfId,
            text, status, MessageData.PROTOCOL_MMS, msgTime, mmsSubject = "Weekend plans",
        )
        latestTime = msgTime
    }

    finalizeConversation(db, convId, latestMsgId, latestTime, "Can't wait!")
}

/**
 * Long thread with Grace, 300 messages, last 5 unread
 */
private fun seedScenarioE(db: DatabaseWrapper, selfId: String, graceId: String, now: Long) {
    val totalMessages = 300
    val baseTime = now - 10 * HOURS
    val unreadStartIndex = totalMessages - 5
    val convId = createConversation(db, "Grace Green", selfId, listOf(graceId), baseTime)

    var latestMsgId = 0L
    var latestText = ""
    for (i in 0 until totalMessages) {
        val msgTime = baseTime + i * 2 * MINUTES
        val isIncoming = i % 2 == 0
        val senderId = if (isIncoming) graceId else selfId
        val status = if (isIncoming) {
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE
        } else {
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
        }
        val isUnread = i >= unreadStartIndex
        latestText = "Message ${i + 1} — scroll performance test"
        latestMsgId = insertTextMessage(
            db, convId, senderId, selfId,
            latestText, status, MessageData.PROTOCOL_SMS, msgTime,
            seen = !isUnread, read = !isUnread,
        )
    }

    finalizeConversation(
        db,
        convId,
        latestMsgId,
        baseTime + totalMessages * 2 * MINUTES,
        latestText,
    )
}

/**
 * All-unread conversation with Henry, 5 incoming messages
 */
private fun seedScenarioF(db: DatabaseWrapper, selfId: String, henryId: String, now: Long) {
    val baseTime = now - 30 * MINUTES
    val convId = createConversation(db, "Henry Hall", selfId, listOf(henryId), baseTime)

    val texts = listOf(
        "Hey, are you around?",
        "I need to show you something",
        "It's important",
        "Please reply when you get a chance",
        "I'll be online for the next hour",
    )

    var latestMsgId = 0L
    var latestTime = baseTime
    for ((idx, text) in texts.withIndex()) {
        val msgTime = baseTime + idx * 5 * MINUTES
        latestMsgId = insertTextMessage(
            db, convId, henryId, selfId, text,
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE, MessageData.PROTOCOL_SMS, msgTime,
            seen = false, read = false,
        )
        latestTime = msgTime
    }

    finalizeConversation(db, convId, latestMsgId, latestTime, texts.last())
}

/**
 * 1:1 MMS thread with Iris containing image-only, text+image, and text-only messages
 */
private fun seedScenarioG(
    db: DatabaseWrapper,
    selfId: String,
    irisId: String,
    images: List<String>,
    now: Long,
) {
    val img1 = images[0]
    val img2 = images[1]
    val img3 = images[2]
    val baseTime = now - 1 * DAYS

    val convId = createConversation(
        db,
        "Iris Ingram",
        selfId,
        listOf(irisId),
        baseTime,
        previewUri = img1,
        previewContentType = ContentType.IMAGE_JPEG,
    )

    data class Msg(
        val type: String,
        val text: String = "",
        val imageUri: String = "",
        val isIncoming: Boolean,
    )

    val messages = listOf(
        Msg("text", text = "Hey! Check out what I found", isIncoming = true),
        Msg("image", imageUri = img1, isIncoming = true),
        Msg("text", text = "Wow that looks amazing!", isIncoming = false),
        Msg("text", text = "Where was this taken?", isIncoming = false),
        Msg("text", text = "At the botanical garden last weekend", isIncoming = true),
        Msg(
            "mixed",
            text = "Here's another one from the same day",
            imageUri = img2,
            isIncoming = true,
        ),
        Msg("text", text = "These are stunning", isIncoming = false),
        Msg("image", imageUri = img3, isIncoming = false),
        Msg("text", text = "I took that one on the way home", isIncoming = false),
        Msg("text", text = "You have such a good eye for photos!", isIncoming = true),
        Msg("text", text = "Thanks! We should go together sometime", isIncoming = false),
        Msg("text", text = "Definitely, let me know when you're free", isIncoming = true),
        Msg("image", imageUri = img2, isIncoming = true),
        Msg("text", text = "And one more from yesterday", isIncoming = true),
        Msg(
            "mixed",
            text = "Shot this from my window this morning",
            imageUri = img1,
            isIncoming = false,
        ),
    )

    var latestMsgId = 0L
    var latestTime = baseTime
    for ((idx, m) in messages.withIndex()) {
        val msgTime = baseTime + idx * 12 * MINUTES
        val senderId = if (m.isIncoming) irisId else selfId
        val status = if (m.isIncoming) {
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE
        } else {
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
        }
        latestMsgId = when (m.type) {
            "image" -> insertImageMessage(
                db,
                convId,
                senderId,
                selfId,
                m.imageUri,
                status,
                msgTime,
            )

            "mixed" -> insertMixedMessage(
                db,
                convId,
                senderId,
                selfId,
                m.text,
                m.imageUri,
                status,
                msgTime,
            )

            else -> insertTextMessage(
                db,
                convId,
                senderId,
                selfId,
                m.text,
                status,
                MessageData.PROTOCOL_MMS,
                msgTime,
            )
        }
        latestTime = msgTime
    }

    finalizeConversation(
        db,
        convId,
        latestMsgId,
        latestTime,
        "Shot this from my window this morning",
        previewUri = img1,
        previewContentType = ContentType.IMAGE_JPEG,
    )
}

/**
 * Group MMS thread "Photo Dump" with Jack and Carol containing image bursts
 */
private fun seedScenarioH(
    db: DatabaseWrapper,
    selfId: String,
    jackId: String,
    carolId: String,
    images: List<String>,
    videoUri: String,
    vCardUri: String,
    now: Long,
) {
    val img1 = images[0]
    val img2 = images[1]
    val img3 = images[2]
    val baseTime = now - 6 * HOURS

    val convId = createConversation(
        db,
        "Photo Dump",
        selfId,
        listOf(jackId, carolId),
        baseTime,
        previewUri = img2,
        previewContentType = ContentType.IMAGE_JPEG,
    )

    data class Msg(
        val type: String,
        val text: String = "",
        val attachmentUri: String = "",
        val senderId: String,
    )

    val messages = listOf(
        Msg("text", text = "Dropping some pics from last night", senderId = jackId),
        Msg("image", attachmentUri = img1, senderId = jackId),
        Msg("image", attachmentUri = img3, senderId = jackId),
        Msg("text", text = "The lighting was perfect", senderId = jackId),
        Msg("text", text = "These are great Jack!", senderId = carolId),
        Msg("image", attachmentUri = img2, senderId = carolId),
        Msg("text", text = "I got a few too", senderId = carolId),
        Msg("text", text = "Love that shot Carol", senderId = selfId),
        Msg(
            "mixed",
            text = "Here's mine from the same spot",
            attachmentUri = img3,
            senderId = selfId,
        ),
        Msg("text", text = "We all had the same idea haha", senderId = jackId),
        Msg("image", attachmentUri = img1, senderId = carolId),
        Msg("text", text = TEST_YOUTUBE_VIDEO_URL, senderId = carolId),
        Msg("text", text = "The clip version is even better", senderId = jackId),
        Msg("video", attachmentUri = videoUri, senderId = carolId),
        Msg("text", text = "Send me the photographer contact too", senderId = selfId),
        Msg("vcard", attachmentUri = vCardUri, senderId = carolId),
        Msg("text", text = "One more", senderId = carolId),
        Msg("text", text = "We need to do this again soon", senderId = selfId),
        Msg("text", text = "+1", senderId = jackId),
        Msg("text", text = "Same time next week?", senderId = carolId),
    )

    var latestMsgId = 0L
    var latestTime = baseTime
    for ((idx, m) in messages.withIndex()) {
        val msgTime = baseTime + idx * 7 * MINUTES
        val status = if (m.senderId == selfId) {
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
        } else {
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE
        }
        latestMsgId = when (m.type) {
            "image" -> insertImageMessage(
                db,
                convId,
                m.senderId,
                selfId,
                m.attachmentUri,
                status,
                msgTime,
            )

            "mixed" -> insertMixedMessage(
                db,
                convId,
                m.senderId,
                selfId,
                m.text,
                m.attachmentUri,
                status,
                msgTime,
            )

            "vcard" -> insertVCardMessage(
                db = db,
                conversationId = convId,
                senderId = m.senderId,
                selfId = selfId,
                vCardUri = m.attachmentUri,
                status = status,
                timestamp = msgTime,
            )

            "video" -> insertVideoMessage(
                db = db,
                conversationId = convId,
                senderId = m.senderId,
                selfId = selfId,
                videoUri = m.attachmentUri,
                status = status,
                timestamp = msgTime,
            )

            else -> insertTextMessage(
                db,
                convId,
                m.senderId,
                selfId,
                m.text,
                status,
                MessageData.PROTOCOL_MMS,
                msgTime,
            )
        }
        latestTime = msgTime
    }

    finalizeConversation(
        db,
        convId,
        latestMsgId,
        latestTime,
        "Same time next week?",
        previewUri = img2,
        previewContentType = ContentType.IMAGE_JPEG,
    )
}

/**
 * Group MMS thread containing explicit clustering and non-clustering cases
 */
private fun seedScenarioI(
    db: DatabaseWrapper,
    selfId: String,
    carolId: String,
    daveId: String,
    eveId: String,
    now: Long,
) {
    val baseTime = now - 20 * MINUTES
    val conversationId = createConversation(
        db = db,
        name = "Clustering Test Cases",
        selfId = selfId,
        participantIds = listOf(carolId, daveId, eveId),
        sortTimestamp = baseTime,
    )

    data class ClusterTestMessage(
        val text: String,
        val senderId: String,
        val offsetMillis: Long,
    )

    val messages = listOf(
        ClusterTestMessage(
            text = "Standalone incoming",
            senderId = carolId,
            offsetMillis = 0L,
        ),
        ClusterTestMessage(
            text = "Pair top",
            senderId = carolId,
            offsetMillis = 2 * MINUTES,
        ),
        ClusterTestMessage(
            text = "Pair bottom",
            senderId = carolId,
            offsetMillis = 2 * MINUTES + 30_000L,
        ),
        ClusterTestMessage(
            text = "Triplet top",
            senderId = daveId,
            offsetMillis = 5 * MINUTES,
        ),
        ClusterTestMessage(
            text = "Triplet middle",
            senderId = daveId,
            offsetMillis = 5 * MINUTES + 20_000L,
        ),
        ClusterTestMessage(
            text = "Triplet bottom",
            senderId = daveId,
            offsetMillis = 5 * MINUTES + 40_000L,
        ),
        ClusterTestMessage(
            text = "Quartet top",
            senderId = eveId,
            offsetMillis = 8 * MINUTES,
        ),
        ClusterTestMessage(
            text = "Quartet middle 1",
            senderId = eveId,
            offsetMillis = 8 * MINUTES + 20_000L,
        ),
        ClusterTestMessage(
            text = "Quartet middle 2",
            senderId = eveId,
            offsetMillis = 8 * MINUTES + 40_000L,
        ),
        ClusterTestMessage(
            text = "Quartet bottom",
            senderId = eveId,
            offsetMillis = 9 * MINUTES,
        ),
        ClusterTestMessage(
            text = "Same sender after gap",
            senderId = daveId,
            offsetMillis = 12 * MINUTES,
        ),
        ClusterTestMessage(
            text = "Gap break still standalone",
            senderId = daveId,
            offsetMillis = 13 * MINUTES + 40_000L,
        ),
        ClusterTestMessage(
            text = "Different sender break",
            senderId = carolId,
            offsetMillis = 16 * MINUTES,
        ),
        ClusterTestMessage(
            text = "Outgoing standalone",
            senderId = selfId,
            offsetMillis = 16 * MINUTES + 20_000L,
        ),
        ClusterTestMessage(
            text = "Outgoing pair top",
            senderId = selfId,
            offsetMillis = 19 * MINUTES,
        ),
        ClusterTestMessage(
            text = "Outgoing pair bottom",
            senderId = selfId,
            offsetMillis = 19 * MINUTES + 20_000L,
        ),
    )

    var latestMessageId = 0L
    var latestTimestamp = baseTime
    var latestText = ""
    for (message in messages) {
        val timestamp = baseTime + message.offsetMillis
        val isIncoming = message.senderId != selfId
        val status = if (isIncoming) {
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE
        } else {
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
        }

        latestText = message.text
        latestMessageId = insertTextMessage(
            db = db,
            conversationId = conversationId,
            senderId = message.senderId,
            selfId = selfId,
            text = message.text,
            status = status,
            protocol = MessageData.PROTOCOL_MMS,
            timestamp = timestamp,
        )
        latestTimestamp = timestamp
    }

    finalizeConversation(
        db = db,
        conversationId = conversationId,
        latestMessageId = latestMessageId,
        latestTimestamp = latestTimestamp,
        snippetText = latestText,
    )
}
