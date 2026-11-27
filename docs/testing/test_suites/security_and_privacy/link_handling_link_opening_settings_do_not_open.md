# Link Handling Link Opening Settings Do Not Open
---

## Tap a hyperlink while link opening is disabled

### Preconditions

- Set Link opening to Do not open.
- Open a conversation containing https://example.com/path?q=qa.

### Steps

1. Tap the URL once.
2. Observe whether an external handler opens or a confirmation appears.

### Expected Result

- No external open action or confirmation dialog occurs.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Verify that the browser is not opened

### Preconditions

- Set Link opening to Do not open.
- Open a conversation containing https://example.com/path?q=qa.

### Steps

1. Tap the URL once.
2. Observe whether an external handler opens or a confirmation appears.

### Expected Result

- No external open action or confirmation dialog occurs.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Copy a hyperlink while link opening is disabled

### Steps

1. Open a test conversation or Privacy settings. Set up the test conditions described above.
2. Copy a hyperlink while link opening is disabled.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
---

## Verify that hyperlinks are not opened automatically

### Steps

1. Open a test conversation or Privacy settings. Set up the test conditions described above.
2. Verify that hyperlinks are not opened automatically.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
