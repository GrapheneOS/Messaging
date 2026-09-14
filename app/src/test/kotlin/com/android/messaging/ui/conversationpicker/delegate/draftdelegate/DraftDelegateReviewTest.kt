package com.android.messaging.ui.conversationpicker.delegate.draftdelegate

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DraftDelegateReviewTest : BaseDraftDelegateTest() {

    @Test
    fun enterReview_setsReviewing() = runTest {
        val delegate = boundDelegate()

        delegate.enterReview()
        settle()

        assertTrue(delegate.state.value.isReviewing)
    }

    @Test
    fun exitReview_clearsReviewing() = runTest {
        val delegate = boundDelegate()
        delegate.enterReview()

        delegate.exitReview()
        settle()

        assertFalse(delegate.state.value.isReviewing)
    }

    @Test
    fun bind_whenSelectionIsEmpty_exitsReview() = runTest {
        val delegate = createDelegate()
        delegate.enterReview()

        delegate.bind(backgroundScope, selectedIds(emptySet()))
        settle()

        assertFalse(delegate.state.value.isReviewing)
    }

    @Test
    fun bind_whenSelectionBecomesEmpty_exitsReview() = runTest {
        val delegate = createDelegate()
        val selection = selectedIds(setOf("dest:1"))
        delegate.bind(backgroundScope, selection)
        delegate.enterReview()
        settle()

        selection.value = persistentSetOf()
        settle()

        assertFalse(delegate.state.value.isReviewing)
    }

    @Test
    fun bind_whenSelectionStaysNonEmpty_keepsReview() = runTest {
        val delegate = createDelegate()
        delegate.bind(backgroundScope, selectedIds(setOf("dest:1")))
        delegate.enterReview()
        settle()

        assertTrue(delegate.state.value.isReviewing)
    }

    @Test
    fun bind_calledTwice_ignoresSecondBinding() = runTest {
        val delegate = createDelegate()
        delegate.bind(backgroundScope, selectedIds(setOf("dest:1")))
        delegate.enterReview()
        settle()

        delegate.bind(backgroundScope, selectedIds(emptySet()))
        settle()

        assertTrue(delegate.state.value.isReviewing)
    }

    private fun selectedIds(ids: Set<String>): MutableStateFlow<ImmutableSet<String>> {
        return MutableStateFlow(persistentSetOf<String>().addingAll(ids))
    }
}
