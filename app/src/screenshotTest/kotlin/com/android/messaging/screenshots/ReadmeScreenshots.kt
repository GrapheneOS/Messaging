package com.android.messaging.screenshots

import android.content.res.Configuration
import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellular4Bar
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.asPainter
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.android.messaging.data.conversation.model.ConversationId
import com.android.messaging.ui.conversation.composer.ui.ConversationComposeAttachmentMenuContent
import com.android.messaging.ui.conversation.entry.NewChatScreenContent
import com.android.messaging.ui.conversation.preview.previewSimSelectorUiState
import com.android.messaging.ui.conversation.screen.ConversationScreenModel
import com.android.messaging.ui.conversation.screen.ConversationScreenScaffold
import com.android.messaging.ui.conversation.screen.model.ConversationScreenScaffoldUiState
import com.android.messaging.ui.conversationlist.chats.ConversationListScaffold
import com.android.messaging.ui.core.MessagingPreviewTheme
import com.android.tools.screenshot.PreviewTest
import java.io.File
import java.lang.reflect.Proxy
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import javax.imageio.ImageIO

private const val README_DEVICE = "spec:width=1080px,height=2400px,dpi=420"
private const val STATUS_BAR_HEIGHT_PX = 108
private const val GESTURE_BAR_HEIGHT_PX = 63

private val StatusBarClockFormat = DateTimeFormatter.ofPattern("h:mm")

private val BatteryLevelTextStyle = TextStyle(
    fontSize = 12.5.sp,
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = "pnum",
    lineHeight = 13.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    ),
)

@Preview(device = README_DEVICE)
private annotation class ReadmeScreenshot

@Preview(
    device = README_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
)
private annotation class ReadmeDarkScreenshot

// Layoutlib derives dynamic colour at Standard contrast only, so these are the schemes a phone
// derives at Medium contrast (0.5), generated from the seed of Layoutlib's blue example wallpaper
// (#76777C) with its bundled SystemUI monet (tonal spot, 2025 spec) and read into roles as
// dynamicLightColorScheme and dynamicDarkColorScheme read them on API 34+.
private val ReadmeColorScheme = lightColorScheme(
    primary = Color(0xFF28436C),
    onPrimary = Color(0xFFC7DAFF),
    primaryContainer = Color(0xFF5B75A1),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFB3C7ED),
    secondary = Color(0xFF3A4354),
    onSecondary = Color(0xFFD0DAEF),
    secondaryContainer = Color(0xFF6B7588),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF493C64),
    onTertiary = Color(0xFFE2D2FF),
    tertiaryContainer = Color(0xFF7C6D99),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFFF9F9FE),
    onBackground = Color(0xFF20242B),
    surface = Color(0xFFF9F9FE),
    onSurface = Color(0xFF20242B),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF3F434B),
    inverseSurface = Color(0xFF0C0E12),
    inverseOnSurface = Color(0xFFFFFFFF),
    outline = Color(0xFF5B5F68),
    outlineVariant = Color(0xFF777B84),
    surfaceBright = Color(0xFFF9F9FE),
    surfaceDim = Color(0xFFD7DAE4),
    surfaceContainer = Color(0xFFECEDF6),
    surfaceContainerHigh = Color(0xFFE6E8F1),
    surfaceContainerHighest = Color(0xFFE0E2EC),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceTint = Color(0xFF28436C),
    primaryFixed = Color(0xFF5B75A1),
    primaryFixedDim = Color(0xFF4E6994),
    onPrimaryFixed = Color(0xFFFFFFFF),
    onPrimaryFixedVariant = Color(0xFFFFFFFF),
    secondaryFixed = Color(0xFF6B7588),
    secondaryFixedDim = Color(0xFF5F687B),
    onSecondaryFixed = Color(0xFFFFFFFF),
    onSecondaryFixedVariant = Color(0xFFFFFFFF),
    tertiaryFixed = Color(0xFF7C6D99),
    tertiaryFixedDim = Color(0xFF6F618C),
    onTertiaryFixed = Color(0xFFFFFFFF),
    onTertiaryFixedVariant = Color(0xFFFFFFFF),
)

