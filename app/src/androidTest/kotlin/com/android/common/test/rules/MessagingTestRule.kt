package com.android.common.test.rules

import com.android.common.test.helpers.targetContext
import com.android.messaging.debug.clearSeededTestData
import com.android.messaging.debug.seedTestData
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MessagingTestRule : TestRule {

    override fun apply(
        base: Statement,
        description: Description,
    ): Statement {
        return object : Statement() {
            override fun evaluate() {
                seedTestData(targetContext)
                try {
                    base.evaluate()
                } finally {
                    clearSeededTestData(targetContext)
                }
            }
        }
    }
}
