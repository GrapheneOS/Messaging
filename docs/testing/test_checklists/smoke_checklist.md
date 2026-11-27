# Messaging - Smoke Checklist

## Test Environment

- Application build:
- Main commit:
- OS build:
- Device:
- SIM configuration:
- User profile:

## Application

- [ ] SMK-001 - Open Messaging and check that it launches without errors.
- [ ] SMK-002 - Complete onboarding on a clean installation.
- [ ] SMK-003 - Set Messaging as the default SMS application.
- [ ] SMK-004 - Grant required permissions.
- [ ] SMK-005 - Open the conversation list without errors.
- [ ] SMK-006 - Open an existing conversation.
- [ ] SMK-007 - Navigate back to the conversation list.

## Contacts and Conversations

- [ ] SMK-008 - Create a conversation with a saved contact.
- [ ] SMK-009 - Create a conversation using a manually entered phone number.
- [ ] SMK-010 - Find a recipient by contact name.
- [ ] SMK-011 - Verify contact name and avatar display.
- [ ] SMK-012 - Verify existing message history.

## SMS

- [ ] SMK-013 - Send a standard text SMS.
- [ ] SMK-014 - Receive a standard text SMS.
- [ ] SMK-015 - Send a Unicode SMS.
- [ ] SMK-016 - Send a multipart SMS.
- [ ] SMK-017 - Receive a multipart SMS.
- [ ] SMK-018 - Verify outgoing message status.
- [ ] SMK-019 - Check that received message content matches the original.
- [ ] SMK-020 - Verify conversation list updates after sending and receiving messages.
- [ ] SMK-021 - Verify failed message handling and retry.

## MMS

- [ ] SMK-022 - Attach an image to a message.
- [ ] SMK-023 - Send an MMS with an image.
- [ ] SMK-024 - Receive and download an MMS.
- [ ] SMK-025 - Open a received media attachment.
- [ ] SMK-026 - Verify automatic MMS retrieval when enabled.

## Message Management

- [ ] SMK-027 - Copy message text.
- [ ] SMK-028 - Delete a message.
- [ ] SMK-029 - Delete a conversation.
- [ ] SMK-030 - Save and restore a message draft.

## Notifications

- [ ] SMK-031 - Receive an SMS notification while Messaging is in the background.
- [ ] SMK-032 - Open the correct conversation from a notification.
- [ ] SMK-033 - Verify notification clearing after reading the message.
- [ ] SMK-034 - Receive an MMS notification.

## Widgets

- [ ] SMK-035 - Add the conversation list widget.
- [ ] SMK-036 - Add a single conversation widget.
- [ ] SMK-037 - Verify widget updates after receiving a message.
- [ ] SMK-038 - Open the correct conversation from a widget.

## Settings and SIM

- [ ] SMK-039 - Open application settings.
- [ ] SMK-040 - Change an available setting and verify persistence.
- [ ] SMK-041 - Verify SMS communication using the active SIM.
- [ ] SMK-042 - Select a different SIM and send a message when multiple SIMs are available.

## Stability and Persistence

- [ ] SMK-043 - Background and resume the application.
- [ ] SMK-044 - Restart the application and verify message history.
- [ ] SMK-045 - Reboot the device and verify message history.
- [ ] SMK-046 - Verify message reception after reboot and required device unlock.
- [ ] SMK-047 - Verify that no crash or ANR occurs during smoke execution.

## Conditional Smoke Checks

- [ ] SMK-048 - Verify group messaging.
- [ ] SMK-049 - Verify SMS communication in an authorized secondary user profile.
- [ ] SMK-050 - Verify profile-specific message notifications.

## Result

- Status:
- Failed scenarios:
- Blocked scenarios:
- Related issues:

Conditional scenarios are executed only when the required feature and environment are available.

A build must not be considered smoke-tested successfully if critical communication or data integrity checks fail.