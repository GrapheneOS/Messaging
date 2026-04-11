package com.android.common.test.rules

import com.android.common.test.helpers.TestDataSeeder
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
                TestDataSeeder.seedTestData()
                try {
                    base.evaluate()
                } finally {
                    TestDataSeeder.clearSeededTestData()
                }
            }
        }
    }
}
