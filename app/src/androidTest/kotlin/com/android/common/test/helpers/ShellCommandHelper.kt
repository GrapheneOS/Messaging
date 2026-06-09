package com.android.common.test.helpers

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

object ShellCommandHelper {

    private var originalSmsRoleHolders: List<String>? = null
    private var smsRoleRestoreGeneration = 0

    fun setupSmsDefaultRole(): List<String> {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val persistedRoleHolders = readPersistedSmsRoleHolders(packageName = packageName)
        val currentRoleHolders = getSmsRoleHolders()
        val previousRoleHolders = originalSmsRoleHolders
            ?: (persistedRoleHolders ?: currentRoleHolders).also { roleHolders ->
                originalSmsRoleHolders = roleHolders
            }
        cancelScheduledSmsDefaultRoleRestore(
            packageName = packageName,
            previousRoleHolders = previousRoleHolders,
        )

        if (packageName !in currentRoleHolders) {
            executeCheckedShellCommand(
                command = "cmd role add-role-holder $SMS_ROLE_NAME $packageName",
                failureMessage = "Failed to set SMS default role holder for $packageName",
            )
        }

        return previousRoleHolders
    }

    fun restoreSmsDefaultRole(previousRoleHolders: List<String>) {
        val currentRoleHolders = getSmsRoleHolders()
        if (currentRoleHolders == previousRoleHolders) {
            return
        }

        scheduleSmsDefaultRoleRestore(previousRoleHolders = previousRoleHolders)
    }

    private fun cancelScheduledSmsDefaultRoleRestore(
        packageName: String,
        previousRoleHolders: List<String>,
    ) {
        smsRoleRestoreGeneration += 1
        writeSmsRoleRestoreState(
            generationFilePath = smsRoleRestoreGenerationFilePath(packageName = packageName),
            generation = smsRoleRestoreGeneration,
            previousRoleHolders = previousRoleHolders,
            failureMessage = "Failed to cancel pending SMS default role restore",
        )
    }

    // State file format: first line is the cancellation generation, remaining lines are
    // the original role holders to restore.
    // Persisting the holders lets a later instrumentation run that starts before the delayed
    // restore fires recover the device's true original SMS app instead of capturing the
    // test package as the original holder.
    private fun readPersistedSmsRoleHolders(packageName: String): List<String>? {
        val generationFilePath = smsRoleRestoreGenerationFilePath(packageName = packageName)
        val stateFileContent = executeShellCommand(
            command = "sh -c ${shellSingleQuoted(value = "cat $generationFilePath 2>/dev/null")}",
        )
        val stateFileLines = stateFileContent
            .lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }
            .toList()

        return stateFileLines
            .takeIf { it.isNotEmpty() }
            ?.drop(1)
    }

    private fun writeSmsRoleRestoreState(
        generationFilePath: String,
        generation: Int,
        previousRoleHolders: List<String>,
        failureMessage: String,
    ) {
        val stateFileContent = (listOf(generation.toString()) + previousRoleHolders)
            .joinToString(separator = "\n")

        executeCheckedShellCommand(
            command = "sh -c ${
                shellSingleQuoted(
                    value = "printf %s ${
                        shellSingleQuoted(value = stateFileContent)
                    } > $generationFilePath",
                )
            }",
            failureMessage = failureMessage,
        )
    }

    private fun scheduleSmsDefaultRoleRestore(previousRoleHolders: List<String>) {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        smsRoleRestoreGeneration += 1
        val generationFilePath = smsRoleRestoreGenerationFilePath(packageName = packageName)
        val restoreCommand = smsRoleRestoreCommand(
            previousRoleHolders = previousRoleHolders,
            generation = smsRoleRestoreGeneration,
            generationFilePath = generationFilePath,
        )

        writeSmsRoleRestoreState(
            generationFilePath = generationFilePath,
            generation = smsRoleRestoreGeneration,
            previousRoleHolders = previousRoleHolders,
            failureMessage = "Failed to prepare SMS default role restore",
        )
        executeCheckedShellCommand(
            command = "sh -c ${shellSingleQuoted(value = restoreCommand)}",
            failureMessage = "Failed to schedule SMS default role restore",
        )
    }

    private fun smsRoleRestoreCommand(
        previousRoleHolders: List<String>,
        generation: Int,
        generationFilePath: String,
    ): String {
        val restoreRoleHoldersCommand = previousRoleHolders.joinToString(
            separator = " "
        ) { roleHolder ->
            "cmd role add-role-holder $SMS_ROLE_NAME ${shellWord(value = roleHolder)};"
        }

        return "{" +
            " sleep $SMS_ROLE_RESTORE_DELAY_SECONDS;" +
            " if [ \"\$(sed -n 1p ${shellWord(
                value = generationFilePath
            )} 2>/dev/null)\" = \"$generation\" ]; then" +
            " cmd role clear-role-holders $SMS_ROLE_NAME;" +
            " $restoreRoleHoldersCommand" +
            " rm -f ${shellWord(value = generationFilePath)};" +
            " fi;" +
            " } >/dev/null 2>&1 &"
    }

    private fun smsRoleRestoreGenerationFilePath(packageName: String): String {
        return "$SMS_ROLE_RESTORE_GENERATION_FILE_PREFIX$packageName"
    }

    private fun getSmsRoleHolders(): List<String> {
        val result = executeCheckedShellCommand(
            command = "cmd role get-role-holders $SMS_ROLE_NAME",
            failureMessage = "Failed to read SMS default role holders",
        )
        return result
            .lineSequence()
            .map { line -> line.trim() }
            .filter { line -> line.isNotEmpty() }
            .toList()
    }

    private fun executeCheckedShellCommand(
        command: String,
        failureMessage: String,
    ): String {
        val result = executeShellCommand(command = command)
        check(!result.indicatesShellError()) {
            "$failureMessage: $result"
        }
        return result
    }

    private fun executeShellCommand(command: String): String {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val parcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)

        return ParcelFileDescriptor.AutoCloseInputStream(parcelFileDescriptor).use { inputStream ->
            String(inputStream.readBytes())
        }
    }

    private fun String.indicatesShellError(): Boolean {
        return contains(other = "Error", ignoreCase = true) ||
            contains(other = "Exception", ignoreCase = true)
    }

    private fun shellSingleQuoted(value: String): String {
        return "'${value.replace(oldValue = "'", newValue = "'\\''")}'"
    }

    private fun shellWord(value: String): String {
        return shellSingleQuoted(value = value)
    }

    private const val SMS_ROLE_NAME = "android.app.role.SMS"
    private const val SMS_ROLE_RESTORE_DELAY_SECONDS = 15
    private const val SMS_ROLE_RESTORE_GENERATION_FILE_PREFIX =
        "/data/local/tmp/com.android.messaging.sms-role-restore."
}
