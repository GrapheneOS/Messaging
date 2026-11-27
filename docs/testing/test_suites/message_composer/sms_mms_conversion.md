# Sms Mms Conversion
---

## Convert SMS to MMS when adding an attachment

### Preconditions

- Open an individual conversation with a phone-number recipient and active subscription.
- No other MMS conversion condition applies.

### Steps

1. Enter short GSM-7 text.
2. Add one supported image.
3. Check the message type.

### Expected Result

- The draft resolves to MMS.

### Source reference

`GetConversationDraftSendProtocol.kt`: shouldSendAsMms; `MessageTextStats`.
---

## Convert MMS back to SMS after removing all attachments

### Preconditions

- Open an individual conversation with a phone-number recipient and active subscription.
- No other MMS conversion condition applies.

### Steps

1. Enter short text and add an image.
2. Remove the image.
3. Check the message type.

### Expected Result

- The draft resolves to SMS.

### Source reference

`GetConversationDraftSendProtocol.kt`: shouldSendAsMms; `MessageTextStats`.
---

## Convert SMS to MMS when adding a message subject

### Preconditions

- Open an individual conversation with a phone-number recipient and active subscription.
- No other MMS conversion condition applies.

### Steps

1. Enter short text.
2. Add a nonblank subject.
3. Check the message type.

### Expected Result

- The draft resolves to MMS.

### Source reference

`GetConversationDraftSendProtocol.kt`: shouldSendAsMms; `MessageTextStats`.
---

## Convert MMS back to SMS after removing the message subject

### Preconditions

- Open an individual conversation with a phone-number recipient and active subscription.
- No other MMS conversion condition applies.

### Steps

1. Enter short text and a nonblank subject.
2. Clear the subject.
3. Check the message type.

### Expected Result

- The draft resolves to SMS.

### Source reference

`GetConversationDraftSendProtocol.kt`: shouldSendAsMms; `MessageTextStats`.
---

## Convert a long SMS to MMS according to carrier configuration

### Preconditions

- Open an individual conversation with a phone-number recipient and active subscription.
- No other MMS conversion condition applies.

### Steps

1. Determine the selected subscription conversion threshold.
2. Enter text below the threshold.
3. Increase text until the threshold is crossed.
4. Check the message type.

### Expected Result

- The protocol becomes MMS when MessageTextStats requires MMS.

### Source reference

`GetConversationDraftSendProtocol.kt`: shouldSendAsMms; `MessageTextStats`.
---

## Convert MMS back to SMS after reducing text length when no other conversion conditions apply

### Preconditions

- Open an individual conversation with a phone-number recipient and active subscription.
- No other MMS conversion condition applies.

### Steps

1. Enter text exceeding the configured conversion threshold.
2. Reduce text below that threshold.
3. Check the message type.

### Expected Result

- The protocol returns to SMS.

### Source reference

`GetConversationDraftSendProtocol.kt`: shouldSendAsMms; `MessageTextStats`.
---

## Determine the message protocol when adding multiple recipients

### Steps

1. Open a test conversation composer. Set up the test conditions described above.
2. Determine the message protocol when adding multiple recipients.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Determine the message protocol after changing group messaging settings

### Steps

1. Open a test conversation composer. Set up the test conditions described above.
2. Determine the message protocol after changing group messaging settings.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Display the current message type

### Steps

1. Open a test conversation composer. Set up the test conditions described above.
2. Display the current message type.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Update the message type when editing draft content

### Steps

1. Open a test conversation composer. Set up the test conditions described above.
2. Update the message type when editing draft content.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
