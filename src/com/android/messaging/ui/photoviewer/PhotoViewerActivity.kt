package com.android.messaging.ui.photoviewer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.android.messaging.ui.core.AppTheme
import com.android.messaging.ui.photoviewer.model.PhotoViewerLaunchRequest
import com.android.messaging.ui.photoviewer.model.PhotoViewerSourceBounds
import com.android.messaging.ui.photoviewer.screen.PhotoViewerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class PhotoViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(scrim = Color.TRANSPARENT),
        )

        val launchRequest = toPhotoViewerLaunchRequest(intent = intent) ?: run {
            finish()
            return
        }

        setContent {
            AppTheme {
                PhotoViewerScreen(
                    launchRequest = launchRequest,
                    onFinish = ::finishAfterTransition,
                )
            }
        }
    }

    private fun toPhotoViewerLaunchRequest(intent: Intent): PhotoViewerLaunchRequest? {
        val initialPhotoUri = intent.getParcelableExtra(
            EXTRA_INITIAL_PHOTO_URI,
            Uri::class.java,
        )
        val photosUri = intent.getParcelableExtra(
            EXTRA_PHOTOS_URI,
            Uri::class.java,
        )

        if (initialPhotoUri == null || photosUri == null) {
            return null
        }

        return PhotoViewerLaunchRequest(
            initialPhotoUri = initialPhotoUri.toString(),
            photosUri = photosUri.toString(),
            sourceBounds = sourceBoundsFromIntent(intent = intent),
        )
    }

    private fun sourceBoundsFromIntent(intent: Intent): PhotoViewerSourceBounds {
        val left = intent.getIntExtra(EXTRA_START_BOUNDS_LEFT, 0)
        val top = intent.getIntExtra(EXTRA_START_BOUNDS_TOP, 0)

        return PhotoViewerSourceBounds(
            left = left,
            top = top,
            right = left + intent.getIntExtra(EXTRA_START_BOUNDS_WIDTH, 0),
            bottom = top + intent.getIntExtra(EXTRA_START_BOUNDS_HEIGHT, 0),
        )
    }

    companion object {
        private const val EXTRA_INITIAL_PHOTO_URI =
            "com.android.messaging.photoviewer.extra.INITIAL_PHOTO_URI"
        private const val EXTRA_PHOTOS_URI =
            "com.android.messaging.photoviewer.extra.PHOTOS_URI"
        private const val EXTRA_START_BOUNDS_LEFT =
            "com.android.messaging.photoviewer.extra.START_BOUNDS_LEFT"
        private const val EXTRA_START_BOUNDS_TOP =
            "com.android.messaging.photoviewer.extra.START_BOUNDS_TOP"
        private const val EXTRA_START_BOUNDS_WIDTH =
            "com.android.messaging.photoviewer.extra.START_BOUNDS_WIDTH"
        private const val EXTRA_START_BOUNDS_HEIGHT =
            "com.android.messaging.photoviewer.extra.START_BOUNDS_HEIGHT"

        @JvmStatic
        fun createIntent(
            context: Context,
            initialPhotoUri: Uri,
            photosUri: Uri,
            sourceBounds: Rect,
        ): Intent {
            return Intent(context, PhotoViewerActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_PHOTO_URI, initialPhotoUri)
                putExtra(EXTRA_PHOTOS_URI, photosUri)
                putExtra(EXTRA_START_BOUNDS_LEFT, sourceBounds.left)
                putExtra(EXTRA_START_BOUNDS_TOP, sourceBounds.top)
                putExtra(EXTRA_START_BOUNDS_WIDTH, sourceBounds.width())
                putExtra(EXTRA_START_BOUNDS_HEIGHT, sourceBounds.height())
            }
        }
    }
}
