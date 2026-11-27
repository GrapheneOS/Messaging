# Messaging Regression Checklist

## Application

### First Launch and Onboarding

- [ ] Launch the app after a clean installation.
- [ ] Launch the app after clearing app data.
- [ ] Verify onboarding screen display.
- [ ] Complete all onboarding steps.
- [ ] Set Messaging as the default SMS application.
- [ ] Decline the request to set Messaging as the default SMS application.
- [ ] Set Messaging as the default SMS application after previously declining the request.
- [ ] Change the default SMS application through system settings.
- [ ] Return to Messaging after changing the default SMS application.
- [ ] Close the app during onboarding.
- [ ] Relaunch the app after incomplete onboarding.
- [ ] Verify that onboarding is not displayed again after successful completion.

### Permissions

- [ ] Grant requested permissions.
- [ ] Deny requested permissions.
- [ ] Deny requested permissions repeatedly.
- [ ] Grant previously denied permissions through system settings.
- [ ] Revoke permissions through system settings.
- [ ] Launch the app without Contacts permission.
- [ ] Launch the app without Notifications permission.
- [ ] Verify permission requests when accessing the affected feature for the first time.

### Application Launch

- [ ] Launch the app from the home screen.
- [ ] Launch the app from the app drawer.
- [ ] Open the app from Recent Apps.
- [ ] Switch to another app and return to Messaging.
- [ ] Relaunch the app after closing it.
- [ ] Launch the app after force-stopping it.
- [ ] Launch the app after rebooting the device.
- [ ] Launch the app repeatedly using its icon.
- [ ] Restore the previously opened screen when returning from Recent Apps.

## Conversations

### Conversation List

- [ ] Check that Messaging displays an empty conversation list.
- [ ] Check that Messaging displays a conversation list containing one conversation.
- [ ] Check that Messaging displays a conversation list containing multiple conversations.
- [ ] Check that Messaging displays a saved contact's name.
- [ ] Check that Messaging displays an unknown contact's phone number.
- [ ] Check that Messaging displays a long contact name.
- [ ] Check that Messaging displays a contact avatar.
- [ ] Check that Messaging displays a default contact avatar.
- [ ] Check that Messaging displays a group conversation.
- [ ] Check that Messaging displays the latest incoming message preview.
- [ ] Check that Messaging displays the latest outgoing message preview.
- [ ] Check that Messaging displays an MMS preview.
- [ ] Check that Messaging displays a preview of a message containing an attachment without text.
- [ ] Check that Messaging displays long text in a message preview.
- [ ] Check that Messaging displays multiline text in a message preview.
- [ ] Check that Messaging displays emojis in a message preview.
- [ ] Check that Messaging displays the date and time of the latest message.
- [ ] Check that Messaging displays an unread conversation.
- [ ] Check that Messaging displays a read conversation.
- [ ] Check that Messaging displays a conversation containing a draft.
- [ ] Check that Messaging displays a conversation containing a failed message.
- [ ] Check that Messaging displays a pinned conversation.
- [ ] Check that Messaging displays a conversation with disabled notifications.
- [ ] Sort conversations by latest activity.
- [ ] Update the conversation preview after receiving a message.
- [ ] Update the conversation preview after sending a message.
- [ ] Update the conversation preview after deleting the latest message.
- [ ] Update the conversation preview after deleting a draft.
- [ ] Scroll the conversation list vertically.
- [ ] Preserve the scroll position when returning from a conversation.

### New Conversation

- [ ] Open the new conversation screen.
- [ ] Create a conversation with a saved contact.
- [ ] Create a conversation with an unknown phone number.
- [ ] Create a conversation with a contact containing multiple phone numbers.
- [ ] Reopen an existing conversation through the new conversation screen.
- [ ] Cancel conversation creation without entering a message.
- [ ] Cancel conversation creation after entering text.
- [ ] Create a conversation without Contacts permission.
- [ ] Create a conversation with a previously selected recipient.
- [ ] Verify that duplicate conversations with the same recipient are not created.

### Conversation Details

- [ ] Open an existing conversation.
- [ ] Check that Messaging displays the recipient's name.
- [ ] Check that Messaging displays an unknown recipient's phone number.
- [ ] Check that Messaging displays the recipient's avatar.
- [ ] Check that Messaging displays a long recipient name in the conversation header.
- [ ] Check that Messaging displays incoming messages.
- [ ] Check that Messaging displays outgoing messages.
- [ ] Verify visual distinction between incoming and outgoing messages.
- [ ] Check that Messaging displays messages in chronological order.
- [ ] Check that Messaging displays date separators.
- [ ] Check that Messaging displays message timestamps.
- [ ] Check that Messaging displays messages received or sent today.
- [ ] Check that Messaging displays messages received or sent yesterday.
- [ ] Check that Messaging displays messages from previous months and years.
- [ ] Check that Messaging displays messages with identical timestamps.
- [ ] Check that Messaging displays long messages.
- [ ] Check that Messaging displays multiline messages.
- [ ] Check that Messaging displays messages containing emojis.
- [ ] Check that Messaging displays messages containing attachments.
- [ ] Check that Messaging displays consecutive messages from the same sender.
- [ ] Scroll through message history.
- [ ] Load older messages while scrolling.
- [ ] Receive a new message while viewing the latest messages.
- [ ] Receive a new message while viewing older messages.
- [ ] Navigate to the latest message.
- [ ] Open conversation settings.
- [ ] Open contact information.
- [ ] Return to the conversation list.

### Read / Unread

- [ ] Check that Messaging displays a new conversation as unread.
- [ ] Mark an existing conversation as unread after receiving a message.
- [ ] Update the conversation state after opening an unread message.
- [ ] Receive multiple unread messages in one conversation.
- [ ] Receive unread messages in multiple conversations.
- [ ] Receive a message while the corresponding conversation is open.
- [ ] Receive a message while another conversation is open.
- [ ] Preserve unread conversation states after restarting the app.
- [ ] Verify that opening one unread conversation does not change the state of other unread conversations.

### Pin / Unpin

- [ ] Pin one conversation.
- [ ] Pin multiple conversations.
- [ ] Unpin a conversation.
- [ ] Pin a previously unpinned conversation.
- [ ] Pin an unread conversation.
- [ ] Pin a conversation containing a draft.
- [ ] Pin a group conversation.
- [ ] Receive a message in a pinned conversation.
- [ ] Preserve pinned conversation states after restarting the app.

### Archive / Unarchive

- [ ] Archive one conversation.
- [ ] Archive multiple conversations.
- [ ] Archive a conversation using a swipe gesture.
- [ ] Archive an unread conversation.
- [ ] Archive a conversation containing a draft.
- [ ] Archive a group conversation.
- [ ] Undo conversation archiving.
- [ ] Archive multiple conversations in succession.
- [ ] Undo consecutive conversation archiving.
- [ ] Open the archived conversation list.
- [ ] Check that Messaging displays an empty archive.
- [ ] Open an archived conversation.
- [ ] Unarchive one conversation.
- [ ] Unarchive multiple conversations.
- [ ] Undo conversation unarchiving.
- [ ] Receive a message in an archived conversation.
- [ ] Send a message from an archived conversation.
- [ ] Preserve message history after archiving a conversation.
- [ ] Preserve drafts after archiving a conversation.
- [ ] Preserve archived conversation states after restarting the app.

### Conversation Selection and Deletion

- [ ] Select one conversation using a long press.
- [ ] Select multiple conversations.
- [ ] Deselect one conversation.
- [ ] Deselect all conversations.
- [ ] Exit selection mode.
- [ ] Check that Messaging displays the number of selected conversations.
- [ ] Update the number of selected conversations.
- [ ] Check that Messaging displays available actions for selected conversations.
- [ ] Delete one conversation.
- [ ] Delete multiple conversations.
- [ ] Cancel conversation deletion.
- [ ] Delete an archived conversation.
- [ ] Delete a pinned conversation.
- [ ] Delete an unread conversation.
- [ ] Delete a conversation containing a draft.
- [ ] Delete a conversation containing MMS messages and attachments.
- [ ] Delete the last conversation in the list.
- [ ] Receive a new message after deleting the corresponding conversation.
- [ ] Recreate a deleted conversation.
- [ ] Preserve message history in unselected conversations after bulk deletion.

## Contacts

### Recipient Selection

- [ ] Check that Messaging displays the contact list.
- [ ] Check that Messaging displays an empty contact list.
- [ ] Check that Messaging displays contacts containing one phone number.
- [ ] Check that Messaging displays contacts containing multiple phone numbers.
- [ ] Check that Messaging displays contacts without names.
- [ ] Check that Messaging displays contacts without phone numbers.
- [ ] Check that Messaging displays contacts with identical names.
- [ ] Check that Messaging displays contacts with identical phone numbers.
- [ ] Check that Messaging displays contacts with long names.
- [ ] Check that Messaging displays contacts with avatars.
- [ ] Check that Messaging displays contacts without avatars.
- [ ] Check that Messaging displays contacts with names in different languages.
- [ ] Check that Messaging displays contacts containing emojis in their names.
- [ ] Select a contact from the list.
- [ ] Select a specific phone number belonging to a contact.
- [ ] Select multiple recipients.
- [ ] Remove one recipient from the selected recipient list.
- [ ] Remove all selected recipients.
- [ ] Select an already added recipient.
- [ ] Confirm the selected recipient using the Enter key.
- [ ] Confirm the recipient by tapping Send to.
- [ ] Check that Messaging displays the Send to section when entering a phone number.
- [ ] Update the Send to section when editing a phone number.
- [ ] Check that Messaging displays the Send to section when entering a long phone number.
- [ ] Check that Messaging displays the Send to section when no matching contacts are found.
- [ ] Check that Messaging displays selected recipients when a large number of contacts is selected.

