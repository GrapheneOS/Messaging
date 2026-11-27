# Snooze
---

## Snooze notifications for 1 hour

### Preconditions

- Two test conversations exist; notifications are enabled and neither is blocked or snoozed.

### Steps

1. Open the target conversation notification settings.
2. Choose Snooze and select 1 hour.
3. Receive an SMS in the target conversation while snoozed.
4. Receive an SMS in the other conversation.

### Expected Result

- The target conversation is snoozed for 1 hour.
- The other conversation remains independently configured. Verify notification suppression on device.

### Source reference

`SnoozeOption.kt`: OneHour, EightHours, TwentyFourHours, Always; `ConversationSnoozeQuery.kt`.
---

## Snooze notifications for 8 hours

### Preconditions

- Two test conversations exist; notifications are enabled and neither is blocked or snoozed.

### Steps

1. Open the target conversation notification settings.
2. Choose Snooze and select 8 hours.
3. Receive an SMS in the target conversation while snoozed.
4. Receive an SMS in the other conversation.

### Expected Result

- The target conversation is snoozed for 8 hours.
- The other conversation remains independently configured. Verify notification suppression on device.

### Source reference

`SnoozeOption.kt`: OneHour, EightHours, TwentyFourHours, Always; `ConversationSnoozeQuery.kt`.
---

## Snooze notifications for 24 hours

### Preconditions

- Two test conversations exist; notifications are enabled and neither is blocked or snoozed.

### Steps

1. Open the target conversation notification settings.
2. Choose Snooze and select 24 hours.
3. Receive an SMS in the target conversation while snoozed.
4. Receive an SMS in the other conversation.

### Expected Result

- The target conversation is snoozed for 24 hours.
- The other conversation remains independently configured. Verify notification suppression on device.

### Source reference

`SnoozeOption.kt`: OneHour, EightHours, TwentyFourHours, Always; `ConversationSnoozeQuery.kt`.
---

## Set Snooze to Always

### Preconditions

- Two test conversations exist; notifications are enabled and neither is blocked or snoozed.

### Steps

1. Open the target conversation notification settings.
2. Choose Snooze and select Always.
3. Receive an SMS in the target conversation while snoozed.
4. Receive an SMS in the other conversation.

### Expected Result

- The target conversation is snoozed for Always.
- The other conversation remains independently configured. Verify notification suppression on device.

### Source reference

`SnoozeOption.kt`: OneHour, EightHours, TwentyFourHours, Always; `ConversationSnoozeQuery.kt`.
---

## Cancel Snooze

### Preconditions

- Use a test sender; configure notification permission and channel unless the title changes them.

### Steps

1. Open Messaging and prepare the conversation and notification state in the title.
2. Cancel Snooze.
3. Check the Android notification shade and the corresponding conversation; compare sender, content and target with the test message.

### Expected Result

- Notification behavior reflects the configured permission, channel and conversation state; any notification action targets the correct conversation.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Change the Snooze duration

### Preconditions

- Use a test sender; configure notification permission and channel unless the title changes them.

### Steps

1. Open Messaging and prepare the conversation and notification state in the title.
2. Change the Snooze duration.
3. Check the Android notification shade and the corresponding conversation; compare sender, content and target with the test message.

### Expected Result

- Notification behavior reflects the configured permission, channel and conversation state; any notification action targets the correct conversation.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive a message while Snooze is active

### Preconditions

- Use a test sender; configure notification permission and channel unless the title changes them.

### Steps

1. Open Messaging and prepare the conversation and notification state in the title.
2. Receive a message while Snooze is active.
3. Check the Android notification shade and the corresponding conversation; compare sender, content and target with the test message.

### Expected Result

- Notification behavior reflects the configured permission, channel and conversation state; any notification action targets the correct conversation.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive a message after Snooze expires

### Preconditions

- Use a test sender; configure notification permission and channel unless the title changes them.

### Steps

1. Open Messaging and prepare the conversation and notification state in the title.
2. Receive a message after Snooze expires.
3. Check the Android notification shade and the corresponding conversation; compare sender, content and target with the test message.

### Expected Result

- Notification behavior reflects the configured permission, channel and conversation state; any notification action targets the correct conversation.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive a message after manually cancelling Snooze

### Preconditions

- Use a test sender; configure notification permission and channel unless the title changes them.

### Steps

1. Open Messaging and prepare the conversation and notification state in the title.
2. Receive a message after manually cancelling Snooze.
3. Check the Android notification shade and the corresponding conversation; compare sender, content and target with the test message.

### Expected Result

- Notification behavior reflects the configured permission, channel and conversation state; any notification action targets the correct conversation.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Apply Snooze independently to multiple conversations

### Preconditions

- Use a test sender; configure notification permission and channel unless the title changes them.

### Steps

1. Open Messaging and prepare the conversation and notification state in the title.
2. Apply Snooze independently to multiple conversations.
3. Check the Android notification shade and the corresponding conversation; compare sender, content and target with the test message.

### Expected Result

- Notification behavior reflects the configured permission, channel and conversation state; any notification action targets the correct conversation.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Preserve Snooze settings after restarting the application

### Preconditions

- Use a test sender; configure notification permission and channel unless the title changes them.

### Steps

1. Open Messaging and prepare the conversation and notification state in the title.
2. Preserve Snooze settings after restarting the application.
3. Check the Android notification shade and the corresponding conversation; compare sender, content and target with the test message.

### Expected Result

- Notification behavior reflects the configured permission, channel and conversation state; any notification action targets the correct conversation.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Preserve Snooze settings after rebooting the device

### Preconditions

- Use a test sender; configure notification permission and channel unless the title changes them.

### Steps

1. Open Messaging and prepare the conversation and notification state in the title.
2. Preserve Snooze settings after rebooting the device.
3. Check the Android notification shade and the corresponding conversation; compare sender, content and target with the test message.

### Expected Result

- Notification behavior reflects the configured permission, channel and conversation state; any notification action targets the correct conversation.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