private val ReadmeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFB3C7ED),
    onPrimary = Color(0xFF233755),
    primaryContainer = Color(0xFF627597),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFF28436C),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF2D3647),
    secondaryContainer = Color(0xFF6B7588),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFEADDFF),
    onTertiary = Color(0xFF4E4169),
    tertiaryContainer = Color(0xFFDECCFD),
    onTertiaryContainer = Color(0xFF453960),
    background = Color(0xFF0C0E12),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0C0E12),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF22262D),
    onSurfaceVariant = Color(0xFFB6B8C2),
    inverseSurface = Color(0xFFF9F9FE),
    inverseOnSurface = Color(0xFF20242B),
    outline = Color(0xFF90939D),
    outlineVariant = Color(0xFF72757E),
    surfaceBright = Color(0xFF282C34),
    surfaceDim = Color(0xFF0C0E12),
    surfaceContainer = Color(0xFF171A1F),
    surfaceContainerHigh = Color(0xFF1C2026),
    surfaceContainerHighest = Color(0xFF22262D),
    surfaceContainerLow = Color(0xFF111318),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceTint = Color(0xFFB3C7ED),
    primaryFixed = Color(0xFF5B75A1),
    primaryFixedDim = Color(0xFF4E6994),
    onPrimaryFixed = Color(0xFFFFFFFF),
    onPrimaryFixedVariant = Color(0xFFFFFFFF),
    secondaryFixed = Color(0xFF6B7588),
    secondaryFixedDim = Color(0xFF5F687B),
    onSecondaryFixed = Color(0xFFFFFFFF),
    onSecondaryFixedVariant = Color(0xFFFFFFFF),
    tertiaryFixed = Color(0xFF7C6D99),
    tertiaryFixedDim = Color(0xFF6F618C),
    onTertiaryFixed = Color(0xFFFFFFFF),
    onTertiaryFixedVariant = Color(0xFFFFFFFF),
)

@PreviewTest
@ReadmeScreenshot
@Composable
private fun ConversationList() {
    ReadmeScreen(clock = ReadmeSceneTime) {
        ConversationListScaffold(
            modifier = Modifier.fillMaxSize(),
            uiState = readmeConversationListUiState(),
            listState = rememberLazyListState(),
            snackbarHostState = SnackbarHostState(),
            pinAnimationController = null,
            onAction = {},
            onDeleteClick = {},
            onSnoozeClick = {},
            onScrollToTop = {},
        )
    }
}

@PreviewTest
@ReadmeScreenshot
@Composable
private fun Chat() {
    ReadmeConversation(clock = LocalTime.of(10, 9), uiState = readmeChatUiState()) {
        OpenAttachmentMenu(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 92.dp),
        )
    }
}

@PreviewTest
@ReadmeDarkScreenshot
@Composable
private fun GroupChat() {
    ReadmeConversation(
        clock = ReadmeSceneTime.plusMinutes(2),
        uiState = readmeGroupChatUiState(),
    )
}

@PreviewTest
@ReadmeScreenshot
@Composable
private fun NewChat() {
    ReadmeScreen(clock = ReadmeSceneTime.plusMinutes(4)) {
        NewChatScreenContent(
            modifier = Modifier.fillMaxSize(),
            pickerUiState = readmeRecipientPickerUiState(),
            simSelectorUiState = previewSimSelectorUiState(),
        )
    }
}

@Composable
private fun ReadmeConversation(
    clock: LocalTime,
    uiState: ConversationScreenScaffoldUiState,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    ReadmeScreen(clock = clock) {
        Box(modifier = Modifier.fillMaxSize()) {
            ConversationScreenScaffold(
                modifier = Modifier.fillMaxSize(),
                conversationId = ConversationId("readme"),
                uiState = uiState,
                snackbarHostState = SnackbarHostState(),
                isMediaPickerOpen = false,
                messageFieldFocusRequester = FocusRequester(),
                pendingScrollMessageId = null,
                onPendingScrollMessageIdConsumed = {},
                onAddPeopleClick = {},
                onConversationDetailsClick = {},
                onNavigateBack = {},
                onOpenContactPicker = {},
                onOpenMediaPicker = {},
                onAudioRecordingStartRequest = {},
                onLockedAudioRecordingStartRequest = {},
                screenModel = NoOpConversationScreenModel,
            )
            overlay()
        }
    }
}