### Contact Search

- [ ] Search by full first name.
- [ ] Search by partial first name.
- [ ] Search by last name.
- [ ] Search by first and last name.
- [ ] Search by full phone number.
- [ ] Search by partial phone number.
- [ ] Search by an additional contact phone number.
- [ ] Search using an international country calling code.
- [ ] Search without an international country calling code.
- [ ] Search using upper- and lowercase letters.
- [ ] Search for contacts containing Cyrillic characters.
- [ ] Search for contacts containing Unicode characters.
- [ ] Search for contacts containing special characters.
- [ ] Search for contacts containing emojis.
- [ ] Search using leading spaces.
- [ ] Search using trailing spaces.
- [ ] Search using one character.
- [ ] Search using multiple characters.
- [ ] Search for a nonexistent contact.
- [ ] Search within an empty contact list.
- [ ] Clear the search query.
- [ ] Change the search query rapidly.
- [ ] Edit the search query.
- [ ] Select a contact from search results.
- [ ] Restore the full contact list after clearing the search query.

### Phone Number Formats

- [ ] Enter a phone number in national format.
- [ ] Enter a phone number in international format.
- [ ] Enter phone numbers with different country calling codes.
- [ ] Enter phone numbers with country calling codes of different lengths.
- [ ] Enter phone numbers of different valid lengths.
- [ ] Enter a phone number with the `+` prefix.
- [ ] Enter a phone number with the `00` international prefix.
- [ ] Enter a phone number containing spaces.
- [ ] Enter a phone number containing hyphens.
- [ ] Enter a phone number containing parentheses.
- [ ] Enter a phone number with a leading zero.
- [ ] Paste a phone number from the clipboard.
- [ ] Enter a phone number of the minimum valid length.
- [ ] Enter a phone number of the maximum valid length.
- [ ] Enter a phone number below the minimum valid length.
- [ ] Enter a phone number exceeding the maximum valid length.
- [ ] Enter a phone number containing letters.
- [ ] Enter a phone number containing special characters.
- [ ] Enter a phone number containing emojis.
- [ ] Enter only the `+` symbol.
- [ ] Enter a phone number containing multiple `+` symbols.
- [ ] Enter whitespace only.
- [ ] Handle an empty phone number field.
- [ ] Automatically populate the country calling code.
- [ ] Edit the automatically populated country calling code.
- [ ] Remove the automatically populated country calling code.
- [ ] Preserve a manually entered country calling code.
- [ ] Automatically populate the country calling code when using SIM cards from different countries.
- [ ] Automatically populate the country calling code when no SIM is available.
- [ ] Automatically populate the country calling code when using two SIM cards from different countries.
- [ ] Match national and international formats of the same phone number.
- [ ] Match phone numbers containing different separators.
- [ ] Match an incoming message sender's phone number with an existing contact.
- [ ] Verify that different phone numbers with identical trailing digits are not incorrectly matched.

### Contact Updates

- [ ] Create a contact for an unknown sender.
- [ ] Add an unknown phone number to an existing contact.
- [ ] Add a name to an existing contact.
- [ ] Change a contact's name.
- [ ] Remove a contact's name.
- [ ] Add a contact avatar.
- [ ] Change a contact avatar.
- [ ] Remove a contact avatar.
- [ ] Change a contact's name and avatar simultaneously.
- [ ] Change a contact's phone number.
- [ ] Add an additional phone number.
- [ ] Remove one of multiple contact phone numbers.
- [ ] Delete a contact.
- [ ] Update contact information in the conversation list.
- [ ] Update contact information in an open conversation.
- [ ] Update contact information on the recipient selection screen.
- [ ] Update participant information in a group conversation.
- [ ] Preserve message history after deleting a contact.
- [ ] Recreate a previously deleted contact.
- [ ] Display contacts after revoking Contacts permission.
- [ ] Refresh the contact list after granting Contacts permission again.

## Message Composer

### Text Input

- [ ] Enter text manually.
- [ ] Paste text from the clipboard.
- [ ] Enter digits.
- [ ] Enter special characters.
- [ ] Enter emojis.
- [ ] Enter multiple consecutive emojis.
- [ ] Enter composite emojis.
- [ ] Enter Latin characters.
- [ ] Enter Cyrillic characters.
- [ ] Enter CJK characters.
- [ ] Enter RTL text.
- [ ] Enter text in multiple languages.
- [ ] Enter multiline text.
- [ ] Enter text containing empty lines.
- [ ] Enter text containing leading and trailing spaces.
- [ ] Enter whitespace only.
- [ ] Enter line breaks only.
- [ ] Enter very long text.
- [ ] Paste a large amount of text.
- [ ] Delete all entered text.
- [ ] Edit text at the beginning of a line.
- [ ] Edit text in the middle of a line.
- [ ] Edit text at the end of a line.
- [ ] Move the cursor within the text input field.
- [ ] Select part of the text.
- [ ] Select all text.
- [ ] Copy text.
- [ ] Cut text.
- [ ] Replace selected text.
- [ ] Paste text over selected text.
- [ ] Check that Messaging displays the character counter.
- [ ] Check that Messaging displays the number of SMS segments.
- [ ] Update the character counter when adding text.
- [ ] Update the character counter when deleting text.
- [ ] Expand the text input field as the number of lines increases.
- [ ] Reduce the text input field height when deleting text.
- [ ] Scroll within the text input field containing a long message.
- [ ] Verify the Send button state when the input field is empty.
- [ ] Verify the Send button state when text is entered.
- [ ] Verify the Send button state when only whitespace is entered.
- [ ] Verify the Send button state when only an attachment is added.
- [ ] Verify the Send button state after removing all text and attachments.

### Keyboard

- [ ] Open the keyboard by tapping the text input field.
- [ ] Close the keyboard.
- [ ] Reopen the keyboard.
- [ ] Switch keyboard languages.
- [ ] Switch to the numeric keyboard.
- [ ] Switch to the emoji keyboard.
- [ ] Insert a line break using the Enter key.
- [ ] Check that Messaging displays the text input field while the keyboard is open.
- [ ] Check that Messaging displays the latest message while the keyboard is open.
- [ ] Scroll message history while the keyboard is open.
- [ ] Open the attachment panel while the keyboard is open.
- [ ] Switch between the keyboard and attachment panel.
- [ ] Restore focus to the text input field after closing the attachment panel.
- [ ] Preserve the cursor position after closing the keyboard.
- [ ] Send a message while the keyboard is open.
- [ ] Verify the keyboard state after sending a message.

### Drafts

- [ ] Create a text draft.
- [ ] Create a draft containing an attachment.
- [ ] Create a draft containing multiple attachments.
- [ ] Create a draft containing text and an attachment.
- [ ] Create a draft containing a message subject.
- [ ] Edit an existing draft.
- [ ] Delete draft text.
- [ ] Remove an attachment from a draft.
- [ ] Remove the message subject from a draft.
- [ ] Preserve a draft when returning to the conversation list.
- [ ] Preserve a draft when switching between conversations.
- [ ] Create drafts in multiple conversations.
- [ ] Edit drafts in different conversations independently.
- [ ] Receive an SMS while a draft exists.
- [ ] Receive an MMS while a draft exists.
- [ ] Check that Messaging displays the draft in the conversation preview after receiving a message.
- [ ] Restore a draft after restarting the app.
- [ ] Restore a draft after rebooting the device.
- [ ] Clear the draft after successfully sending a message.
- [ ] Preserve the draft after a validation error before sending.
- [ ] Restore or resend message content after a sending failure.
- [ ] Remove an empty draft after clearing the input field.

### SMS / MMS Conversion

- [ ] Convert SMS to MMS when adding an attachment.
- [ ] Convert MMS back to SMS after removing all attachments.
- [ ] Convert SMS to MMS when adding a message subject.
- [ ] Convert MMS back to SMS after removing the message subject.
- [ ] Convert a long SMS to MMS according to carrier configuration.
- [ ] Convert MMS back to SMS after reducing text length when no other conversion conditions apply.
- [ ] Determine the message protocol when adding multiple recipients.
- [ ] Determine the message protocol after changing group messaging settings.
- [ ] Check that Messaging displays the current message type.
- [ ] Update the message type when editing draft content.

## SMS

### Sending

- [ ] Send an SMS to a saved contact.
- [ ] Send an SMS to an unknown phone number.
- [ ] Send an SMS to an international phone number.
- [ ] Send an SMS from a new conversation.
- [ ] Send an SMS from an existing conversation.
- [ ] Send an SMS from an archived conversation.
- [ ] Send multiple consecutive messages.
- [ ] Send messages to different recipients in succession.
- [ ] Send and receive SMS messages simultaneously.
- [ ] Tap the Send button rapidly multiple times.
- [ ] Send a message containing one character.
- [ ] Send a message containing digits.
- [ ] Send a message containing special characters.
- [ ] Send a message containing emojis.
- [ ] Send a multiline message.
- [ ] Send a message containing Unicode characters.
- [ ] Send a message containing RTL text.
- [ ] Send a message containing a hyperlink.
- [ ] Send a message containing a phone number.
- [ ] Attempt to send an empty message.
- [ ] Attempt to send a message containing whitespace only.
- [ ] Send an SMS with Wi-Fi disabled.
- [ ] Send an SMS with mobile data disabled.
- [ ] Attempt to send an SMS without cellular network coverage.
- [ ] Attempt to send an SMS in airplane mode without an available SMS transport.
- [ ] Attempt to send an SMS without an active SIM.
- [ ] Send an SMS while internationally roaming.
- [ ] Verify message content on the recipient's device.
- [ ] Verify that sent messages are not duplicated.

### Receiving

