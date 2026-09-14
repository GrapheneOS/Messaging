package com.android.messaging.ui.conversation.navigation

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategy

@Immutable
internal class ConversationListDetailLayout(
    val sceneStrategy: SceneStrategy<NavKey>,
    val showsTwoPanes: Boolean,
)