// The composer keeps the menu's open state to itself, so the open menu is drawn over it: the same
// items in the DropdownMenu's container, where the menu opens 8dp above the + button.
@Composable
private fun OpenAttachmentMenu(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .width(IntrinsicSize.Max),
        ) {
            ConversationComposeAttachmentMenuContent(
                isAudioRecordActionEnabled = true,
                onMediaPickerClick = {},
                onAudioAttachClick = {},
                onContactAttachClick = {},
            )
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
private fun ReadmeScreen(clock: LocalTime, content: @Composable () -> Unit) {
    val context = LocalContext.current
    remember(context) {
        ReadmeFactory(context).install()
        TimeZone.setDefault(TimeZone.getTimeZone(ReadmeSceneZone))
    }
    val colorScheme = when {
        isSystemInDarkTheme() -> ReadmeDarkColorScheme
        else -> ReadmeColorScheme
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides ReadmeImages) {
        MessagingPreviewTheme {
            MaterialTheme(colorScheme = colorScheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ReadmeSystemBars(clock = clock, content = content)
                }
            }
        }
    }
}

// Layoutlib draws no system bars and dispatches zero insets on every layout pass. The compose
// view's parent swaps them for a Pixel 7 Pro's at 420dpi on the way down, so the screens pad for
// the bars edge to edge, as on a phone, and the bars are drawn on top.
@Composable
private fun ReadmeSystemBars(clock: LocalTime, content: @Composable () -> Unit) {
    val composeView = LocalView.current
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        StatusBar(clock = clock, modifier = Modifier.align(Alignment.TopCenter))
        GestureHandle(modifier = Modifier.align(Alignment.BottomCenter))
    }
    // Side effects run after the content's effects have installed Compose's insets listener.
    SideEffect {
        val insets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.statusBars(),
                Insets.of(0, STATUS_BAR_HEIGHT_PX, 0, 0),
            )
            .setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                Insets.of(0, 0, 0, GESTURE_BAR_HEIGHT_PX),
            )
            .setVisible(WindowInsetsCompat.Type.systemBars(), true)
            .build()
        val insetsHost = composeView.parent as View
        ViewCompat.setOnApplyWindowInsetsListener(insetsHost) { _, _ -> insets }
        ViewCompat.dispatchApplyWindowInsets(insetsHost, insets)
    }
}

@Composable
private fun StatusBar(clock: LocalTime, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(with(LocalDensity.current) { STATUS_BAR_HEIGHT_PX.toDp() })
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = clock.format(StatusBarClockFormat), color = color, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color,
        )
        Icon(
            imageVector = Icons.Rounded.SignalCellular4Bar,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color,
        )
        StatusBarBattery(color = color, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun StatusBarBattery(color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 23.dp, height = 13.dp)
                .background(color = color, shape = RoundedCornerShape(size = 4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "100",
                modifier = Modifier.offset(y = (-0.2).dp),
                color = MaterialTheme.colorScheme.surface,
                style = BatteryLevelTextStyle,
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 1.dp)
                .size(width = 1.5.dp, height = 6.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(topEnd = 1.dp, bottomEnd = 1.dp),
                ),
        )
    }
}

@Composable
private fun GestureHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(with(LocalDensity.current) { GESTURE_BAR_HEIGHT_PX.toDp() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 108.dp, height = 4.dp)
                .background(color = MaterialTheme.colorScheme.onSurface, shape = CircleShape),
        )
    }
}

// The scaffold only takes method references from the model, so nothing is ever called on it.
private val NoOpConversationScreenModel = Proxy.newProxyInstance(
    ConversationScreenModel::class.java.classLoader,
    arrayOf(ConversationScreenModel::class.java),
) { _, _, _ -> null } as ConversationScreenModel

// Serves `readme://<name>` from the CC0 photos in `src/screenshotTest/images`. Layoutlib can't
// decode JPEG and doesn't see this source set's assets, so the file is read with ImageIO from the
// module directory, which is the test's working directory.
@OptIn(ExperimentalCoilApi::class)
private object ReadmeImages : AsyncImagePreviewHandler {
    override suspend fun handle(
        imageLoader: ImageLoader,
        request: ImageRequest,
    ): AsyncImagePainter.State {
        val name = request.data.toString().substringAfterLast('/')
        val photo = ImageIO.read(File("src/screenshotTest/images/$name"))
        val pixels = photo.getRGB(0, 0, photo.width, photo.height, null, 0, photo.width)
        val image = Bitmap.createBitmap(
            pixels,
            photo.width,
            photo.height,
            Bitmap.Config.ARGB_8888
        ).asImage()
        return AsyncImagePainter.State.Success(
            painter = image.asPainter(request.context),
            result = SuccessResult(image = image, request = request),
        )
    }
}