- [ ] Receive an SMS from a saved contact.
- [ ] Receive an SMS from an unknown phone number.
- [ ] Receive an SMS from an international phone number.
- [ ] Receive an SMS from an alphanumeric sender.
- [ ] Receive an SMS from a short code.
- [ ] Receive an SMS containing a verification code.
- [ ] Receive an SMS containing a hyperlink.
- [ ] Receive an SMS containing a phone number.
- [ ] Receive an SMS containing emojis.
- [ ] Receive an SMS containing Unicode characters.
- [ ] Receive a multiline SMS.
- [ ] Receive a long SMS.
- [ ] Receive multiple consecutive SMS messages.
- [ ] Receive SMS messages containing identical content.
- [ ] Receive SMS messages from multiple senders simultaneously.
- [ ] Receive an SMS while the conversation list is open.
- [ ] Receive an SMS while the app is in the background.
- [ ] Receive an SMS while the app is closed.
- [ ] Receive an SMS while the device is locked.
- [ ] Receive an SMS after cellular network coverage is restored.
- [ ] Receive an SMS with mobile data disabled.
- [ ] Receive an SMS while internationally roaming.
- [ ] Attempt to reply to an alphanumeric sender.
- [ ] Verify that received messages are not duplicated.

### Multipart SMS

- [ ] Send an SMS containing 159 GSM-7 characters.
- [ ] Send an SMS containing 160 GSM-7 characters.
- [ ] Send an SMS containing 161 GSM-7 characters.
- [ ] Send SMS messages containing 305, 306, and 307 GSM-7 characters.
- [ ] Send SMS messages containing 69, 70, and 71 UCS-2 code units.
- [ ] Send SMS messages containing 133, 134, and 135 UCS-2 code units.
- [ ] Send an SMS containing GSM-7 extension table characters.
- [ ] Update the number of SMS segments when adding a Unicode character.
- [ ] Update the number of SMS segments when adding an emoji.
- [ ] Update the number of SMS segments when removing a Unicode character.
- [ ] Receive a multipart SMS.
- [ ] Assemble message segments in the correct order.
- [ ] Receive message segments with delays.
- [ ] Receive message segments out of order.
- [ ] Verify that message segments are not duplicated.
- [ ] Preserve the complete content of a multipart message.

### Message Status and Delivery Reports

- [ ] Check that Messaging displays the sending status.
- [ ] Check that Messaging displays the successfully sent message status.
- [ ] Check that Messaging displays the failed message status.
- [ ] Check that Messaging displays a delivery report.
- [ ] Send a message with delivery reports enabled.
- [ ] Send a message with delivery reports disabled.
- [ ] Receive a delayed delivery report.
- [ ] Receive delivery reports for multiple messages.
- [ ] Check that Messaging displays the message status when the recipient's device is unavailable.
- [ ] Resend a failed message.
- [ ] Resend a failed message after network recovery.
- [ ] Handle repeated sending failures.
- [ ] Preserve message status after restarting the app.
- [ ] Verify that the displayed status matches the actual sending result.

### Class 0 SMS

- [ ] Receive a Class 0 SMS.
- [ ] Check that Messaging displays Class 0 SMS content.
- [ ] Check that Messaging displays the Class 0 SMS sender.
- [ ] Save a Class 0 SMS.
- [ ] Dismiss a Class 0 SMS without saving it.
- [ ] Receive multiple Class 0 SMS messages.
- [ ] Receive a Class 0 SMS while the device is locked.

## MMS

### Sending

- [ ] Send an MMS containing an image.
- [ ] Send an MMS containing a video.
- [ ] Send an MMS containing audio.
- [ ] Send an MMS containing a GIF.
- [ ] Send an MMS containing a vCard.
- [ ] Send an MMS containing multiple images.
- [ ] Send an MMS containing text and an attachment.
- [ ] Send an MMS without text.
- [ ] Send an MMS containing only a message subject.
- [ ] Send an MMS containing a subject and text.
- [ ] Send an MMS containing a subject, text, and an attachment.
- [ ] Send an MMS containing different supported attachment types.
- [ ] Send multiple consecutive MMS messages.
- [ ] Send an MMS with Wi-Fi disabled.
- [ ] Send an MMS with mobile data disabled.
- [ ] Attempt to send an MMS without network coverage.
- [ ] Send an MMS while roaming.
- [ ] Attempt to send an MMS without an active SIM.
- [ ] Check that Messaging displays the MMS sending status.
- [ ] Check that Messaging displays an MMS sending error.
- [ ] Resend a failed MMS.
- [ ] Verify MMS content on the recipient's device.
- [ ] Verify that sent MMS messages are not duplicated.

### Receiving and Downloading

- [ ] Receive an MMS containing an image.
- [ ] Receive an MMS containing a video.
- [ ] Receive an MMS containing audio.
- [ ] Receive an MMS containing a GIF.
- [ ] Receive an MMS containing a vCard.
- [ ] Receive an MMS containing multiple attachments.
- [ ] Receive an MMS containing different attachment types.
- [ ] Receive an MMS containing text and a subject.
- [ ] Receive an MMS with automatic downloading enabled.
- [ ] Receive an MMS with automatic downloading disabled.
- [ ] Download an MMS manually.
- [ ] Receive an MMS with mobile data disabled.
- [ ] Receive an MMS while network coverage is unavailable.
- [ ] Receive an MMS while roaming with automatic downloading enabled.
- [ ] Receive an MMS while roaming with automatic downloading disabled.
- [ ] Retry downloading an MMS after a failure.
- [ ] Download an MMS after network recovery.
- [ ] Attempt to download an expired MMS.
- [ ] Receive multiple consecutive MMS messages.
- [ ] Receive SMS and MMS messages simultaneously.
- [ ] Download multiple MMS messages simultaneously.
- [ ] Check that Messaging displays the MMS downloading status.
- [ ] Check that Messaging displays an MMS downloading error.
- [ ] Open downloaded attachments.
- [ ] Reopen a previously downloaded MMS without network connectivity.
- [ ] Verify that MMS messages are not duplicated after retrying downloads.

### MMS Limits

- [ ] Send an attachment smaller than the configured size limit.
- [ ] Send an attachment equal to the configured size limit.
- [ ] Attempt to send an attachment exceeding the configured size limit.
- [ ] Add fewer attachments than the configured attachment count limit.
- [ ] Add the maximum allowed number of attachments.
- [ ] Exceed the allowed attachment count.
- [ ] Exceed the size limit with the combined attachment size.
- [ ] Exceed the size limit with the combined text and attachment size.
- [ ] Check that Messaging displays a warning when exceeding MMS limits.
- [ ] Cancel sending after the warning is displayed.
- [ ] Confirm sending using Send anyway.
- [ ] Add one video attachment.
- [ ] Attempt to send two video attachments in one message.
- [ ] Remove an attachment after exceeding the limit.
- [ ] Retry sending after reducing the message size.
- [ ] Preserve draft content after a validation error.
- [ ] Verify MMS limits for different carriers.
- [ ] Verify MMS limits for different SIM cards.

## Group Messaging

### Group Creation

- [ ] Create a conversation with two recipients.
- [ ] Create a conversation with multiple recipients.
- [ ] Create a group containing saved contacts.
- [ ] Create a group containing unknown phone numbers.
- [ ] Create a group containing saved contacts and unknown phone numbers.
- [ ] Add the same recipient multiple times.
- [ ] Add the same phone number in different formats.
- [ ] Add different phone numbers belonging to the same contact.
- [ ] Remove a recipient before creating a group.
- [ ] Remove all recipients before creating a group.
- [ ] Cancel group creation.
- [ ] Recreate a group with the same recipients.
- [ ] Create a group with the same recipients in a different order.
- [ ] Create a group with one fewer recipient than the configured limit.
- [ ] Create a group with the maximum allowed number of recipients.
- [ ] Exceed the recipient count limit.

### Group Messages

- [ ] Send individual SMS messages to multiple recipients.
- [ ] Send a group MMS.
- [ ] Send a group MMS containing an attachment.
- [ ] Send multiple consecutive group messages.
- [ ] Receive a reply from a group participant.
- [ ] Receive replies from multiple group participants.
- [ ] Receive an individual reply after sending individual SMS messages.
- [ ] Check that Messaging displays the sender's name in a group conversation.
- [ ] Check that Messaging displays an unknown participant's phone number.
- [ ] Check that Messaging displays the group participant list.
- [ ] Check that Messaging displays a group avatar.
- [ ] Check that Messaging displays long participant names.
- [ ] Attempt to send a group MMS without a configured self phone number.
- [ ] Enter the self phone number when prompted.
- [ ] Cancel entering the self phone number.
- [ ] Retry sending after entering the self phone number.
- [ ] Switch between group messaging modes.
- [ ] Attempt to send a group message when the selected SIM is unavailable.
- [ ] Attempt to send a group message without network coverage.

## Message Management

### Message Selection

- [ ] Select a message using a long press.
- [ ] Select an incoming message.
- [ ] Select an outgoing message.
- [ ] Select a message containing an attachment.
- [ ] Select a failed message.
- [ ] Select multiple messages.
- [ ] Select SMS and MMS messages simultaneously.
- [ ] Select incoming and outgoing messages simultaneously.
- [ ] Deselect one message.
- [ ] Deselect all messages.
- [ ] Exit message selection mode.
- [ ] Check that Messaging displays the number of selected messages.
- [ ] Update available actions when the selection changes.
- [ ] Receive a new message while message selection mode is active.

### Copy

- [ ] Copy message text.
- [ ] Copy a long message.
- [ ] Copy a message containing emojis.
- [ ] Copy a message containing line breaks.
- [ ] Copy a message containing special characters.
- [ ] Copy a message containing a hyperlink.
- [ ] Copy text from an MMS containing an attachment.
- [ ] Select and copy part of a message, if supported.
- [ ] Verify the copied message content in the clipboard.

### Message Details

