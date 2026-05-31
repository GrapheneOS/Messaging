package com.android.messaging.data.conversation.repository.conversations

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import com.android.messaging.data.conversation.repository.ConversationsRepositoryImpl
import com.android.messaging.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class BaseConversationsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    protected lateinit var contentResolver: ContentResolver

    @Before
    fun setUp() {
        contentResolver = mockk()
    }

    protected fun createRepository(): ConversationsRepositoryImpl {
        return ConversationsRepositoryImpl(
            contentResolver = contentResolver,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            messagingDbDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    protected fun stubObserverRegistration(
        registeredObservers: MutableList<ContentObserver>,
        expectedUri: Uri,
    ) {
        every {
            contentResolver.registerContentObserver(
                expectedUri,
                true,
                any(),
            )
        } answers {
            registeredObservers.add(thirdArg<ContentObserver>())
        }
        every { contentResolver.unregisterContentObserver(any()) } just runs
    }

    protected fun stubQuery(
        expectedUri: Uri,
        capturedProjections: MutableList<Array<String>?>,
        result: Cursor?,
    ) {
        every {
            contentResolver.query(
                expectedUri,
                any(),
                null,
                null,
                null,
            )
        } answers {
            capturedProjections.add(secondArg<Array<String>?>())
            result
        }
    }
}
