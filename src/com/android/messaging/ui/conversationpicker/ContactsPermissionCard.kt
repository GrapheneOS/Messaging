package com.android.messaging.ui.conversationpicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.messaging.R

private val ContactsPermissionCardVerticalMargin = 8.dp
private val ContactsPermissionCardCornerRadius = 20.dp
private val ContactsPermissionCardPadding = 20.dp
private val ContactsPermissionContentSpacing = 8.dp
private val ContactsPermissionIconSize = 32.dp
private val ContactsPermissionButtonTopPadding = 4.dp

@Composable
internal fun ContactsPermissionCard(
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ContactsPermissionCardVerticalMargin),
        shape = RoundedCornerShape(ContactsPermissionCardCornerRadius),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(ContactsPermissionCardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ContactsPermissionContentSpacing),
        ) {
            Icon(
                imageVector = Icons.Rounded.Contacts,
                contentDescription = null,
                modifier = Modifier.size(ContactsPermissionIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = stringResource(R.string.share_contacts_permission_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.share_contacts_permission_rationale),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = onGrant,
                modifier = Modifier.padding(top = ContactsPermissionButtonTopPadding),
            ) {
                Text(text = stringResource(R.string.share_contacts_permission_action))
            }
        }
    }
}