- [ ] Open Message Details.
- [ ] Check that Messaging displays details of an incoming SMS.
- [ ] Check that Messaging displays details of an outgoing SMS.
- [ ] Check that Messaging displays details of an incoming MMS.
- [ ] Check that Messaging displays details of an outgoing MMS.
- [ ] Check that Messaging displays details of a failed message.
- [ ] Check that Messaging displays details of a group message.
- [ ] Check that Messaging displays the sender's phone number.
- [ ] Check that Messaging displays the recipient's phone number.
- [ ] Check that Messaging displays the message date and time.
- [ ] Check that Messaging displays the message status.
- [ ] Check that Messaging displays information about the SIM used, if available.
- [ ] Copy available information from Message Details.
- [ ] Close Message Details.

### Forward

- [ ] Forward an SMS.
- [ ] Forward an MMS.
- [ ] Forward a message containing an image.
- [ ] Forward a message containing a video.
- [ ] Forward a message containing audio.
- [ ] Forward a message containing multiple attachments.
- [ ] Forward an incoming message.
- [ ] Forward an outgoing message.
- [ ] Forward a message to an existing contact.
- [ ] Forward a message to an unknown phone number.
- [ ] Forward a message to multiple recipients.
- [ ] Edit message text before forwarding.
- [ ] Remove an attachment before forwarding.
- [ ] Cancel message forwarding.
- [ ] Preserve the original message after forwarding.

### Delete

- [ ] Delete an incoming message.
- [ ] Delete an outgoing message.
- [ ] Delete an MMS.
- [ ] Delete a failed message.
- [ ] Delete an undownloaded MMS.
- [ ] Delete multiple messages.
- [ ] Delete SMS and MMS messages simultaneously.
- [ ] Cancel message deletion.
- [ ] Delete the first message in a conversation.
- [ ] Delete the latest message in a conversation.
- [ ] Delete all messages in a conversation.
- [ ] Delete a message from a group conversation.
- [ ] Preserve unselected messages after bulk deletion.

## Attachments And Media

### Attachment Picker

- [ ] Open the attachment panel.
- [ ] Close the attachment panel.
- [ ] Check that Messaging displays the attachment panel when no media files are available.
- [ ] Select an image.
- [ ] Select a video.
- [ ] Select an audio file.
- [ ] Select a GIF.
- [ ] Select a contact.
- [ ] Select multiple images.
- [ ] Select different attachment types.
- [ ] Deselect a selected attachment.
- [ ] Cancel attachment selection.
- [ ] Confirm attachment selection.
- [ ] Remove one attachment.
- [ ] Remove multiple attachments.
- [ ] Remove all attachments.
- [ ] Add a previously removed attachment again.
- [ ] Add an attachment to existing text.
- [ ] Add text to an existing attachment.
- [ ] Attempt to add an attachment without the required permissions.
- [ ] Attempt to add an attachment from an inaccessible source.
- [ ] Switch conversations while an attachment is being processed.
- [ ] Attempt to send a message while an attachment is being processed.

### Images and Video

- [ ] Add a JPEG image.
- [ ] Add a PNG image.
- [ ] Add a WebP image.
- [ ] Add a GIF.
- [ ] Add a high-resolution image.
- [ ] Add an image containing EXIF orientation metadata.
- [ ] Add a portrait image.
- [ ] Add a landscape image.
- [ ] Add a square image.
- [ ] Add a panoramic image.
- [ ] Add an image containing transparency.
- [ ] Add a short video.
- [ ] Add a long video.
- [ ] Add a portrait video.
- [ ] Add a landscape video.
- [ ] Add a video without audio.
- [ ] Add a video containing audio.
- [ ] Add a corrupted media file.
- [ ] Add a zero-byte file.
- [ ] Add a file with an unsupported format.
- [ ] Add a file with a long filename.
- [ ] Add a file containing Unicode characters in its filename.
- [ ] Add a file whose extension does not match its content.
- [ ] Check that Messaging displays an image preview.
- [ ] Check that Messaging displays a video preview.
- [ ] Check that Messaging displays multiple attachments in a draft.
- [ ] Add an attachment caption, if supported.
- [ ] Edit an attachment caption.
- [ ] Remove an attachment caption.
- [ ] Process and compress large images.
- [ ] Preserve image aspect ratio after processing.

### Camera

- [ ] Open the camera from the attachment panel.
- [ ] Grant Camera permission.
- [ ] Deny Camera permission.
- [ ] Reopen the camera after granting permission.
- [ ] Capture a photo.
- [ ] Check that Messaging displays a captured photo preview.
- [ ] Confirm a captured photo.
- [ ] Discard a captured photo.
- [ ] Retake a photo.
- [ ] Switch between cameras, if supported.
- [ ] Return to the conversation after closing the camera.
- [ ] Add a captured photo to an existing draft.
- [ ] Verify that no unintended attachment is added after discarding a photo.

### Audio Recording

- [ ] Grant Microphone permission.
- [ ] Deny Microphone permission.
- [ ] Retry audio recording after granting permission.
- [ ] Start audio recording.
- [ ] Check that Messaging displays the recording state.
- [ ] Check that Messaging displays the recording duration.
- [ ] Stop audio recording.
- [ ] Cancel audio recording.
- [ ] Record a short audio clip.
- [ ] Record a long audio clip.
- [ ] Reach the recording duration or size limit.
- [ ] Play recorded audio.
- [ ] Delete recorded audio.
- [ ] Record audio again.
- [ ] Add an audio recording to existing text.
- [ ] Play received audio.
- [ ] Pause audio playback.
- [ ] Resume audio playback.
- [ ] Replay audio.
- [ ] Switch between multiple audio messages.
- [ ] Verify that microphone access stops after completing a recording.
- [ ] Verify that microphone access stops after cancelling a recording.

### Media Viewer

- [ ] Open an image from an incoming message.
- [ ] Open an image from an outgoing message.
- [ ] Open a video from a message.
- [ ] Check that Messaging displays a portrait image.
- [ ] Check that Messaging displays a landscape image.
- [ ] Check that Messaging displays an image in full-screen mode.
- [ ] Zoom in using a pinch-to-zoom gesture.
- [ ] Zoom out.
- [ ] Pan a zoomed image.
- [ ] Reach the maximum zoom level.
- [ ] Return to the original zoom level.
- [ ] Navigate between images.
- [ ] Navigate between the first and last available images.
- [ ] Play a video.
- [ ] Pause video playback.
- [ ] Resume video playback.
- [ ] Replay a video.
- [ ] Seek within a video, if supported.
- [ ] Open a corrupted media file.
- [ ] Save an attachment, if the action is available.
- [ ] Save the same attachment repeatedly.
- [ ] Close Media Viewer.
- [ ] Return to the original message.

### vCard

- [ ] Attach a contact containing one phone number.
- [ ] Attach a contact containing multiple phone numbers.
- [ ] Attach a contact containing a first and last name.
- [ ] Attach a contact containing an avatar.
- [ ] Attach a contact containing additional fields.
- [ ] Attach a contact without a phone number.
- [ ] Open received vCard information.
- [ ] Check that Messaging displays the contact name and phone numbers.
- [ ] Check that Messaging displays additional contact information.
- [ ] Save a received contact to Contacts.
- [ ] Add vCard information to an existing contact.
- [ ] Cancel saving a contact.
- [ ] Handle a corrupted vCard.

## Notifications

### Incoming Message Notifications

- [ ] Receive a notification for a new SMS.
- [ ] Receive a notification for a new MMS.
- [ ] Receive a notification for a group message.
- [ ] Receive a notification from a saved contact.
- [ ] Receive a notification from an unknown phone number.
- [ ] Receive a notification from an alphanumeric sender.
- [ ] Receive a notification from a short code.
- [ ] Check that Messaging displays the sender's name.
- [ ] Check that Messaging displays the sender's phone number.
- [ ] Check that Messaging displays the sender's avatar.
- [ ] Check that Messaging displays message content.
- [ ] Check that Messaging displays a long message.
- [ ] Check that Messaging displays a multiline message.
- [ ] Check that Messaging displays emojis.
- [ ] Check that Messaging displays a message containing an attachment.
- [ ] Verify notification behavior while the corresponding conversation is open.
- [ ] Receive a notification while another conversation is open.
- [ ] Receive a notification while the conversation list is open.
- [ ] Receive a notification while the app is in the background.
- [ ] Receive a notification while the app is closed.
- [ ] Receive a notification while the device is locked.
- [ ] Receive multiple notifications from one conversation.
- [ ] Receive notifications from multiple conversations.
- [ ] Group notifications.
- [ ] Update an existing notification after receiving a new message.
- [ ] Check that Messaging displays a message sending failure notification.
- [ ] Check that Messaging displays an MMS downloading failure notification, if supported.

### Notification Actions

- [ ] Expand a notification.
- [ ] Collapse a notification.
- [ ] Open the corresponding conversation by tapping a notification.
- [ ] Open a conversation from a grouped notification.
- [ ] Reply using Quick Reply.
- [ ] Reply to a group message using Quick Reply.
- [ ] Send multiple replies using Quick Reply.
- [ ] Send a long reply using Quick Reply.
- [ ] Send a reply containing emojis using Quick Reply.
- [ ] Reply to a notification without network coverage.
- [ ] Attempt to send an empty reply.
- [ ] Mark a message as read from a notification, if available.
- [ ] Dismiss a notification using a swipe gesture.
- [ ] Clear all notifications.
- [ ] Clear the notification after reading the corresponding message.
- [ ] Clear the notification after deleting the corresponding conversation.
- [ ] Preserve notifications belonging to other unread conversations.
- [ ] Receive a new notification after dismissing the previous one.

### Notification Settings

