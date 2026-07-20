package com.android.messaging.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

@Composable
internal fun rememberAppBackStack(startDestination: NavKey): NavBackStack<NavKey> {
    return key(startDestination) { rememberNavBackStack(startDestination) }
}
