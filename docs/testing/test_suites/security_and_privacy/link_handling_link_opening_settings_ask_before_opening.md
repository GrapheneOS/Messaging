# Link Handling Link Opening Settings Ask Before Opening
---

## Display a confirmation dialog when tapping a hyperlink

### Preconditions

- Set Link opening to Ask before opening.
- Open a conversation containing https://example.com/path?q=qa#test; a browser is available.

### Steps

1. Tap the URL.

### Expected Result

- A confirmation dialog appears before external navigation.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Display the complete hyperlink address in the confirmation dialog

### Preconditions

- Set Link opening to Ask before opening.
- Open a conversation containing https://example.com/path?q=qa#test; a browser is available.

### Steps

1. Tap the URL.
2. Check the dialog; scroll its content if necessary.

### Expected Result

- The host example.com and complete URL including query and fragment are shown.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Confirm hyperlink opening

### Preconditions

- Set Link opening to Ask before opening.
- Open a conversation containing https://example.com/path?q=qa#test; a browser is available.

### Steps

1. Tap the URL.
2. Tap the confirmation action.

### Expected Result

- The dialog closes and the original URI is passed to the external handler.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Cancel hyperlink opening

### Preconditions

- Set Link opening to Ask before opening.
- Open a conversation containing https://example.com/path?q=qa#test; a browser is available.

### Steps

1. Tap the URL.
2. Tap Cancel.

### Expected Result

- The dialog closes without an external open action.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Dismiss the confirmation dialog using system Back navigation

### Preconditions

- Set Link opening to Ask before opening.
- Open a conversation containing https://example.com/path?q=qa#test; a browser is available.

### Steps

1. Tap the URL.
2. Use system Back.

### Expected Result

- The dialog is dismissed without opening the URI.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Tap the hyperlink again after cancelling

### Preconditions

- Set Link opening to Ask before opening.
- Open a conversation containing https://example.com/path?q=qa#test; a browser is available.

### Steps

1. Tap the URL.
2. Tap Cancel.
3. Tap the same URL again.

### Expected Result

- A new confirmation dialog appears.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Verify that the hyperlink is not opened without confirmation

### Preconditions

- Set Link opening to Ask before opening.
- Open a conversation containing https://example.com/path?q=qa#test; a browser is available.

### Steps

1. Tap the URL.
2. Leave the confirmation dialog open.

### Expected Result

- No external open action occurs before confirmation.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
