# Link Handling Link Opening Settings
---

## Verify the default link opening mode

### Preconditions

- Use a clean app settings state.

### Steps

1. Open Messaging settings and navigate to Privacy.
2. Inspect Link opening.

### Expected Result

- Ask before opening is selected by default.

### Source reference

`AppSettingsRepository.kt`: default LinkOpeningMode.AskBeforeOpening.
---

## Switch between available link opening modes

### Steps

1. Open a test conversation or Privacy settings. Set up the test conditions described above.
2. Switch between available link opening modes.
3. Check the result on the affected screen or in the saved data. Compare it with the initial state.

### Expected Result

- Record what happens. Check that the app does not crash and that unrelated recipients and records are unchanged.

### Review Status

Draft: confirm the expected behavior in the source code or on a test device.
