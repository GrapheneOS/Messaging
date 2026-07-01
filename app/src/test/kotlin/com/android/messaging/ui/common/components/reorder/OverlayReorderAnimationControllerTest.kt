package com.android.messaging.ui.common.components.reorder

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayReorderAnimationControllerTest {

    private val controller = OverlayReorderAnimationController<String, String>(
        key = { it },
        isSettled = { _, _ -> true },
    )

    @Test
    fun prepare_withKnownItemAndBounds_createsHiddenPendingAnimation() {
        seedItem("a")

        controller.prepare(
            keys = listOf("a"),
            anchorToTop = true,
            transform = { it },
        )

        val animation = controller.animations.single()
        assertEquals("a", animation.key)
        assertFalse(animation.isCommitted)
        assertFalse(animation.isStarted)
        assertTrue(controller.isItemHidden("a"))
    }

    @Test
    fun startAnimation_onlySucceedsOnceAndAfterCommit() {
        seedItem("a")
        controller.prepare(
            keys = listOf("a"),
            anchorToTop = true,
            transform = { it },
        )

        val animationId = controller.animations.single().animationId
        assertNull(controller.startAnimation(animationId))

        controller.markCommitted()

        assertEquals(true, controller.startAnimation(animationId)?.isStarted)
        assertNull(controller.startAnimation(animationId))
    }

    @Test
    fun finish_removesAnimationAndUnhidesItem() {
        seedItem("a")
        controller.prepare(
            keys = listOf("a"),
            anchorToTop = true,
            transform = { it },
        )

        val animationId = controller.animations.single().animationId
        controller.finish(animationId)

        assertTrue(controller.animations.isEmpty())
        assertFalse(controller.isItemHidden("a"))
    }

    private fun seedItem(itemKey: String) {
        controller.updateItems(listOf(itemKey))
        controller.updateContainerBounds(Rect(left = 0f, top = 0f, right = 100f, bottom = 1_000f))
        controller.updateItemBounds(
            itemKey = itemKey,
            boundsInRoot = Rect(left = 0f, top = 0f, right = 100f, bottom = 50f),
            isPhysicallyVisible = true,
            firstVisibleItemIndex = 0,
            lastVisibleItemIndex = 0,
        )
    }
}
