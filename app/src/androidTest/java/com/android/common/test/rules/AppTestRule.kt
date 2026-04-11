package com.android.common.test.rules

import com.android.common.test.helpers.ShellCommandHelper
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
                ShellCommandHelper.setupSmsDefaultRole()
                base.evaluate()
            }
        }
    }
}
