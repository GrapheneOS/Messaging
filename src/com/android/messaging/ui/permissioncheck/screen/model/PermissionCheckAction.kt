package com.android.messaging.ui.permissioncheck.screen.model

internal sealed interface PermissionCheckAction {

    data object NextClicked : PermissionCheckAction

    data object SettingsClicked : PermissionCheckAction
}
