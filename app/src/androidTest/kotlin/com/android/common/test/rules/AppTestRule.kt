package com.android.common.test.rules

import com.android.common.test.helpers.ShellCommandHelper
import com.android.common.test.helpers.SmsWarningHelper
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class AppTestRule : TestRule {

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement {
        return object : Statement() {
            override fun evaluate() {
                val previousSmsRoleHolders = ShellCommandHelper.setupSmsDefaultRole()
                val previousAcknowledgedVersion = SmsWarningHelper.acknowledgeSmsWarning()
                var baseFailure: Throwable? = null
                try {
                    base.evaluate()
                } catch (throwable: Throwable) {
                    baseFailure = throwable
                } finally {
                    try {
                        SmsWarningHelper.restoreSmsWarning(
                            previousAcknowledgedVersion = previousAcknowledgedVersion,
                        )
                        ShellCommandHelper.restoreSmsDefaultRole(
                            previousRoleHolders = previousSmsRoleHolders,
                        )
                    } catch (restoreFailure: Throwable) {
                        if (baseFailure == null) {
                            throw restoreFailure
                        }
                        baseFailure.addSuppressed(restoreFailure)
                    }
                }

                if (baseFailure != null) {
                    throw baseFailure
                }
            }
        }
    }
}