- [ ] Receive messages with application notifications disabled.
- [ ] Receive messages after re-enabling application notifications.
- [ ] Receive messages with the corresponding notification channel disabled.
- [ ] Receive messages with notifications disabled for a specific conversation.
- [ ] Receive messages after re-enabling conversation notifications.
- [ ] Change the notification sound.
- [ ] Receive messages in silent mode.
- [ ] Receive messages in vibration mode.
- [ ] Receive messages in Do Not Disturb mode.
- [ ] Receive messages after revoking Notifications permission.
- [ ] Receive messages after granting Notifications permission again.

### Snooze

- [ ] Snooze notifications for 1 hour.
- [ ] Snooze notifications for 8 hours.
- [ ] Snooze notifications for 24 hours.
- [ ] Set Snooze to Always.
- [ ] Cancel Snooze.
- [ ] Change the Snooze duration.
- [ ] Receive a message while Snooze is active.
- [ ] Receive a message after Snooze expires.
- [ ] Receive a message after manually cancelling Snooze.
- [ ] Apply Snooze independently to multiple conversations.
- [ ] Preserve Snooze settings after restarting the app.
- [ ] Preserve Snooze settings after rebooting the device.

### Blocked Senders

- [ ] Receive an SMS from a blocked sender.
- [ ] Receive an MMS from a blocked sender.
- [ ] Verify that notifications from blocked senders are suppressed.
- [ ] Receive notifications from other senders while blocked contacts exist.
- [ ] Receive notifications after unblocking a sender.

## Widgets

### Conversation List Widget

- [ ] Add the widget to the home screen.
- [ ] Add the widget when the conversation list is empty.
- [ ] Check that Messaging displays the conversation list.
- [ ] Check that Messaging displays a contact name.
- [ ] Check that Messaging displays an unknown phone number.
- [ ] Check that Messaging displays a long contact name.
- [ ] Check that Messaging displays a contact avatar.
- [ ] Check that Messaging displays a default contact avatar.
- [ ] Check that Messaging displays a group conversation.
- [ ] Check that Messaging displays a message preview.
- [ ] Check that Messaging displays long text.
- [ ] Check that Messaging displays multiline text.
- [ ] Check that Messaging displays emojis.
- [ ] Check that Messaging displays an MMS preview.
- [ ] Check that Messaging displays the date and time.
- [ ] Check that Messaging displays unread messages.
- [ ] Check that Messaging displays a conversation containing a draft.
- [ ] Update the widget after receiving an SMS.
- [ ] Update the widget after receiving an MMS.
- [ ] Update the widget after sending a message.
- [ ] Update the widget after reading a message.
- [ ] Update the widget after deleting a message.
- [ ] Update the widget after deleting a conversation.
- [ ] Update the widget after archiving a conversation.
- [ ] Update the widget after unarchiving a conversation.
- [ ] Update the widget after pinning a conversation.
- [ ] Update the widget after unpinning a conversation.
- [ ] Update the widget after creating a new conversation.
- [ ] Update the widget after deleting all conversations.
- [ ] Open the corresponding conversation from the widget.
- [ ] Open the app from the widget.
- [ ] Scroll the conversation list within the widget.

### Conversation Widget

- [ ] Add a widget for a specific conversation.
- [ ] Select a conversation with a saved contact.
- [ ] Select a conversation with an unknown phone number.
- [ ] Select a group conversation.
- [ ] Cancel conversation selection.
- [ ] Check that Messaging displays a contact name.
- [ ] Check that Messaging displays an unknown contact's phone number.
- [ ] Check that Messaging displays a contact avatar.
- [ ] Check that Messaging displays a default contact avatar.
- [ ] Check that Messaging displays message history.
- [ ] Check that Messaging displays incoming messages.
- [ ] Check that Messaging displays outgoing messages.
- [ ] Check that Messaging displays long messages.
- [ ] Check that Messaging displays multiline messages.
- [ ] Check that Messaging displays emojis.
- [ ] Check that Messaging displays messages containing attachments.
- [ ] Check that Messaging displays group conversation messages.
- [ ] Update the widget after receiving an SMS.
- [ ] Update the widget after receiving an MMS.
- [ ] Update the widget after sending a message.
- [ ] Update the widget after deleting a message.
- [ ] Update the widget after deleting all messages in the conversation.
- [ ] Update the widget after deleting the associated conversation.
- [ ] Update the widget after archiving the associated conversation.
- [ ] Update the widget after unarchiving the associated conversation.
- [ ] Open the corresponding conversation.
- [ ] Scroll message history within the widget.

### Widget Contact Updates

Perform the following checks for both widget types.

- [ ] Add a name to a contact previously displayed only by phone number.
- [ ] Add an avatar to a contact previously displayed only by phone number.
- [ ] Add a contact name and avatar simultaneously.
- [ ] Change a contact name.
- [ ] Change a contact avatar.
- [ ] Remove a contact avatar.
- [ ] Remove a contact name.
- [ ] Delete a contact.
- [ ] Recreate a previously deleted contact.
- [ ] Update contact information while Messaging is closed.
- [ ] Update contact information after receiving a new message.
- [ ] Update contact information after sending a new message.
- [ ] Display contact information after revoking Contacts permission.
- [ ] Update contact information after granting Contacts permission again.

### Widget Configuration

- [ ] Update widgets while the app is closed.
- [ ] Update widgets with application notifications disabled.
- [ ] Update widgets with notifications disabled for a specific conversation.
- [ ] Update widgets after receiving multiple consecutive messages.
- [ ] Resize the widget horizontally.
- [ ] Resize the widget vertically.
- [ ] Check that Messaging displays the widget at its minimum supported size.
- [ ] Check that Messaging displays the widget at its maximum supported size.
- [ ] Add multiple instances of the same widget type.
- [ ] Add widgets for different conversations.
- [ ] Add multiple widgets for the same conversation.
- [ ] Use both widget types simultaneously.
- [ ] Update widgets belonging to different conversations independently.
- [ ] Remove one widget without affecting other widgets.
- [ ] Remove all widgets.
- [ ] Add a previously removed widget again.
- [ ] Move a widget between home screens.
- [ ] Preserve widgets after restarting the app.
- [ ] Preserve widgets after rebooting the device.

## Settings

### General Settings

- [ ] Open app settings.
- [ ] Check that Messaging displays default settings after a clean installation.
- [ ] Enable outgoing message sounds.
- [ ] Disable outgoing message sounds.
- [ ] Enable in-conversation sounds.
- [ ] Disable in-conversation sounds.
- [ ] Open system notification settings.
- [ ] Open Wireless Emergency Alerts.
- [ ] Open Open Source Licenses.
- [ ] View Open Source Licenses information.
- [ ] Return from nested settings screens.
- [ ] Preserve modified settings after closing the app.
- [ ] Preserve modified settings after rebooting the device.

### SMS / MMS Settings

- [ ] Enable delivery reports.
- [ ] Disable delivery reports.
- [ ] Enable automatic MMS downloading.
- [ ] Disable automatic MMS downloading.
- [ ] Enable automatic MMS downloading while roaming.
- [ ] Disable automatic MMS downloading while roaming.
- [ ] Change the group messaging mode.
- [ ] Check that Messaging displays the self phone number.
- [ ] Enter the self phone number.
- [ ] Edit the self phone number.
- [ ] Clear the self phone number.
- [ ] Enter the self phone number in international format.
- [ ] Enter an invalid self phone number.
- [ ] Cancel changes to the self phone number.
- [ ] Modify settings independently for each SIM.
- [ ] Check that Messaging displays settings without an active SIM.

### Conversation Settings

- [ ] Open settings for an individual conversation.
- [ ] Open settings for a group conversation.
- [ ] Open settings for a conversation with an unknown phone number.
- [ ] Change conversation notification settings.
- [ ] Disable conversation notifications.
- [ ] Re-enable conversation notifications.
- [ ] Preserve settings independently for different conversations.
- [ ] Preserve conversation settings after archiving.
- [ ] Preserve conversation settings after unarchiving.

### Blocked Contacts

- [ ] Block a saved contact.
- [ ] Block an unknown phone number.
- [ ] Block multiple contacts.
- [ ] Block a contact containing multiple phone numbers.
- [ ] Block a contact from an open conversation.
- [ ] Cancel blocking from the confirmation dialog.
- [ ] Open the blocked contact list.
- [ ] Check that Messaging displays an empty blocked contact list.
- [ ] Check that Messaging displays multiple blocked contacts.
- [ ] Check that Messaging displays a blocked contact's name.
- [ ] Check that Messaging displays a blocked unknown phone number.
- [ ] Unblock a contact.
- [ ] Unblock an unknown phone number.
- [ ] Block a previously unblocked contact.
- [ ] Verify the conversation state after blocking.
- [ ] Verify the conversation state after unblocking.
- [ ] Preserve message history after blocking.
- [ ] Preserve message history after unblocking.
- [ ] Change a blocked contact's name.
- [ ] Delete a blocked contact from Contacts.
- [ ] Preserve the blocked contact list after restarting the app.

## SIM And eSIM

### SIM Configurations

- [ ] Launch the app without a SIM.
- [ ] Use the app with one physical SIM.
- [ ] Use the app with one eSIM.
- [ ] Use the app with two active physical SIM cards on a supported device.
- [ ] Use the app with a physical SIM and an eSIM.
- [ ] Use the app with two active eSIMs on a supported device.
- [ ] Use the app with one active and one disabled SIM.
- [ ] Check that Messaging displays active SIM cards.
- [ ] Check that Messaging displays carrier names.
- [ ] Check that Messaging displays custom SIM names.
- [ ] Check that Messaging displays SIM cards with identical names.
- [ ] Check that Messaging displays SIM cards with long names.
- [ ] Check that Messaging displays the SIM selector when multiple SIM cards are active.
- [ ] Check that Messaging displays the interface when only one SIM is active.

### SIM Selection

