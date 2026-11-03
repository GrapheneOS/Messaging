package com.android.messaging.ui.recipientselection.component

import android.icu.text.Collator
import com.android.messaging.ui.recipientselection.model.picker.RecipientPickerListItem
import com.android.messaging.ui.recipientselection.model.section.RecipientContactListEntry
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val NON_LETTER_SECTION_LABEL = "#"

internal fun recipientContactListEntries(
    items: ImmutableList<RecipientPickerListItem>,
    showSectionHeaders: Boolean,
): ImmutableList<RecipientContactListEntry> {
    if (!showSectionHeaders) {
        return items
            .mapIndexed { index, item ->
                RecipientContactListEntry.Row(
                    item = item,
                    positionInSection = index,
                    sectionSize = items.size,
                )
            }
            .toImmutableList()
    }

    val sections = LinkedHashMap<String, MutableList<RecipientPickerListItem>>()
    items.forEach { item ->
        val label = recipientContactSectionLabel(item)
        sections.getOrPut(label) { mutableListOf() }.add(item)
    }

    val entries = persistentListOf<RecipientContactListEntry>().builder()
    sections.keys
        .sortedWith(sectionLabelComparator())
        .forEach { label ->
            val sectionItems = sections.getValue(label)
            entries.add(RecipientContactListEntry.Header(label))
            sectionItems.forEachIndexed { position, item ->
                entries.add(
                    RecipientContactListEntry.Row(
                        item = item,
                        positionInSection = position,
                        sectionSize = sectionItems.size,
                    ),
                )
            }
        }

    return entries.build()
}

internal fun recipientContactSectionLabel(item: RecipientPickerListItem): String {
    val source = when (item) {
        is RecipientPickerListItem.Contact -> item.contact.displayName
        is RecipientPickerListItem.SyntheticPhone -> item.displayName
    }

    val firstCharacter = source.trim()
        .firstOrNull()
        ?.uppercaseChar()

    return when {
        firstCharacter?.isLetter() == true -> firstCharacter.toString()
        else -> NON_LETTER_SECTION_LABEL
    }
}

private fun sectionLabelComparator(): Comparator<String> {
    return compareBy<String> { label -> label == NON_LETTER_SECTION_LABEL }
        .then(Collator.getInstance())
}
