package com.android.messaging.ui.contact.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.android.messaging.R
import com.android.messaging.ui.contact.AddContactConfirmation
import com.android.messaging.ui.contact.launchAddContact
import com.android.messaging.ui.contact.model.AddContactRequest
import com.android.messaging.ui.contact.model.AddContactUiState
import com.android.messaging.ui.navigation.LocalNavigator
import com.android.messaging.ui.navigation.Navigator
import com.android.messaging.ui.navigation.paneTitleMetadata
import com.android.messaging.util.AccessibilityUtil

internal fun Navigator.navigateToAddContact(request: AddContactRequest) {
    push(destination = AddContactNavKey(request = request))
}

internal fun EntryProviderScope<NavKey>.addContactEntries() {
    entry<AddContactNavKey>(
        metadata = DialogSceneStrategy.dialog() +
            paneTitleMetadata(R.string.add_contact_confirmation_dialog_title),
        content = addContactRouteContent(),
    )
}

private fun addContactRouteContent(): @Composable (AddContactNavKey) -> Unit {
    return { navKey ->
        val context = LocalContext.current
        val resources = LocalResources.current
        val navigator = LocalNavigator.current
        val uiState = remember(navKey, resources) {
            AddContactUiState(
                avatarUri = navKey.request.avatarUri,
                destination = navKey.request.destination,
                vocalizedDestination = AccessibilityUtil.getVocalizedPhoneNumber(
                    resources,
                    navKey.request.destination,
                ),
            )
        }

        AddContactConfirmation(
            uiState = uiState,
            onConfirm = {
                launchAddContact(
                    context = context,
                    destination = navKey.request.destination,
                )
                navigator.back()
            },
            onDismiss = navigator::back,
        )
    }
}
