package com.android.messaging.ui.shareintent.screen.mapper

import com.android.messaging.ui.shareintent.screen.model.ShareContactSection
import com.android.messaging.ui.shareintent.screen.model.ShareTargetUiState
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal interface ShareContactSectionMapper {
    fun map(
        contacts: ImmutableList<ShareTargetUiState.Contact>,
    ): ImmutableList<ShareContactSection>
}

internal class ShareContactSectionMapperImpl @Inject constructor() : ShareContactSectionMapper {

    override fun map(
        contacts: ImmutableList<ShareTargetUiState.Contact>,
    ): ImmutableList<ShareContactSection> {
        return contacts
            .groupBy(::sectionLabel)
            .map { (label, sectionContacts) ->
                ShareContactSection(
                    label = label,
                    targets = sectionContacts.toImmutableList(),
                )
            }
            .toImmutableList()
    }

    private fun sectionLabel(contact: ShareTargetUiState.Contact): String {
        val firstCharacter = contact.displayName.trim().firstOrNull()?.uppercaseChar()

        return when {
            firstCharacter?.isLetter() == true -> firstCharacter.toString()
            else -> NON_LETTER_SECTION_LABEL
        }
    }

    private companion object {
        private const val NON_LETTER_SECTION_LABEL = "#"
    }
}
