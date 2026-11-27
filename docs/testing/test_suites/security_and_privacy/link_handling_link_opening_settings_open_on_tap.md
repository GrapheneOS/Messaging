# Link Handling Link Opening Settings Open On Tap
---

## Open a hyperlink with a single tap

### Preconditions

- Set Link opening to Open on tap.
- Open a conversation containing https://example.com/path?q=qa.

### Steps

1. Tap the URL once.
2. Observe whether an external handler opens or a confirmation appears.

### Expected Result

- The URI is passed directly to the external handler without confirmation.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
---

## Open a hyperlink without additional confirmation

### Preconditions

- Set Link opening to Open on tap.
- Open a conversation containing https://example.com/path?q=qa.

### Steps

1. Tap the URL once.
2. Observe whether an external handler opens or a confirmation appears.

### Expected Result

- The URI is passed directly to the external handler without confirmation.

### Source reference

`ConversationLinkDelegate.kt`; `ConversationLinkConfirmationDialog.kt`.
