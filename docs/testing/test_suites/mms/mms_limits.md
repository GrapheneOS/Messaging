# Mms Limits
---

## Send an attachment smaller than the configured size limit

### Preconditions

- Messaging is the default SMS app; an active MMS-capable SIM and a test recipient are available.
- Open an individual conversation. Prepare supported media.
- Use an instrumented fixture or inspect the mapped minimum sending size; raw file size alone is insufficient.

### Steps

1. Prepare a mapped attachment whose minimum sending size is below the resolved MMS size limit.
2. Keep attachment count within the resolved limit and tap Send.
3. Check the validation outcome.

### Expected Result

- The size validation does not reject the message.
- Actual delivery remains carrier-dependent.

### Source reference

`SendConversationDraft.kt`: validateVideoAttachmentLimit / validateMappedMessageForSend; `MmsUtils.java`: MAX_VIDEO_ATTACHMENT_COUNT; `ConversationDraftDelegateSendValidationTest.kt`.
---

## Send an attachment equal to the configured size limit

### Preconditions

- Messaging is the default SMS app; an active MMS-capable SIM and a test recipient are available.
- Open an individual conversation. Prepare supported media.
- Use an instrumented fixture or inspect the mapped minimum sending size; raw file size alone is insufficient.

### Steps

1. Prepare a mapped attachment whose minimum sending size equals the resolved MMS size limit.
2. Keep attachment count within the resolved limit and tap Send.
3. Check the validation outcome.

### Expected Result

- The strict greater-than size check does not reject the message.
- Actual delivery remains carrier-dependent.

### Source reference

`SendConversationDraft.kt`: validateVideoAttachmentLimit / validateMappedMessageForSend; `MmsUtils.java`: MAX_VIDEO_ATTACHMENT_COUNT; `ConversationDraftDelegateSendValidationTest.kt`.
---

## Attempt to send an attachment exceeding the configured size limit

### Preconditions

- Messaging is the default SMS app; an active MMS-capable SIM and a test recipient are available.
- Open an individual conversation. Prepare supported media.
- Use an instrumented fixture or inspect the mapped minimum sending size; raw file size alone is insufficient.

### Steps

1. Prepare a mapped attachment whose minimum sending size exceeds the resolved MMS size limit.
2. Keep attachment count within the resolved limit and tap Send.
3. Check the validation outcome.

### Expected Result

- The message limit warning is displayed before dispatch.
- Actual delivery remains carrier-dependent.

### Source reference

`SendConversationDraft.kt`: validateVideoAttachmentLimit / validateMappedMessageForSend; `MmsUtils.java`: MAX_VIDEO_ATTACHMENT_COUNT; `ConversationDraftDelegateSendValidationTest.kt`.
---

## Add fewer attachments than the configured attachment count limit

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Add fewer attachments than the configured attachment count limit.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Add the maximum allowed number of attachments

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Add the maximum allowed number of attachments.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Exceed the allowed attachment count

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Exceed the allowed attachment count.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Exceed the size limit with the combined attachment size

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Exceed the size limit with the combined attachment size.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Exceed the size limit with the combined text and attachment size

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Exceed the size limit with the combined text and attachment size.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Display a warning when exceeding MMS limits

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Display a warning when exceeding MMS limits.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Cancel sending after the warning is displayed

### Preconditions

- Messaging is the default SMS app; an active MMS-capable SIM and a test recipient are available.
- Open an individual conversation. Prepare supported media.

### Steps

1. Prepare a draft exceeding the mapped attachment limit.
2. Tap Send.
3. Dismiss the limit warning without confirming.

### Expected Result

- No dispatch is initiated by the dismissed warning. The draft remains available.

### Source reference

`SendConversationDraft.kt`: validateVideoAttachmentLimit / validateMappedMessageForSend; `MmsUtils.java`: MAX_VIDEO_ATTACHMENT_COUNT; `ConversationDraftDelegateSendValidationTest.kt`.
---

## Confirm sending using Send anyway

### Preconditions

- Messaging is the default SMS app; an active MMS-capable SIM and a test recipient are available.
- Open an individual conversation. Prepare supported media.

### Steps

1. Prepare a draft exceeding the mapped attachment size or count limit, with no more than one video.
2. Tap Send.
3. Choose Send anyway in the limit warning.
4. Check the outgoing message and recipient device.

### Expected Result

- The app retries dispatch with the size/count validation bypassed.
- If dispatch succeeds, the draft is cleared. Carrier delivery is not guaranteed by the bypass.

### Source reference

`SendConversationDraft.kt`: validateVideoAttachmentLimit / validateMappedMessageForSend; `MmsUtils.java`: MAX_VIDEO_ATTACHMENT_COUNT; `ConversationDraftDelegateSendValidationTest.kt`.
---

## Add one video attachment

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Add one video attachment.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Attempt to send two video attachments in one message

### Preconditions

- Messaging is the default SMS app; an active MMS-capable SIM and a test recipient are available.
- Open an individual conversation. Prepare supported media.

### Steps

1. Add two supported videos to the same draft.
2. Tap Send.
3. Check the warning and draft.

### Expected Result

- The video-count warning is displayed; dispatch is blocked.
- The draft remains available for correction.

### Source reference

`SendConversationDraft.kt`: validateVideoAttachmentLimit / validateMappedMessageForSend; `MmsUtils.java`: MAX_VIDEO_ATTACHMENT_COUNT; `ConversationDraftDelegateSendValidationTest.kt`.
---

## Remove an attachment after exceeding the limit

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Remove an attachment after exceeding the limit.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Retry sending after reducing the message size

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Retry sending after reducing the message size.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Preserve draft content after a validation error

### Preconditions

- Messaging is the default SMS app; an active MMS-capable SIM and a test recipient are available.
- Open an individual conversation. Prepare supported media.

### Steps

1. Enter identifiable text and add two videos.
2. Tap Send.
3. Check the composer after the warning.

### Expected Result

- The draft text is retained and the sending state is cleared; the user can correct the draft.

### Source reference

`SendConversationDraft.kt`: validateVideoAttachmentLimit / validateMappedMessageForSend; `MmsUtils.java`: MAX_VIDEO_ATTACHMENT_COUNT; `ConversationDraftDelegateSendValidationTest.kt`.
---

## Verify MMS limits for different carriers

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Verify MMS limits for different carriers.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Verify MMS limits for different SIM cards

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open a controlled MMS conversation. Set up the test conditions described above.
2. Verify MMS limits for different SIM cards.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
