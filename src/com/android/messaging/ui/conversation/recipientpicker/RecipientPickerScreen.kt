@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.messaging.ui.conversation.recipientpicker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.messaging.R
import com.android.messaging.ui.conversation.navigation.RecipientPickerMode

@Composable
internal fun RecipientPickerScreen(
    mode: RecipientPickerMode,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = recipientPickerTitle(mode = mode))
                },
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "",
            )
        }
    }
}

@Composable
private fun recipientPickerTitle(
    mode: RecipientPickerMode,
): String {
    return when (mode) {
        RecipientPickerMode.ADD_PARTICIPANTS -> {
            stringResource(id = R.string.conversation_add_people)
        }

        RecipientPickerMode.CREATE_GROUP -> {
            stringResource(id = R.string.conversation_new_group)
        }
    }
}
