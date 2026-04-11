package com.android.common.test.helpers

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

object ShellCommandHelper {

    fun setupSmsDefaultRole() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        val command = "cmd role add-role-holder android.app.role.SMS $packageName"

        executeShellCommand(command)
    }

    private fun executeShellCommand(command: String): String {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val parcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)

        return ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor).use { inputStream ->
            String(inputStream.readBytes())
        }
    }
}
