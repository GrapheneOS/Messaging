package com.android.messaging.ui.conversation.messages.ui.message.rendering

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.android.common.test.helpers.targetContext
import com.android.messaging.R
import com.android.messaging.ui.conversation.messages.model.message.MmsDownloadUiModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ConversationMmsDownloadBodyRenderingTest :
    BaseConversationMessageRenderingTest() {

    @Test
    fun awaitingManualDownload_rendersDownloadTitleInfoAndAction() {
        val download = mmsDownload(
            state = MmsDownloadUiModel.State.AwaitingManualDownload,
        )

        setMmsDownloadBodyContent(
            download = download,
            canDownloadMessage = true,
        )

        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_title_manual_download))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = expectedInfoText(download = download))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_status_download))
            .assertIsDisplayed()
    }

    @Test
    fun downloading_rendersInProgressStatus() {
        setMmsDownloadBodyContent(
            download = mmsDownload(state = MmsDownloadUiModel.State.Downloading),
            canDownloadMessage = false,
        )

        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_title_downloading))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_status_downloading))
            .assertIsDisplayed()
    }

    @Test
    fun downloadFailed_rendersRetryAction() {
        setMmsDownloadBodyContent(
            download = mmsDownload(state = MmsDownloadUiModel.State.DownloadFailed),
            canDownloadMessage = true,
        )

        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_title_download_failed))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_status_download))
            .assertIsDisplayed()
    }

    @Test
    fun expiredOrUnavailable_rendersUnavailableStatus() {
        setMmsDownloadBodyContent(
            download = mmsDownload(state = MmsDownloadUiModel.State.ExpiredOrUnavailable),
            canDownloadMessage = false,
        )

        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_title_download_failed))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_status_download_error))
            .assertIsDisplayed()
    }

    @Test
    fun secondaryUserAwaitingManualDownload_rendersOwnerUserReferral() {
        setMmsDownloadBodyContent(
            download = mmsDownload(
                state = MmsDownloadUiModel.State.AwaitingManualDownload,
                isSecondaryUser = true,
            ),
            canDownloadMessage = true,
        )

        composeTestRule
            .onNodeWithText(
                text = stringResourceText(R.string.message_title_download_secondary_user)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                text = stringResourceText(R.string.message_status_download_secondary_user)
            )
            .assertIsDisplayed()
    }

    @Test
    fun secondaryUserDownloadFailed_rendersOwnerUserReferral() {
        setMmsDownloadBodyContent(
            download = mmsDownload(
                state = MmsDownloadUiModel.State.DownloadFailed,
                isSecondaryUser = true,
            ),
            canDownloadMessage = true,
        )

        composeTestRule
            .onNodeWithText(
                text = stringResourceText(R.string.message_title_download_secondary_user)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                text = stringResourceText(R.string.message_status_download_secondary_user),
            )
            .assertIsDisplayed()
    }

    @Test
    fun secondaryUserDownloading_rendersRegularDownloadingStatus() {
        setMmsDownloadBodyContent(
            download = mmsDownload(
                state = MmsDownloadUiModel.State.Downloading,
                isSecondaryUser = true,
            ),
            canDownloadMessage = false,
        )

        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_title_downloading))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_status_downloading))
            .assertIsDisplayed()
    }

    @Test
    fun secondaryUserExpiredOrUnavailable_rendersUnavailableStatus() {
        setMmsDownloadBodyContent(
            download = mmsDownload(
                state = MmsDownloadUiModel.State.ExpiredOrUnavailable,
                isSecondaryUser = true,
            ),
            canDownloadMessage = false,
        )

        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_title_download_failed))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(text = stringResourceText(R.string.message_status_download_error))
            .assertIsDisplayed()
    }

    @Test
    fun simDisplayName_presentAppendsSimAnnotation() {
        val statusLine = buildStatusLineText(
            statusText = stringResourceText(R.string.message_status_download),
            simDisplayName = SIM_DISPLAY_NAME,
        )

        setMmsDownloadBodyContent(
            download = mmsDownload(state = MmsDownloadUiModel.State.AwaitingManualDownload),
            canDownloadMessage = true,
            simDisplayName = SIM_DISPLAY_NAME,
        )

        composeTestRule
            .onNodeWithText(text = statusLine)
            .assertIsDisplayed()
    }

    @Test
    fun simDisplayName_blankOmitsSimAnnotation() {
        val statusText = stringResourceText(R.string.message_status_download)

        setMmsDownloadBodyContent(
            download = mmsDownload(state = MmsDownloadUiModel.State.AwaitingManualDownload),
            canDownloadMessage = true,
            simDisplayName = "   ",
        )

        composeTestRule
            .onNodeWithText(text = statusText)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                text = stringResourceText(
                    resourceId = R.string.conversation_message_sim_annotation,
                    argument = SIM_DISPLAY_NAME,
                ),
                substring = true,
            )
            .assertDoesNotExist()
    }

    private fun expectedInfoText(download: MmsDownloadUiModel): String {
        val formattedSize = Formatter.formatFileSize(targetContext, download.sizeBytes)
        val formattedExpiry = DateUtils.formatDateTime(
            targetContext,
            download.expiryTimestamp,
            DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_NUMERIC_DATE or
                DateUtils.FORMAT_NO_YEAR,
        )

        return targetContext.getString(R.string.mms_info, formattedSize, formattedExpiry)
    }

    @Suppress("SameParameterValue")
    private fun buildStatusLineText(statusText: String, simDisplayName: String): String {
        val simAnnotation = stringResourceText(
            resourceId = R.string.conversation_message_sim_annotation,
            argument = simDisplayName,
        )

        return "$statusText \u2022 $simAnnotation"
    }

    private fun stringResourceText(resourceId: Int): String {
        return targetContext.getString(resourceId)
    }

    private fun stringResourceText(resourceId: Int, argument: Any): String {
        return targetContext.getString(resourceId, argument)
    }
}