- [ ] Select a SIM for sending messages.
- [ ] Check that Messaging displays the selected SIM in the conversation.
- [ ] Send an SMS using SIM 1.
- [ ] Send an SMS using SIM 2.
- [ ] Receive an SMS on SIM 1.
- [ ] Receive an SMS on SIM 2.
- [ ] Send an MMS using SIM 1.
- [ ] Send an MMS using SIM 2.
- [ ] Receive an MMS on SIM 1.
- [ ] Receive an MMS on SIM 2.
- [ ] Receive messages on both SIM cards simultaneously.
- [ ] Receive messages from the same sender on different SIM cards.
- [ ] Send messages to the same recipient using different SIM cards.
- [ ] Check that Messaging displays SIM information for incoming messages.
- [ ] Check that Messaging displays SIM information for outgoing messages.
- [ ] Switch SIM cards in an existing conversation.
- [ ] Switch SIM cards while a draft exists.
- [ ] Select SIM cards independently in different conversations.
- [ ] Change the default SIM through system settings.
- [ ] Change the default SIM while a draft exists.
- [ ] Use the app without a configured default SIM.
- [ ] Preserve the selected SIM after reopening a conversation.
- [ ] Send a message using the selected SIM when another SIM is configured as the system default.

### SIM Changes

- [ ] Disable the selected SIM before sending a message.
- [ ] Disable one of two active SIM cards.
- [ ] Disable all SIM cards.
- [ ] Re-enable a SIM.
- [ ] Remove a physical SIM.
- [ ] Replace a physical SIM.
- [ ] Disable an eSIM.
- [ ] Reactivate an eSIM.
- [ ] Change a SIM name.
- [ ] Update the SIM selector after changing a SIM name.
- [ ] Update the SIM selector after disabling a SIM.
- [ ] Update the SIM selector after activating a SIM.
- [ ] Use SIM cards from different carriers.
- [ ] Use SIM cards from different countries.
- [ ] Use a SIM while internationally roaming.
- [ ] Preserve message history after replacing a SIM.
- [ ] Preserve settings after switching SIM cards.
- [ ] Send a message after replacing a SIM.

## User Profiles

### Owner Profile

- [ ] Launch Messaging in the Owner Profile for the first time.
- [ ] Send an SMS.
- [ ] Receive an SMS.
- [ ] Send an MMS.
- [ ] Receive an MMS.
- [ ] Receive notifications.
- [ ] Update the Conversation List Widget.
- [ ] Update the Conversation Widget.
- [ ] Access contacts belonging to the Owner Profile.
- [ ] Use Messaging while Secondary Profiles are active.

### Secondary Profile

- [ ] Launch Messaging in a Secondary Profile for the first time.
- [ ] Use the app with access to phone calls and SMS enabled.
- [ ] Use the app with access to phone calls and SMS disabled.
- [ ] Send an SMS with access enabled.
- [ ] Attempt to send an SMS with access disabled.
- [ ] Receive an SMS with access enabled.
- [ ] Verify SMS receiving behavior with access disabled.
- [ ] Send an MMS with access enabled.
- [ ] Attempt to send an MMS with access disabled.
- [ ] Receive an MMS.
- [ ] Display the MMS downloading restriction in a Secondary Profile.
- [ ] Download an MMS through the Owner Profile.
- [ ] Check that Messaging displays an MMS after downloading it in the Owner Profile.
- [ ] Receive SMS notifications.
- [ ] Receive MMS notifications.
- [ ] Reply using Quick Reply.
- [ ] Add the Conversation List Widget.
- [ ] Add the Conversation Widget.
- [ ] Update the Conversation List Widget after receiving a message.
- [ ] Update the Conversation Widget after receiving a message.
- [ ] Access contacts belonging to the Secondary Profile.
- [ ] Check that Messaging displays a sender saved only in the Secondary Profile.
- [ ] Check that Messaging displays a sender saved only in the Owner Profile.
- [ ] Check that Messaging displays a sender with different contact names in different profiles.
- [ ] Change profile permissions after launching Messaging.
- [ ] Re-enable access to phone calls and SMS.
- [ ] Use Messaging in a Secondary Profile with one SIM.
- [ ] Use Messaging in a Secondary Profile with two SIM cards.

### Profile Switching

- [ ] Switch from the Owner Profile to a Secondary Profile.
- [ ] Switch from a Secondary Profile to the Owner Profile.
- [ ] Receive an SMS while the Owner Profile is active.
- [ ] Receive an SMS while a Secondary Profile is active.
- [ ] Receive an SMS while a profile is running in the background.
- [ ] Receive an MMS while a Secondary Profile is active.
- [ ] Receive an MMS while a profile is running in the background.
- [ ] Switch profiles while a draft exists.
- [ ] Switch profiles while sending a message.
- [ ] Switch profiles while receiving a message.
- [ ] Check that Messaging displays message history after switching profiles.
- [ ] Check that Messaging displays contacts after switching profiles.
- [ ] Receive notifications after switching profiles.
- [ ] Update widgets after switching profiles.
- [ ] Use Messaging with multiple Secondary Profiles.
- [ ] Receive a message while multiple profiles are active.
- [ ] Use profiles with different phone call and SMS permissions.
- [ ] End a Secondary Profile session.
- [ ] Restart a previously ended profile session.
- [ ] Receive messages while a profile session is ended.
- [ ] Check that Messaging displays messages after restarting a previously ended profile session.
- [ ] Delete a Secondary Profile without affecting Owner Profile data.

## System Integration

### Android Share

- [ ] Share text from an external application to Messaging.
- [ ] Share an image.
- [ ] Share multiple images.
- [ ] Share a video.
- [ ] Share an audio file.
- [ ] Share a contact.
- [ ] Share a hyperlink.
- [ ] Share text and an attachment.
- [ ] Share multiple attachments of different types.
- [ ] Check that Messaging displays the recipient selection screen.
- [ ] Check that Messaging displays recent conversations.
- [ ] Check that Messaging displays the recipient selection screen when no conversations exist.
- [ ] Search for a recipient on the Share screen.
- [ ] Select an existing conversation.
- [ ] Select a new recipient.
- [ ] Select a group conversation.
- [ ] Select multiple recipients.
- [ ] Deselect one recipient.
- [ ] Deselect all recipients.
- [ ] Check that Messaging displays the preview screen.
- [ ] Edit text before sending.
- [ ] Remove an attachment before sending.
- [ ] Confirm sending shared content.
- [ ] Cancel sending shared content.
- [ ] Share an unsupported file.
- [ ] Share an inaccessible file.
- [ ] Share multiple files containing unsupported file types.
- [ ] Share content to a conversation containing an existing draft.
- [ ] Reopen Messaging through Android Share.

### Android Intents and External Apps

- [ ] Open Messaging from Contacts.
- [ ] Open Messaging from Phone.
- [ ] Open Messaging using an SMS URI.
- [ ] Open Messaging with a prefilled phone number.
- [ ] Open Messaging with prefilled message text.
- [ ] Open Messaging with a prefilled phone number and message text.
- [ ] Open Messaging with multiple recipients.
- [ ] Open Messaging with an invalid phone number.
- [ ] Open an existing conversation using an external Intent.
- [ ] Open a new conversation using an external Intent.
- [ ] Open Messaging using an external Intent while a draft exists.
- [ ] Initiate a call to a saved contact from Messaging.
- [ ] Initiate a call to an unknown phone number from Messaging.
- [ ] Open Phone with the corresponding phone number.
- [ ] Open an existing contact from Messaging.
- [ ] Return to Messaging from an external application.
- [ ] Verify that messages are not sent automatically without user confirmation.

## Security And Privacy

### Link Handling

#### Link Detection

- [ ] Detect an HTTP hyperlink.
- [ ] Detect an HTTPS hyperlink.
- [ ] Detect a hyperlink without an explicitly specified protocol.
- [ ] Detect a hyperlink containing www.
- [ ] Detect a shortened hyperlink.
- [ ] Detect multiple hyperlinks in one message.
- [ ] Detect a hyperlink at the beginning of a message.
- [ ] Detect a hyperlink in the middle of a message.
- [ ] Detect a hyperlink at the end of a message.
- [ ] Detect a hyperlink containing query parameters.
- [ ] Detect a hyperlink containing Unicode characters.
- [ ] Detect a hyperlink adjacent to punctuation.
- [ ] Handle a malformed hyperlink.
- [ ] Check that Messaging displays a long hyperlink.

#### Link Opening

- [ ] Open a hyperlink from an incoming SMS.
- [ ] Open a hyperlink from an outgoing SMS.
- [ ] Open a hyperlink from an MMS.
- [ ] Open a hyperlink from a group message.
- [ ] Open each hyperlink in a message containing multiple hyperlinks.
- [ ] Open a hyperlink in the default browser.
- [ ] Open a hyperlink after changing the default browser.
- [ ] Open a hyperlink without a configured default browser.
- [ ] Attempt to open a hyperlink when no suitable application is available.
- [ ] Open a hyperlink without internet connectivity.
- [ ] Pass the complete URL without losing query parameters.
- [ ] Open the same hyperlink repeatedly.
- [ ] Return to the original conversation after opening a hyperlink.
- [ ] Preserve a draft when opening a hyperlink.
- [ ] Copy a hyperlink to the clipboard.
- [ ] Copy a long hyperlink without losing characters.

#### Link Opening Settings

- [ ] Verify the default link opening mode.
- [ ] Switch between available link opening modes.

##### Ask before opening

- [ ] Check that Messaging displays a confirmation dialog when tapping a hyperlink.
- [ ] Check that Messaging displays the complete hyperlink address in the confirmation dialog.
- [ ] Confirm hyperlink opening.
- [ ] Cancel hyperlink opening.
- [ ] Dismiss the confirmation dialog using system Back navigation.
- [ ] Tap the hyperlink again after cancelling.
- [ ] Verify that the hyperlink is not opened without confirmation.

