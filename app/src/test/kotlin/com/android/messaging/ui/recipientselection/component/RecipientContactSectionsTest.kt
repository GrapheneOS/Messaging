package com.android.messaging.ui.recipientselection.component

import com.android.messaging.ui.recipientselection.component.row.contactItem
import com.android.messaging.ui.recipientselection.model.section.RecipientContactListEntry
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecipientContactSectionsTest {

    @Test
    fun sectionHeadersFollowAlphabetOrderWithNonLettersLast() {
        val displayNames = listOf(
            "123 Taxi",
            "Zoe",
            "Émile",
            "Bob",
            "تینا",
            "پرویز",
            "بهرام",
            "گلاره",
            "کامران",
            "لیلا",
        )
        val items = displayNames
            .mapIndexed { index, displayName ->
                contactItem(id = index + 1L, displayName = displayName)
            }
            .toImmutableList()

        val entries = recipientContactListEntries(
            items = items,
            showSectionHeaders = true,
        )

        val headers = entries
            .filterIsInstance<RecipientContactListEntry.Header>()
            .map { header -> header.label }
        Assert.assertEquals(
            listOf("B", "É", "Z", "ب", "پ", "ت", "ک", "گ", "ل", "#"),
            headers,
        )
    }
}
