package com.android.messaging.ui.navigation

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
internal fun SeededViewModelStoreOwner(
    defaultArgs: Bundle,
    content: @Composable () -> Unit,
) {
    val parentOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "Missing ViewModelStoreOwner"
    }

    require(parentOwner is HasDefaultViewModelProviderFactory) {
        "Owner has no default extras"
    }

    val seededOwner = remember(parentOwner, defaultArgs) {
        seededViewModelStoreOwner(
            parentOwner = parentOwner,
            defaultArgs = defaultArgs,
        )
    }

    CompositionLocalProvider(LocalViewModelStoreOwner provides seededOwner) {
        content()
    }
}

private fun <T> seededViewModelStoreOwner(
    parentOwner: T,
    defaultArgs: Bundle,
): ViewModelStoreOwner where T : ViewModelStoreOwner, T : HasDefaultViewModelProviderFactory {
    return object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {

        override val viewModelStore: ViewModelStore
            get() = parentOwner.viewModelStore

        override val defaultViewModelProviderFactory: ViewModelProvider.Factory
            get() = parentOwner.defaultViewModelProviderFactory

        override val defaultViewModelCreationExtras: CreationExtras
            get() = MutableCreationExtras(parentOwner.defaultViewModelCreationExtras).apply {
                this[DEFAULT_ARGS_KEY] = defaultArgs
            }
    }
}