##### Open on tap

- [ ] Open a hyperlink with a single tap.
- [ ] Open a hyperlink without additional confirmation.

##### Do not open

- [ ] Tap a hyperlink while link opening is disabled.
- [ ] Verify that the browser is not opened.
- [ ] Copy a hyperlink while link opening is disabled.
- [ ] Verify that hyperlinks are not opened automatically.

#### Link Security

- [ ] Handle hyperlinks containing unsupported URI schemes.
- [ ] Handle malformed URLs.
- [ ] Handle hyperlinks containing redirects.
- [ ] Handle hyperlinks containing an IP address instead of a domain name.
- [ ] Handle hyperlinks containing a non-standard port.
- [ ] Handle hyperlinks containing encoded characters.
- [ ] Verify that hyperlinks are not opened automatically when receiving an SMS.
- [ ] Verify that hyperlinks are not opened automatically when viewing a message.
- [ ] Apply the updated link opening mode to previously received messages.
- [ ] Apply the updated link opening mode to newly received messages.
- [ ] Preserve the selected link opening mode after restarting the app.

### YouTube Link Previews

- [ ] Verify the default YouTube link preview setting.
- [ ] Receive a YouTube hyperlink with previews disabled.
- [ ] Receive a YouTube hyperlink with previews enabled.
- [ ] Send a YouTube hyperlink with previews enabled.
- [ ] Check that Messaging displays a preview for a valid YouTube hyperlink.
- [ ] Check that Messaging displays a preview for a shortened YouTube hyperlink.
- [ ] Check that Messaging displays previews for multiple YouTube hyperlinks in one message.
- [ ] Handle an unavailable YouTube video.
- [ ] Handle a deleted YouTube video.
- [ ] Handle a private YouTube video.
- [ ] Handle a preview loading failure.
- [ ] Receive a YouTube hyperlink without network connectivity.
- [ ] Disable YouTube link previews after previously enabling them.
- [ ] Verify previously loaded previews after disabling the setting.
- [ ] Verify that no new preview network requests are made while previews are disabled.

### Permissions and Data Privacy

- [ ] Verify that unauthorized contact access is prevented after revoking Contacts permission.
- [ ] Access media files according to granted permissions.
- [ ] Verify that the camera is accessed only during the corresponding user action.
- [ ] Check that Messaging displays notification content when lock screen content visibility is enabled.
- [ ] Hide sensitive notification content on the lock screen.
- [ ] Suppress lock screen notifications according to system settings.
- [ ] Verify contact isolation between user profiles.
- [ ] Verify app data isolation according to user profile restrictions.
- [ ] Handle corrupted URIs.
- [ ] Handle malformed URIs.
- [ ] Handle URIs without granted read permission.
- [ ] Verify that arbitrary files cannot be accessed through external Intents.
- [ ] Verify that message content and personal data are not unnecessarily exposed in diagnostic logs.

## Performance And Stability

### Large Data Sets

- [ ] Launch the app containing 100 messages.
- [ ] Launch the app containing 1,000 messages.
- [ ] Launch the app containing 10,000 messages.
- [ ] Open a conversation containing 100 messages.
- [ ] Open a conversation containing 1,000 messages.
- [ ] Open a conversation containing 10,000 messages.
- [ ] Open a conversation list containing 100 conversations.
- [ ] Open a conversation list containing 1,000 conversations.
- [ ] Scroll a large conversation list.
- [ ] Scroll a large message history.
- [ ] Scroll message history rapidly.
- [ ] Scroll message history while receiving new messages.
- [ ] Open a contact list containing a large number of contacts.
- [ ] Search within a large contact list.
- [ ] Open a conversation containing a large number of attachments.
- [ ] Open an archive containing a large number of conversations.
- [ ] Archive a large number of conversations.
- [ ] Delete a large number of conversations.
- [ ] Delete a large number of messages.

### Load and Stability

- [ ] Receive a large number of SMS messages within a short period.
- [ ] Receive a large number of MMS messages within a short period.
- [ ] Receive messages from a large number of senders simultaneously.
- [ ] Send a large number of consecutive messages.
- [ ] Send and receive a large number of messages simultaneously.
- [ ] Process multiple large images.
- [ ] Open and close conversations repeatedly.
- [ ] Open and close Media Viewer repeatedly.
- [ ] Open and close the attachment panel repeatedly.
- [ ] Switch between conversations repeatedly.
- [ ] Use the app for an extended period without restarting.
- [ ] Monitor memory usage during prolonged scrolling.
- [ ] Monitor memory usage while viewing media files.
- [ ] Verify memory release after closing Media Viewer.
- [ ] Monitor battery consumption while the app is running in the background.
- [ ] Monitor resource consumption with multiple active widgets.

### Low Resources

- [ ] Launch the app under low-memory conditions.
- [ ] Open a large conversation under low-memory conditions.
- [ ] Open a large image under low-memory conditions.
- [ ] Launch the app with insufficient available storage.
- [ ] Create a draft with insufficient available storage.
- [ ] Send an SMS when device storage is full.
- [ ] Receive an SMS when device storage is full.
- [ ] Download an MMS when device storage is full.
- [ ] Capture a photo when device storage is full.
- [ ] Record audio when device storage is full.
- [ ] Save an attachment when device storage is full.
- [ ] Restore application functionality after freeing storage space.
- [ ] Preserve previously received messages after resource-related failures.

## Accessibility And Localization

### Accessibility

- [ ] Open conversations using TalkBack.
- [ ] Create a new conversation using TalkBack.
- [ ] Select a recipient using TalkBack.
- [ ] Send a message using TalkBack.
- [ ] Add an attachment using TalkBack.
- [ ] Verify TalkBack announcements for conversation list items.
- [ ] Verify TalkBack announcements for unread conversations.
- [ ] Verify TalkBack announcements for incoming messages.
- [ ] Verify TalkBack announcements for outgoing messages.
- [ ] Verify TalkBack announcements for sender names.
- [ ] Verify TalkBack announcements for message statuses.
- [ ] Verify TalkBack announcements for attachments.
- [ ] Verify TalkBack announcements for buttons without text labels.
- [ ] Verify TalkBack announcements for input fields.
- [ ] Verify TalkBack announcements for dialogs.
- [ ] Verify TalkBack announcements for error messages.
- [ ] Verify TalkBack announcements for the selected SIM.
- [ ] Verify TalkBack announcements for the number of selected items.
- [ ] Verify the focus navigation order.
- [ ] Restore focus after closing a dialog.
- [ ] Restore focus after closing a nested screen.
- [ ] Verify accessibility of interactive elements using TalkBack.
- [ ] Verify touch target sizes for interactive elements.
- [ ] Verify text and control contrast.
- [ ] Check that Messaging displays the app using the minimum system font size.
- [ ] Check that Messaging displays the app using the maximum system font size.
- [ ] Check that Messaging displays the app using the minimum system display size.
- [ ] Check that Messaging displays the app using the maximum system display size.
- [ ] Check that Messaging displays the app using the maximum font and display sizes simultaneously.
- [ ] Verify that elements do not overlap when increasing the font size.
- [ ] Verify button accessibility when increasing the font size.
- [ ] Verify text input field accessibility when increasing the font size.

### Localization

- [ ] Check that Messaging displays the app in English.
- [ ] Check that Messaging displays the app in Russian.
- [ ] Check that Messaging displays the app in other supported languages.
- [ ] Change the system language while the app is open.
- [ ] Check that Messaging displays long localized strings.
- [ ] Verify that localized text is not truncated.
- [ ] Verify that supported localizations do not contain untranslated strings.
- [ ] Check that Messaging displays the RTL interface.
- [ ] Check that Messaging displays RTL messages.
- [ ] Check that Messaging displays messages containing mixed text directions.
- [ ] Check that Messaging displays numbers within RTL messages.
- [ ] Check that Messaging displays phone numbers within the RTL interface.
- [ ] Check that Messaging displays localized date formats.
- [ ] Check that Messaging displays timestamps using the 12-hour format.
- [ ] Check that Messaging displays timestamps using the 24-hour format.
- [ ] Check that Messaging displays timestamps after changing the system time zone.
- [ ] Check that Messaging displays dates after changing the system date.
- [ ] Check that Messaging displays messages received before and after changing the time zone.
- [ ] Check that Messaging displays phone numbers according to regional settings.

## Compatibility

### Screen and Layout

- [ ] Check that Messaging displays the app on devices with small screens.
- [ ] Check that Messaging displays the app on devices with large screens.
- [ ] Check that Messaging displays the app on tablets.
- [ ] Check that Messaging displays the app on foldable devices when available.
- [ ] Check that Messaging displays the app in portrait orientation.
- [ ] Check that Messaging displays the app in landscape orientation.
- [ ] Rotate the device while the conversation list is open.
- [ ] Rotate the device while a conversation is open.
- [ ] Rotate the device while the recipient selection screen is open.
- [ ] Rotate the device while the keyboard is open.
- [ ] Rotate the device while a draft exists.
- [ ] Rotate the device while selecting an attachment.
- [ ] Rotate the device while the camera is open.
- [ ] Rotate the device while Media Viewer is open.
- [ ] Rotate the device during video playback.
- [ ] Rotate the device while a dialog is open.
- [ ] Rotate the device while message selection mode is active.
- [ ] Rotate the device while conversation selection mode is active.
- [ ] Rotate the device while settings are open.
- [ ] Check that Messaging displays the two-pane layout.
- [ ] Select a conversation in the two-pane layout.
- [ ] Check that Messaging displays the two-pane layout without a selected conversation.
- [ ] Switch between single-pane and two-pane layouts.
- [ ] Preserve the selected conversation when resizing the app window.
- [ ] Preserve drafts when resizing the app window.
- [ ] Preserve selected items when resizing the app window.
- [ ] Use the app in Split Screen mode, if supported by the system.
- [ ] Resize the app window in Split Screen mode.
- [ ] Return from Split Screen mode to full-screen mode.
- [ ] Verify that application elements are not obscured by the system status bar.
- [ ] Verify that application elements are not obscured by the system navigation bar.

