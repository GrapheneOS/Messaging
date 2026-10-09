package com.android.messaging.testutil

import android.content.Context
import com.android.messaging.R
import org.xmlpull.v1.XmlPullParser

private const val BACKUP_RULE_DEPTH = 2

internal fun backupRulesExcluding(context: Context, sharedPreferencesName: String): Set<String> {
    val excludingRules = mutableSetOf<String>()
    context.resources.getXml(R.xml.backup_rules).use { parser ->
        var rule = ""
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when {
                parser.eventType != XmlPullParser.START_TAG -> Unit
                parser.depth == BACKUP_RULE_DEPTH -> rule = parser.name
                parser.excludesSharedPreferences(name = sharedPreferencesName) ->
                    excludingRules += rule
            }
        }
    }
    return excludingRules
}

private fun XmlPullParser.excludesSharedPreferences(name: String): Boolean {
    return this.name == "exclude" &&
        getAttributeValue(null, "domain") == "sharedpref" &&
        getAttributeValue(null, "path") == "$name.xml"
}
