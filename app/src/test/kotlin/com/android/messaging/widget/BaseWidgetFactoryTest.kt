package com.android.messaging.widget

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import android.widget.RemoteViews
import com.android.messaging.Factory
import com.android.messaging.FactoryTestAccess
import com.android.messaging.R
import com.android.messaging.datamodel.media.DecodedImageResource
import com.android.messaging.datamodel.media.ImageResource
import com.android.messaging.datamodel.media.MediaRequest
import com.android.messaging.datamodel.media.MediaResourceManager
import com.android.messaging.testutil.installTestFactory
import com.android.messaging.util.AvatarUriUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class BaseWidgetFactoryTest {

    private val context: Application = RuntimeEnvironment.getApplication()
    private val mediaResourceManager = mockk<MediaResourceManager>()
    private val loadedResources = mutableListOf<ImageResource>()

    @Before
    fun setUp() {
        installTestFactory(context = context)
        every { Factory.get().mediaResourceManager } returns mediaResourceManager
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns mockk(relaxed = true)
        // Like an avatar the cache has already let go of: the caller's reference is the only one,
        // so releasing it recycles the bitmap.
        every {
            mediaResourceManager.requestMediaResourceSync(any<MediaRequest<ImageResource>>())
        } answers {
            val bitmap = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(AVATAR_COLOR)
            DecodedImageResource(
                firstArg<MediaRequest<ImageResource>>().key,
                bitmap,
                ExifInterface.ORIENTATION_NORMAL,
            ).apply {
                addRef()
                loadedResources += this
            }
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
        FactoryTestAccess.reset()
    }

    /**
     * The host asks for every row before it shares their bitmaps, which is where an earlier
     * row's recycled avatar crashed the app.
     */
    @Test
    fun getAvatarBitmap_nextRowLoadsAnotherAvatar_earlierRowsAvatarCanStillBeShared() {
        val factory = TestWidgetFactory()
        val firstAvatar = requireNotNull(factory.loadAvatar(FIRST_SENDER))
        val secondAvatar = requireNotNull(factory.loadAvatar(SECOND_SENDER))
        val items = RemoteViews.RemoteCollectionItems.Builder()
            .addItem(FIRST_ROW_ID, messageRow(firstAvatar))
            .addItem(SECOND_ROW_ID, messageRow(secondAvatar))
            .build()

        RemoteViews(context.packageName, R.layout.widget_conversation)
            .setRemoteAdapter(R.id.message_list, items)

        assertFalse(firstAvatar.isRecycled)
    }

    @Test
    fun getAvatarBitmap_avatarLoaded_returnsCopyAndReleasesTheLoadedAvatar() {
        val factory = TestWidgetFactory()

        val avatar = requireNotNull(factory.loadAvatar(FIRST_SENDER))

        assertFalse(avatar.isRecycled)
        assertEquals(AVATAR_COLOR, avatar.getPixel(0, 0))
        assertEquals(0, loadedResources.single().refCount)
    }

    @Test
    fun getAvatarBitmap_avatarFailsToLoad_returnsNull() {
        every {
            mediaResourceManager.requestMediaResourceSync(any<MediaRequest<ImageResource>>())
        } returns null
        val factory = TestWidgetFactory()

        assertNull(factory.loadAvatar(FIRST_SENDER))
    }

    @Test
    fun buildItems_avatarsPassTheBitmapBudget_keepsTheNewestItemsAndPutsViewMoreLast() {
        val factory = TestWidgetFactory(senders = listOf(FIRST_SENDER, SECOND_SENDER, THIRD_SENDER))

        val items = factory.buildItems(TWO_AVATARS_BYTE_COUNT)

        assertEquals(
            listOf(
                factory.itemViews.getValue(0),
                factory.itemViews.getValue(1),
                factory.viewMoreView,
            ),
            items.views(),
        )
        assertEquals(listOf(0L, 1L, 2L), items.ids())
    }

    @Test
    fun buildItems_newestItemLast_putsViewMoreFirstAndTheNewestItemAtTheBottom() {
        val factory = TestWidgetFactory(
            senders = listOf(FIRST_SENDER, SECOND_SENDER, THIRD_SENDER),
            isNewestItemLast = true,
        )

        val items = factory.buildItems(TWO_AVATARS_BYTE_COUNT)

        assertEquals(
            listOf(
                factory.viewMoreView,
                factory.itemViews.getValue(1),
                factory.itemViews.getValue(0),
            ),
            items.views(),
        )
    }

    @Test
    fun buildItems_rowsShareASender_loadsTheirAvatarOnceAndCountsItOnce() {
        val factory = TestWidgetFactory(senders = listOf(FIRST_SENDER, FIRST_SENDER, SECOND_SENDER))

        val items = factory.buildItems(TWO_AVATARS_BYTE_COUNT)

        assertEquals(3, items.itemCount)
        assertEquals(2, loadedResources.size)
    }

    @Test
    fun buildItems_rowsPassTheParcelLimit_stopsBeforeTheRowThatPassesIt() {
        val factory = TestWidgetFactory(
            senders = listOf(FIRST_SENDER, SECOND_SENDER),
            messageLength = OVER_HALF_PARCEL_LIMIT_MESSAGE_LENGTH,
        )

        val items = factory.buildItems(Long.MAX_VALUE)

        assertEquals(listOf(factory.itemViews.getValue(0), factory.viewMoreView), items.views())
    }

    @Test
    fun buildItems_afterLoadingTheRows_closesTheCursor() {
        val factory = TestWidgetFactory(senders = listOf(FIRST_SENDER))

        factory.buildItems(Long.MAX_VALUE)

        assertTrue(factory.cursor.isClosed)
    }

    @Test
    fun buildItems_avatarLoadedForTheBuild_isReleasedAndSharedByEveryRowOfThatSender() {
        val factory = TestWidgetFactory(senders = listOf(FIRST_SENDER, FIRST_SENDER))

        factory.buildItems(Long.MAX_VALUE)

        assertSame(factory.avatars.getValue(0), factory.avatars.getValue(1))
        assertEquals(0, loadedResources.single().refCount)
    }

    private fun RemoteViews.RemoteCollectionItems.views(): List<RemoteViews> {
        return (0 until itemCount).map { position -> getItemView(position) }
    }

    private fun RemoteViews.RemoteCollectionItems.ids(): List<Long> {
        return (0 until itemCount).map { position -> getItemId(position) }
    }

    private fun messageRow(avatar: Bitmap): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_message_item_incoming).apply {
            setImageViewBitmap(R.id.avatarView, avatar)
        }
    }

    private inner class TestWidgetFactory(
        senders: List<String> = emptyList(),
        private val isNewestItemLast: Boolean = false,
        private val messageLength: Int = 0,
    ) : BaseWidgetFactory(context, Intent()) {

        val cursor = MatrixCursor(arrayOf(SENDER_COLUMN)).apply {
            senders.forEach { sender -> addRow(arrayOf(sender)) }
        }
        val itemViews = mutableMapOf<Int, RemoteViews>()
        val avatars = mutableMapOf<Int, Bitmap?>()
        val viewMoreView = RemoteViews(context.packageName, R.layout.widget_loading)

        fun loadAvatar(name: String): Bitmap? {
            return getAvatarBitmap(AvatarUriUtil.createAvatarUri(null, name, null, null))
        }

        override fun doQuery(): Cursor {
            return cursor
        }

        override fun getViewMoreItemsView(): RemoteViews {
            return viewMoreView
        }

        override fun getMainLayoutId(): Int {
            return R.layout.widget_conversation
        }

        override fun getViewAt(position: Int): RemoteViews? {
            return null
        }

        override fun getItemView(cursorPosition: Int): RemoteViews? {
            return when {
                !cursor.moveToPosition(cursorPosition) -> null
                else -> {
                    val avatar = loadAvatar(cursor.getString(0))
                    avatars[cursorPosition] = avatar
                    RemoteViews(context.packageName, R.layout.widget_message_item_incoming).apply {
                        setImageViewBitmap(R.id.avatarView, avatar)
                        setTextViewText(R.id.message, "m".repeat(messageLength))
                        itemViews[cursorPosition] = this
                    }
                }
            }
        }

        override fun isNewestItemLast(): Boolean {
            return isNewestItemLast
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 2
        }
    }

    private companion object {
        private const val AVATAR_COLOR = Color.RED
        private const val AVATAR_SIZE = 8
        private const val FIRST_ROW_ID = 1L
        private const val FIRST_SENDER = "Ada"
        private const val SECOND_ROW_ID = 2L
        private const val SECOND_SENDER = "Grace"
        private const val THIRD_SENDER = "Hedy"
        private const val SENDER_COLUMN = "sender"
        private const val TWO_AVATARS_BYTE_COUNT = 2L * AVATAR_SIZE * AVATAR_SIZE * 4

        private const val OVER_HALF_PARCEL_LIMIT_MESSAGE_LENGTH = 150_000
    }
}
