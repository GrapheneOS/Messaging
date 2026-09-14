package com.android.messaging.domain.vcarddetail.usecase

import androidx.core.net.toUri
import com.android.messaging.datamodel.MediaScratchFileProvider
import com.android.messaging.di.core.IoDispatcher
import com.android.messaging.domain.vcarddetail.model.AddVCardToContactsResult
import com.android.messaging.util.UriUtil
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

internal interface AddVCardToContacts {
    suspend operator fun invoke(vCardUri: String, displayName: String?): AddVCardToContactsResult
}

internal class AddVCardToContactsImpl @Inject constructor(
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : AddVCardToContacts {

    override suspend operator fun invoke(
        vCardUri: String,
        displayName: String?,
    ): AddVCardToContactsResult {
        return withContext(ioDispatcher) {
            val scratchUri = UriUtil.persistContentToScratchSpace(
                vCardUri.toUri(),
            ) ?: return@withContext AddVCardToContactsResult.Failed

            if (!displayName.isNullOrBlank()) {
                MediaScratchFileProvider.addUriToDisplayNameEntry(scratchUri, displayName)
            }

            AddVCardToContactsResult.Prepared(scratchUri.toString())
        }
    }
}
