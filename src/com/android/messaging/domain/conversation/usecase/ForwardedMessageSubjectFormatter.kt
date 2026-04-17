package com.android.messaging.domain.conversation.usecase

import android.content.Context
import com.android.messaging.R
import com.android.messaging.sms.cleanseMmsSubject
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal interface ForwardedMessageSubjectFormatter {
    fun format(subject: String?): String?
}

internal class ForwardedMessageSubjectFormatterImpl @Inject constructor(
    @param:ApplicationContext
    private val context: Context,
) : ForwardedMessageSubjectFormatter {

    override fun format(subject: String?): String? {
        val resources = context.resources
        val originalSubject = cleanseMmsSubject(
            resources = resources,
            subject = subject,
        ) ?: return null

        return resources.getString(
            R.string.message_fwd,
            originalSubject,
        )
    }
}
