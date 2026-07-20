package com.android.messaging.ui

import com.android.messaging.testutil.androidManifestDocument
import com.android.messaging.testutil.elementsByTagName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element

internal class LauncherAliasManifestTest {

    private val launcherAliasName = ".ui.conversationlist.ConversationListActivity"
    private val hostName = ".ui.MainActivity"
    private val mainAction = "android.intent.action.MAIN"
    private val launcherCategory = "android.intent.category.LAUNCHER"

    private val manifest: Document by lazy { androidManifestDocument() }

    @Test
    fun launcherAlias_keepsPublicName_andTargetsHost() {
        val matches = aliases().filter { element ->
            element.getAttribute("android:name") == launcherAliasName
        }

        assertEquals("Expected a single alias named $launcherAliasName", 1, matches.size)

        val alias = matches.single()
        assertEquals(hostName, alias.getAttribute("android:targetActivity"))
        assertEquals("true", alias.getAttribute("android:exported"))
        assertTrue(
            "Alias $launcherAliasName declares no MAIN + LAUNCHER intent filter",
            alias.declaresLauncherIntentFilter(),
        )
    }

    @Test
    fun host_isNotExported_andDeclaresNoIntentFilter() {
        val matches = activities().filter { element ->
            element.getAttribute("android:name") == hostName
        }

        assertEquals("Expected a single activity named $hostName", 1, matches.size)

        val host = matches.single()
        assertEquals("false", host.getAttribute("android:exported"))
        assertEquals(0, host.elementsByTagName(tagName = "intent-filter").size)
    }

    @Test
    fun manifest_declaresSingleLauncherEntry() {
        val launcherEntries = (aliases() + activities())
            .filter { element -> element.declaresLauncherIntentFilter() }
            .map { element -> element.getAttribute("android:name") }

        assertEquals(listOf(launcherAliasName), launcherEntries)
    }

    private fun Element.declaresLauncherIntentFilter(): Boolean {
        return elementsByTagName(tagName = "intent-filter").any { filter ->
            mainAction in filter.childElementValues(tagName = "action") &&
                launcherCategory in filter.childElementValues(tagName = "category")
        }
    }

    private fun Element.childElementValues(tagName: String): Set<String> {
        return elementsByTagName(tagName = tagName)
            .map { element -> element.getAttribute("android:name") }
            .toSet()
    }

    private fun aliases(): List<Element> {
        return manifest.elementsByTagName(tagName = "activity-alias")
    }

    private fun activities(): List<Element> {
        return manifest.elementsByTagName(tagName = "activity")
    }
}
