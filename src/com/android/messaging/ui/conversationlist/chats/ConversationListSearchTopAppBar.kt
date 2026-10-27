package com.android.messaging.ui.conversationlist.chats

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.android.messaging.R
import com.android.messaging.ui.common.components.SearchTopAppBarField
import com.android.messaging.ui.conversationlist.common.support.CONVERSATION_LIST_SEARCH_FIELD_TEST_TAG
import com.android.messaging.ui.core.MessagingPreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConversationListSearchTopAppBar(
    searchState: TextFieldState,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.conversation_list_search_close),
                )
            }
        },
        title = {
            SearchTopAppBarField(
                state = searchState,
                hint = R.string.conversation_list_search_hint,
                modifier = Modifier.testTag(CONVERSATION_LIST_SEARCH_FIELD_TEST_TAG),
            )
        },
        actions = {
            if (searchState.text.isNotEmpty()) {
                IconButton(onClick = { searchState.clearText() }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(
                            R.string.conversation_list_search_clear,
                        ),
                    )
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun ConversationListSearchTopAppBarPreview() {
    MessagingPreviewTheme {
        ConversationListSearchTopAppBar(
            searchState = remember { TextFieldState(initialText = "Anna") },
            onCloseClick = {},
        )
    }
}