### System Navigation

- [ ] Use the app with gesture navigation.
- [ ] Use the app with three-button navigation.
- [ ] Navigate back from the conversation list.
- [ ] Navigate back from an open conversation.
- [ ] Navigate back from the recipient selection screen.
- [ ] Navigate back while the keyboard is open.
- [ ] Navigate back while the attachment panel is open.
- [ ] Navigate back from Media Viewer.
- [ ] Navigate back from settings.
- [ ] Navigate back from the archive.
- [ ] Navigate back from Message Details.
- [ ] Navigate back while a dialog is open.
- [ ] Navigate back while message selection mode is active.
- [ ] Navigate back while conversation selection mode is active.
- [ ] Complete a Predictive Back gesture.
- [ ] Cancel a Predictive Back gesture.
- [ ] Complete a Predictive Back gesture while the keyboard is open.
- [ ] Cancel a Predictive Back gesture while a draft exists.
- [ ] Verify that system navigation gestures do not conflict with application horizontal swipe gestures.
- [ ] Change the system navigation mode while the app is running.

### Theme and Keyboard

- [ ] Check that Messaging displays the app in light theme.
- [ ] Check that Messaging displays the app in dark theme.
- [ ] Change the system theme while the app is open.
- [ ] Change the system theme while a draft exists.
- [ ] Change the system theme while the keyboard is open.
- [ ] Change the system theme while a dialog is open.
- [ ] Change the system theme while Media Viewer is open.
- [ ] Check that Messaging displays the Conversation List Widget in light and dark themes.
- [ ] Check that Messaging displays the Conversation Widget in light and dark themes.
- [ ] Update widget themes after changing the system theme.
- [ ] Use the default GrapheneOS keyboard.
- [ ] Use an alternative keyboard.
- [ ] Switch keyboards while a conversation is open.
- [ ] Use keyboards with different heights.
- [ ] Use a floating keyboard, if supported by the keyboard application.
- [ ] Use a physical keyboard when available.

## Updates And Data Persistence

### Application Update

- [ ] Update the app from a previous supported version.
- [ ] Update the app containing existing SMS history.
- [ ] Update the app containing existing MMS history.
- [ ] Update the app containing a large number of conversations.
- [ ] Preserve incoming messages after updating the app.
- [ ] Preserve outgoing messages after updating the app.
- [ ] Preserve message statuses after updating the app.
- [ ] Preserve attachments after updating the app.
- [ ] Preserve group conversations after updating the app.
- [ ] Preserve drafts after updating the app.
- [ ] Preserve pinned conversations after updating the app.
- [ ] Preserve archived conversations after updating the app.
- [ ] Preserve the blocked contact list after updating the app.
- [ ] Preserve read and unread states after updating the app.
- [ ] Preserve app settings after updating the app.
- [ ] Preserve SIM settings after updating the app.
- [ ] Preserve individual conversation settings after updating the app.
- [ ] Preserve Snooze states after updating the app.
- [ ] Preserve Privacy settings after updating the app.
- [ ] Preserve the Conversation List Widget after updating the app.
- [ ] Preserve the Conversation Widget after updating the app.
- [ ] Preserve the selected conversation in the Conversation Widget.
- [ ] Update widgets after receiving a new message on the updated app version.
- [ ] Send an SMS after updating the app.
- [ ] Receive an SMS after updating the app.
- [ ] Send an MMS after updating the app.
- [ ] Receive an MMS after updating the app.
- [ ] Verify that no messages are lost after updating the app.
- [ ] Verify that no messages are duplicated after updating the app.

### OS Update

- [ ] Update GrapheneOS with Messaging installed.
- [ ] Preserve SMS history after updating GrapheneOS.
- [ ] Preserve MMS history after updating GrapheneOS.
- [ ] Preserve drafts after updating GrapheneOS.
- [ ] Preserve Messaging settings after updating GrapheneOS.
- [ ] Preserve widgets after updating GrapheneOS.
- [ ] Preserve Messaging data in Secondary Profiles after updating GrapheneOS.
- [ ] Send an SMS after updating GrapheneOS.
- [ ] Receive an SMS after updating GrapheneOS.
- [ ] Send an MMS after updating GrapheneOS.
- [ ] Receive an MMS after updating GrapheneOS.

### Data Synchronization

- [ ] Synchronize message history after temporarily using another SMS application.
- [ ] Check that Messaging displays messages received through another SMS application.
- [ ] Check that Messaging displays messages sent through another SMS application.
- [ ] Update message history after deleting messages through another SMS application.
- [ ] Update read and unread states after using another SMS application.
- [ ] Verify that messages are not duplicated after synchronization.
- [ ] Preserve message associations with their corresponding conversations after synchronization.

## Interruptions And Recovery

### Calls

- [ ] Receive an incoming call while composing a message.
- [ ] Receive an incoming call while sending an SMS.
- [ ] Receive an incoming call while sending an MMS.
- [ ] Receive an incoming call while downloading an MMS.
- [ ] Receive an incoming call while recording audio.
- [ ] Receive an incoming call during audio playback.
- [ ] Receive an incoming call during video playback.
- [ ] Initiate an outgoing call while a draft exists.
- [ ] Receive an SMS during a phone call.
- [ ] Receive an MMS during a phone call.
- [ ] Return to Messaging after ending a phone call.
- [ ] Return to Messaging after declining an incoming call.

### Screen Lock

- [ ] Lock the device while composing a message.
- [ ] Lock the device while sending an SMS.
- [ ] Lock the device while sending an MMS.
- [ ] Lock the device while downloading an MMS.
- [ ] Lock the device while recording audio.
- [ ] Lock the device during audio playback.
- [ ] Lock the device during video playback.
- [ ] Unlock the device after receiving a message.
- [ ] Restore the previously opened conversation after unlocking the device.

### Application Lifecycle

- [ ] Minimize the app while sending an SMS.
- [ ] Minimize the app while sending an MMS.
- [ ] Minimize the app while downloading an MMS.
- [ ] Minimize the app while processing an attachment.
- [ ] Minimize the app while recording audio.
- [ ] Close the app while sending a message.
- [ ] Close the app while downloading an MMS.
- [ ] Close the app while processing an attachment.
- [ ] Force-stop the app while sending an SMS.
- [ ] Force-stop the app while sending an MMS.
- [ ] Force-stop the app while downloading an MMS.
- [ ] Force-stop the app while processing an attachment.
- [ ] Terminate the app process while a draft exists.
- [ ] Reboot the device while sending a message.
- [ ] Reboot the device while downloading an MMS.
- [ ] Restore application functionality after an application crash.

### Network Interruptions

- [ ] Lose cellular network coverage while sending an SMS.
- [ ] Lose cellular network coverage while sending an MMS.
- [ ] Lose cellular network coverage while downloading an MMS.
- [ ] Restore cellular network coverage after a sending failure.
- [ ] Restore cellular network coverage after a downloading failure.
- [ ] Disable mobile data while sending an MMS.
- [ ] Disable mobile data while downloading an MMS.
- [ ] Re-enable mobile data.
- [ ] Switch between Wi-Fi and mobile data while sending a message.
- [ ] Switch between Wi-Fi and mobile data while downloading an MMS.
- [ ] Enable airplane mode while sending an SMS.
- [ ] Enable airplane mode while sending an MMS.
- [ ] Enable airplane mode while downloading an MMS.
- [ ] Disable airplane mode after a sending failure.
- [ ] Disable airplane mode after a downloading failure.
- [ ] Repeatedly lose and restore cellular network coverage.

### SIM Interruptions

- [ ] Disable a SIM while sending an SMS.
- [ ] Disable a SIM while sending an MMS.
- [ ] Disable a SIM while downloading an MMS.
- [ ] Change the default SIM while sending a message.
- [ ] Switch the active mobile data SIM while downloading an MMS.
- [ ] Reactivate a SIM after a sending failure.
- [ ] Reactivate a SIM after a downloading failure.

### Permission Interruptions

- [ ] Revoke Camera permission while working with attachments.
- [ ] Revoke Microphone permission while recording audio.
- [ ] Revoke media access permission while selecting an attachment.
- [ ] Revoke Contacts permission while the recipient selection screen is open.
- [ ] Revoke Notifications permission while unread messages exist.
- [ ] Change the default SMS application while sending a message.
- [ ] Restore the affected feature after granting permission again.

### Recovery

- [ ] Preserve previously received messages after interrupted operations.
- [ ] Preserve previously sent messages after interrupted operations.
- [ ] Check that Messaging displays the actual status of an interrupted sending operation.
- [ ] Check that Messaging displays the actual status of an interrupted downloading operation.
- [ ] Verify that attachments are not corrupted after recovery.
- [ ] Verify that messages are not unintentionally resent after recovery.
- [ ] Verify that messages are not duplicated after recovery.

## TODO / Future Test Coverage

Checks in this section are excluded from mandatory regression testing until the affected feature is implemented or further investigation is completed.

### Planned Features

- [ ] Search within message history.
- [ ] Filter the conversation list.
- [ ] Block all unknown senders.
- [ ] Check that Messaging displays favorite contacts at the top of the recipient list.

### Additional Test Coverage

- [ ] Extend boundary value coverage.
- [ ] Extend negative scenario coverage.
- [ ] Extend edge case coverage.
- [ ] Add regression checks based on identified defects.
- [ ] Add checks for new functionality after it is merged into main.