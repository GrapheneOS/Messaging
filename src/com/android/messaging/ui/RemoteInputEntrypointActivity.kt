package com.android.messaging.ui

import android.content.Intent
import android.os.Bundle
import android.telephony.TelephonyManager
import com.android.messaging.datamodel.NoConfirmationSmsSendService
import com.android.messaging.util.LogUtil

class RemoteInputEntrypointActivity : BugleComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (intent?.action) {
            Intent.ACTION_SENDTO -> {
                startService(createSendIntent(remoteInput = intent))
                setResult(RESULT_OK)
            }

            else -> {
                LogUtil.w(LogUtil.BUGLE_TAG, "Unrecognized intent action: ${intent?.action}")
                setResult(RESULT_CANCELED)
            }
        }

        finish()
    }

    private fun createSendIntent(remoteInput: Intent): Intent {
        return Intent(this, NoConfirmationSmsSendService::class.java).apply {
            action = TelephonyManager.ACTION_RESPOND_VIA_MESSAGE
            putExtras(remoteInput)
            clipData = remoteInput.clipData
        }
    }
}
