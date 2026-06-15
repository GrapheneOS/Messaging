package com.android.messaging.ui.subscription.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.messaging.ui.core.MessagingPreviewColumn

internal val SimAvatarDefaultSize: Dp = 40.dp

@Composable
internal fun SimAvatar(
    slotLabel: String,
    accentColor: Color?,
    modifier: Modifier = Modifier,
    size: Dp = SimAvatarDefaultSize,
) {
    Box(
        modifier = modifier
            .size(size = size)
            .clip(shape = CircleShape)
            .background(
                color = accentColor ?: MaterialTheme.colorScheme.primary,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = density.density,
                fontScale = 1f,
            ),
        ) {
            Text(
                text = slotLabel,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SimAvatarPreview() {
    MessagingPreviewColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            SimAvatar(slotLabel = "1", accentColor = null)
            SimAvatar(slotLabel = "2", accentColor = Color(color = 0xFF2E7D32))
        }
    }
}
