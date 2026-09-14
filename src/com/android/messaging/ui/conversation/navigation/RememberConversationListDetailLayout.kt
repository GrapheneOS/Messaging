@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

package com.android.messaging.ui.conversation.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.window.core.layout.WindowSizeClass

@Composable
internal fun rememberConversationListDetailLayout(): ConversationListDetailLayout {
    val directive = rememberConversationPaneDirective()
    val sceneStrategy = rememberListDetailSceneStrategy<NavKey>(
        backNavigationBehavior = BackNavigationBehavior.PopLatest,
        directive = directive,
    )

    return remember(sceneStrategy, directive) {
        ConversationListDetailLayout(
            sceneStrategy = sceneStrategy,
            showsTwoPanes = directive.maxHorizontalPartitions > SINGLE_PARTITION,
        )
    }
}

@Composable
private fun rememberConversationPaneDirective(): PaneScaffoldDirective {
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()

    return remember(windowAdaptiveInfo) {
        val directive = calculatePaneScaffoldDirective(windowAdaptiveInfo)
        val isTwoPanels = fitsTwoPanes(
            posture = windowAdaptiveInfo.windowPosture,
            windowSizeClass = windowAdaptiveInfo.windowSizeClass,
        )

        when {
            isTwoPanels -> directive.copy(
                maxHorizontalPartitions = TWO_PARTITIONS,
                horizontalPartitionSpacerSize = PaneSpacerSize,
                defaultPanePreferredWidth = ListPanePreferredWidth,
            )

            else -> directive.copy(maxHorizontalPartitions = SINGLE_PARTITION)
        }
    }
}

private fun fitsTwoPanes(
    posture: Posture,
    windowSizeClass: WindowSizeClass,
): Boolean {
    if (posture.hingeList.any { hinge -> hinge.isVertical && hinge.isSeparating }) {
        return true
    }

    val minimumWidthDp = when {
        posture.hingeList.isEmpty() -> WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
        else -> WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
    }

    return windowSizeClass.isWidthAtLeastBreakpoint(minimumWidthDp) &&
        windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
}

private val PaneSpacerSize = 0.dp
private val ListPanePreferredWidth = 400.dp
private const val SINGLE_PARTITION = 1
private const val TWO_PARTITIONS = 2
