# Messaging - Investigation Checklist

## Application And Source Code

- [ ] List all available application screens.
- [ ] Identify all application entry points.
- [ ] List all supported Android Intent actions.
- [ ] List all supported URI schemes.
- [ ] Identify all registered broadcast receivers.
- [ ] Identify all background services and workers.
- [ ] List all required application permissions.
- [ ] List all optional application permissions.
- [ ] Identify all application settings and default values.
- [ ] Identify debug-only functionality.
- [ ] Identify device-specific functionality.
- [ ] Identify Android-version-specific functionality.
- [ ] Identify carrier-specific functionality.
- [ ] Identify user-profile-specific functionality.
- [ ] Identify implemented but inaccessible functionality.
- [ ] Identify incomplete or experimental functionality.
- [ ] Identify remaining Legacy application components.
- [ ] Identify functionality migrated to Compose.
- [ ] Review existing unit tests.
- [ ] Review existing instrumentation tests.
- [ ] Identify scenarios already covered by automated tests.
- [ ] Identify missing functional test coverage.

## Phone Number Processing

- [ ] Investigate phone number normalization.
- [ ] Investigate national phone number formatting.
- [ ] Investigate international phone number formatting.
- [ ] Investigate automatic country calling code population.
- [ ] Compare phone number processing with different system regions.
- [ ] Compare phone number processing with SIM cards from different countries.
- [ ] Compare phone number processing with one active SIM and two active SIM cards.
- [ ] Investigate phone number processing without an active SIM.
- [ ] Investigate phone number matching with different separators.
- [ ] Investigate phone number matching with leading zeros.
- [ ] Investigate phone number matching with international prefixes.
- [ ] Investigate duplicate phone number detection.
- [ ] Investigate ambiguous contact matching.
- [ ] Investigate minimum accepted phone number length.
- [ ] Investigate maximum accepted phone number length.
- [ ] Investigate handling of alphabetic phone number input.
- [ ] Investigate handling of Unicode digits.
- [ ] Investigate handling of invisible Unicode characters.
- [ ] Investigate handling of phone number extensions.
- [ ] Investigate handling of short codes.
- [ ] Investigate handling of service numbers.
- [ ] Investigate handling of email recipients.
- [ ] Investigate Send to visibility conditions.
- [ ] Investigate Send to recipient validation.
- [ ] Investigate differences between keyboard confirmation and Send to confirmation.

## SMS Protocol And Encoding

- [ ] Identify supported SMS encodings.
- [ ] Identify GSM-7 character counting rules.
- [ ] Identify GSM-7 extension character counting rules.
- [ ] Identify Unicode character counting rules.
- [ ] Identify composite emoji counting rules.
- [ ] Identify SMS segment calculation rules.
- [ ] Identify the maximum supported SMS segment count.
- [ ] Investigate SMS-to-MMS conversion thresholds.
- [ ] Compare conversion thresholds across carrier configurations.
- [ ] Compare conversion thresholds across active SIM cards.
- [ ] Investigate automatic encoding changes during text editing.
- [ ] Investigate character replacement during SMS encoding.
- [ ] Investigate unsupported character handling.
- [ ] Investigate message content normalization before sending.
- [ ] Investigate leading and trailing whitespace handling.
- [ ] Investigate multipart SMS assembly.
- [ ] Investigate incomplete multipart SMS handling.
- [ ] Investigate out-of-order multipart SMS handling.
- [ ] Investigate duplicate multipart SMS segment handling.
- [ ] Investigate multipart SMS assembly timeout.
- [ ] Investigate Class 0 SMS processing.
- [ ] Investigate alphanumeric sender processing.
- [ ] Investigate binary SMS processing.
- [ ] Investigate premium SMS restrictions.

## SMS Sending And Recovery

- [ ] Identify all outgoing SMS states.
- [ ] List all supported SMS sending error codes.
- [ ] Identify the mapping between system errors and displayed errors.
- [ ] Investigate automatic SMS retry behavior.
- [ ] Investigate SMS retry limits.
- [ ] Investigate SMS retry intervals.
- [ ] Investigate pending SMS cancellation.
- [ ] Investigate sending queue behavior.
- [ ] Investigate sending order for consecutive messages.
- [ ] Investigate simultaneous sending to multiple recipients.
- [ ] Investigate sending behavior after network recovery.
- [ ] Investigate sending behavior after device reboot.
- [ ] Investigate sending behavior after process termination.
- [ ] Investigate delivery report processing.
- [ ] Investigate delayed delivery reports.
- [ ] Investigate duplicate delivery reports.
- [ ] Investigate delivery reports received after message deletion.
- [ ] Investigate duplicate message prevention.
- [ ] Investigate SMS sending over IMS.
- [ ] Investigate SMS sending over Wi-Fi Calling.

## MMS Configuration And Limits

- [ ] Identify the source of MMS configuration values.
- [ ] Identify default MMS size limits.
- [ ] Identify carrier-specific MMS size limits.
- [ ] Identify subscription-specific MMS size limits.
- [ ] Identify maximum attachment count rules.
- [ ] Identify maximum recipient count rules.
- [ ] Identify video attachment count restrictions.
- [ ] Identify supported MMS content types.
- [ ] Identify MMS subject validation rules.
- [ ] Identify MMS size calculation rules.
- [ ] Identify attachment metadata included in size calculations.
- [ ] Investigate Send anyway behavior.
- [ ] Identify validation rules bypassed by Send anyway.
- [ ] Identify validation rules not bypassed by Send anyway.
- [ ] Investigate limit recalculation after changing the selected SIM.
- [ ] Investigate limit recalculation after carrier configuration changes.
- [ ] Investigate MMS behavior without a valid carrier configuration.
- [ ] Investigate MMS behavior without a configured self phone number.

## MMS Transport

- [ ] Investigate MMS sending with mobile data disabled.
- [ ] Investigate MMS downloading with mobile data disabled.
- [ ] Investigate MMS sending over Wi-Fi.
- [ ] Investigate MMS downloading over Wi-Fi.
- [ ] Investigate MMS behavior over Wi-Fi Calling.
- [ ] Investigate MMS behavior over IMS.
- [ ] Investigate MMS behavior when the selected SMS SIM differs from the mobile data SIM.
- [ ] Investigate MMS behavior while roaming.
- [ ] Investigate MMS behavior with unavailable APN settings.
- [ ] Investigate MMS behavior with invalid APN settings.
- [ ] Investigate MMS connection timeout.
- [ ] Investigate MMS sending timeout.
- [ ] Investigate MMS downloading timeout.
- [ ] Investigate automatic MMS retry behavior.
- [ ] Investigate MMS retry limits.
- [ ] Investigate MMS expiration handling.
- [ ] Investigate duplicate MMS notification handling.
- [ ] Investigate partially downloaded MMS handling.
- [ ] Investigate corrupted MMS payload handling.
- [ ] Investigate unsupported MMS content handling.

## Attachment Format Compatibility

### Images

- [ ] Investigate JPEG support.
- [ ] Investigate PNG support.
- [ ] Investigate WebP support.
- [ ] Investigate animated WebP support.
- [ ] Investigate GIF support.
- [ ] Investigate animated GIF preservation.
- [ ] Investigate HEIC support.
- [ ] Investigate HEIF support.
- [ ] Investigate AVIF support.
- [ ] Investigate BMP support.
- [ ] Investigate TIFF support.
- [ ] Investigate SVG support.
- [ ] Investigate transparent image handling.
- [ ] Investigate high-resolution image handling.
- [ ] Investigate image color profile handling.
- [ ] Investigate EXIF orientation handling.
- [ ] Investigate EXIF metadata preservation.
- [ ] Investigate GPS metadata preservation.
- [ ] Investigate image compression.
- [ ] Investigate image resizing.
- [ ] Investigate image quality after compression.
- [ ] Investigate image aspect ratio preservation.
- [ ] Investigate corrupted image handling.

### Video

- [ ] Investigate MP4 support.
- [ ] Investigate WebM support.
- [ ] Investigate 3GP support.
- [ ] Investigate MOV support.
- [ ] Investigate MKV support.
- [ ] Investigate H.264 video support.
- [ ] Investigate H.265 video support.
- [ ] Investigate VP8 video support.
- [ ] Investigate VP9 video support.
- [ ] Investigate AV1 video support.
- [ ] Investigate HDR video handling.
- [ ] Investigate high-resolution video handling.
- [ ] Investigate high-frame-rate video handling.
- [ ] Investigate variable-frame-rate video handling.
- [ ] Investigate video rotation metadata.
- [ ] Investigate video compression.
- [ ] Investigate video transcoding.
- [ ] Investigate video thumbnail generation.
- [ ] Investigate corrupted video handling.

### Audio

- [ ] Investigate MP3 support.
- [ ] Investigate AAC support.
- [ ] Investigate M4A support.
- [ ] Investigate WAV support.
- [ ] Investigate OGG support.
- [ ] Investigate Opus support.
- [ ] Investigate AMR support.
- [ ] Investigate FLAC support.
- [ ] Investigate audio metadata handling.
- [ ] Investigate audio compression.
- [ ] Investigate audio transcoding.
- [ ] Investigate corrupted audio handling.

### Other Files

- [ ] Investigate PDF attachment handling.
- [ ] Investigate TXT attachment handling.
- [ ] Investigate ZIP attachment handling.
- [ ] Investigate files without extensions.
- [ ] Investigate files with incorrect extensions.
- [ ] Investigate files with incorrect MIME types.
- [ ] Investigate zero-byte files.
- [ ] Investigate inaccessible files.
- [ ] Investigate files deleted after selection.
- [ ] Investigate files modified after selection.
- [ ] Investigate files with extremely long names.
- [ ] Investigate files with identical names.
- [ ] Investigate files with Unicode filenames.

## Attachment Processing

- [ ] Identify supported attachment sources.
- [ ] Investigate attachment selection from local storage.
- [ ] Investigate attachment selection from cloud storage.
- [ ] Investigate attachment selection from external storage.
- [ ] Investigate attachment selection from other applications.
- [ ] Investigate attachment processing order.
- [ ] Investigate simultaneous attachment processing.
- [ ] Investigate attachment processing cancellation.
- [ ] Investigate attachment processing timeout.
- [ ] Investigate attachment processing after process termination.
- [ ] Investigate attachment processing after permission revocation.
- [ ] Investigate attachment draft persistence.
- [ ] Investigate temporary attachment storage.
- [ ] Investigate temporary attachment cleanup.
- [ ] Investigate attachment deletion after draft removal.
- [ ] Investigate attachment deletion after message deletion.
- [ ] Investigate attachment caption support.
- [ ] Investigate attachment reordering support.
- [ ] Investigate attachment forwarding restrictions.

## Camera And Audio Recording

- [ ] Identify available camera controls.
- [ ] Investigate front camera support.
- [ ] Investigate rear camera support.
- [ ] Investigate camera switching.
- [ ] Investigate flash control.
- [ ] Investigate autofocus behavior.
- [ ] Investigate image orientation after capture.
- [ ] Investigate direct video recording support.
- [ ] Investigate camera resource release after cancellation.
- [ ] Identify the audio recording format.
- [ ] Identify the audio recording codec.
- [ ] Identify the audio recording size limit.
- [ ] Identify the audio recording duration limit.
- [ ] Investigate recording behavior when reaching the size limit.
- [ ] Investigate recording behavior when reaching the duration limit.
- [ ] Investigate audio recording in the background.
- [ ] Investigate audio recording during screen lock.
- [ ] Investigate audio recording during incoming calls.
- [ ] Investigate audio recording with Bluetooth microphones.
- [ ] Investigate audio recording with wired headsets.
- [ ] Investigate audio focus handling.
- [ ] Investigate microphone resource release.

## Media Viewer

- [ ] Identify supported Media Viewer content types.
- [ ] Identify available Media Viewer actions.
- [ ] Identify supported Media Viewer gestures.
- [ ] Investigate pinch-to-zoom behavior.
- [ ] Investigate double-tap zoom behavior.
- [ ] Investigate minimum and maximum zoom levels.
- [ ] Investigate image panning boundaries.
- [ ] Investigate image navigation.
- [ ] Investigate navigation between different attachment types.
- [ ] Investigate video playback controls.
- [ ] Investigate video seeking.
- [ ] Investigate playback position restoration.
- [ ] Investigate audio playback controls.
- [ ] Investigate playback through Bluetooth devices.
- [ ] Investigate playback through wired headsets.
- [ ] Investigate playback during screen lock.
- [ ] Investigate playback in the background.
- [ ] Investigate simultaneous media playback.
- [ ] Investigate attachment saving.
- [ ] Identify the attachment saving location.
- [ ] Investigate duplicate filename handling.
- [ ] Investigate sharing attachments from Media Viewer.
- [ ] Investigate opening attachments in external applications.
- [ ] Investigate memory release after closing Media Viewer.

## vCard

- [ ] Identify supported vCard versions.
- [ ] Investigate vCard 2.1 handling.
- [ ] Investigate vCard 3.0 handling.
- [ ] Investigate vCard 4.0 handling.
- [ ] Investigate vCard files containing multiple contacts.
- [ ] Investigate vCard files containing multiple phone numbers.
- [ ] Investigate vCard files containing email addresses.
- [ ] Investigate vCard files containing postal addresses.
- [ ] Investigate vCard files containing contact photos.
- [ ] Investigate vCard files containing Unicode characters.
- [ ] Investigate vCard files containing custom fields.
- [ ] Investigate vCard files containing unsupported fields.
- [ ] Investigate malformed vCard files.
- [ ] Investigate contact import and merge behavior.

## Group Messaging

- [ ] Identify group conversation matching rules.
- [ ] Identify group participant normalization rules.
- [ ] Identify group recipient count limits.
- [ ] Investigate duplicate participant handling.
- [ ] Investigate participant order independence.
- [ ] Investigate group creation with the self phone number.
- [ ] Investigate group creation with email recipients.
- [ ] Investigate group creation with short codes.
- [ ] Investigate group creation with different numbers belonging to one contact.
- [ ] Investigate group creation with recipients using different phone number formats.
- [ ] Investigate group MMS and individual SMS mode differences.
- [ ] Investigate partial sending failures in individual SMS mode.
- [ ] Investigate incoming group participant resolution.
- [ ] Investigate incoming group messages with changed participant lists.
- [ ] Investigate group conversation matching after contact updates.
- [ ] Investigate blocked participants in group conversations.
- [ ] Investigate group behavior after changing the selected SIM.
- [ ] Investigate group behavior after changing the self phone number.

## Notifications

- [ ] Identify all Messaging notification channels.
- [ ] List all supported notification actions.
- [ ] Identify notification grouping rules.
- [ ] Identify notification suppression rules.
- [ ] Identify notification preview limits.
- [ ] Investigate notification behavior for the currently opened conversation.
- [ ] Investigate notification behavior for other conversations.
- [ ] Investigate notification dismissal and read state interaction.
- [ ] Investigate Quick Reply and read state interaction.
- [ ] Investigate Quick Reply with an existing draft.
- [ ] Investigate Quick Reply sending failures.
- [ ] Investigate Quick Reply after conversation deletion.
- [ ] Investigate Quick Reply after SIM changes.
- [ ] Investigate notification behavior after contact updates.
- [ ] Investigate notification behavior after blocking a sender.
- [ ] Investigate notification behavior after unblocking a sender.
- [ ] Investigate notification behavior after archiving a conversation.
- [ ] Investigate notification behavior during Snooze.
- [ ] Investigate Snooze expiration after system time changes.
- [ ] Investigate Snooze expiration after device reboot.
- [ ] Investigate notification behavior under Doze mode.
- [ ] Investigate notification behavior under battery restrictions.
- [ ] Investigate notification behavior after force-stopping Messaging.
- [ ] Investigate notification behavior before the first device unlock.
- [ ] Investigate notification bubbles support.
- [ ] Investigate Android conversation notification integration.

## Widgets

- [ ] Identify all widget update triggers.
- [ ] Identify widget data sources.
- [ ] Identify widget conversation filtering rules.
- [ ] Identify widget message filtering rules.
- [ ] Identify widget preview length limits.
- [ ] Identify widget conversation count limits.
- [ ] Identify widget message count limits.
- [ ] Identify widget size restrictions.
- [ ] Measure widget update delay after receiving messages.
- [ ] Measure widget update delay after sending messages.
- [ ] Measure widget update delay after contact updates.
- [ ] Investigate widget avatar caching.
- [ ] Investigate widget contact name caching.
- [ ] Investigate widget updates after contact deletion.
- [ ] Investigate widget updates after contact permission revocation.
- [ ] Investigate widget updates with notifications disabled.
- [ ] Investigate widget updates while Messaging is force-stopped.
- [ ] Investigate widget updates under Doze mode.
- [ ] Investigate widget updates under battery restrictions.
- [ ] Investigate widget behavior before the first device unlock.
- [ ] Investigate widget behavior for archived conversations.
- [ ] Investigate widget behavior for blocked conversations.
- [ ] Investigate widget behavior after deleting the associated conversation.
- [ ] Investigate widget behavior after recreating the associated conversation.
- [ ] Investigate widget configuration persistence.
- [ ] Investigate widget migration from Legacy Messaging.
- [ ] Investigate widget behavior across different launchers.
- [ ] Investigate widget behavior in Secondary Profiles.
- [ ] Investigate widget behavior in Work Profiles.

## SIM And eSIM

- [ ] Identify subscription selection rules.
- [ ] Identify default subscription resolution rules.
- [ ] Identify per-conversation subscription persistence.
- [ ] Identify per-SIM application settings.
- [ ] Identify carrier configuration dependencies.
- [ ] Investigate subscription ID changes.
- [ ] Investigate SIM removal and reinsertion.
- [ ] Investigate SIM replacement.
- [ ] Investigate eSIM deactivation and reactivation.
- [ ] Investigate simultaneous physical SIM and eSIM usage.
- [ ] Investigate simultaneous eSIM usage.
- [ ] Investigate SIM selection without a system default SMS SIM.
- [ ] Investigate SIM selection when the previously selected SIM is unavailable.
- [ ] Investigate automatic SIM fallback behavior.
- [ ] Investigate default SMS SIM changes during draft creation.
- [ ] Investigate default SMS SIM changes during sending.
- [ ] Investigate mobile data SIM changes during MMS sending.
- [ ] Investigate mobile data SIM changes during MMS downloading.
- [ ] Investigate SIM settings persistence after subscription changes.
- [ ] Investigate message history association with removed SIM cards.
- [ ] Investigate message history association with changed subscription IDs.
- [ ] Investigate SIM cards with identical phone numbers.
- [ ] Investigate SIM cards without available phone numbers.
- [ ] Investigate international roaming behavior.
- [ ] Investigate carrier configuration overrides.

## User Profiles

### Owner Profile

- [ ] Identify Owner Profile-specific functionality.
- [ ] Identify Owner Profile-specific permissions.
- [ ] Identify Owner Profile-specific settings.
- [ ] Investigate message processing while another profile is active.
- [ ] Investigate MMS downloading while another profile is active.

### Secondary Profiles

- [ ] Identify Secondary Profile-specific restrictions.
- [ ] Investigate default SMS application availability.
- [ ] Investigate SMS sending permissions.
- [ ] Investigate SMS receiving permissions.
- [ ] Investigate MMS sending permissions.
- [ ] Investigate MMS receiving permissions.
- [ ] Investigate MMS downloading restrictions.
- [ ] Investigate message history availability.
- [ ] Investigate message history synchronization.
- [ ] Investigate read state synchronization.
- [ ] Investigate message deletion synchronization.
- [ ] Investigate conversation deletion synchronization.
- [ ] Investigate conversation archiving synchronization.
- [ ] Investigate pinned conversation isolation.
- [ ] Investigate draft isolation.
- [ ] Investigate blocked sender isolation.
- [ ] Investigate notification settings isolation.
- [ ] Investigate SIM selection isolation.
- [ ] Investigate contact information isolation.
- [ ] Investigate attachment storage isolation.
- [ ] Investigate notification delivery to background profiles.
- [ ] Investigate notification delivery to multiple active profiles.
- [ ] Investigate widget updates in background profiles.
- [ ] Investigate behavior after revoking phone call and SMS access.
- [ ] Investigate behavior after ending a profile session.
- [ ] Investigate message availability after restarting a profile session.
- [ ] Investigate behavior after deleting a Secondary Profile.

### Other Profile Types

- [ ] Investigate Messaging availability in Work Profiles.
- [ ] Investigate Messaging availability in Private Space.
- [ ] Investigate Messaging availability in Guest Profiles.
- [ ] Identify supported functionality for each available profile type.
- [ ] Identify permission restrictions for each available profile type.
- [ ] Identify data isolation requirements for each available profile type.

## System Integration

- [ ] Identify supported Android Share MIME types.
- [ ] Identify supported ACTION_SEND scenarios.
- [ ] Identify supported ACTION_SEND_MULTIPLE scenarios.
- [ ] Identify supported ACTION_SENDTO scenarios.
- [ ] Investigate sms URI handling.
- [ ] Investigate smsto URI handling.
- [ ] Investigate mms URI handling.
- [ ] Investigate mmsto URI handling.
- [ ] Investigate URI-encoded message bodies.
- [ ] Investigate URI-encoded message subjects.
- [ ] Investigate multiple recipients in external Intents.
- [ ] Investigate malformed external Intents.
- [ ] Investigate missing Intent parameters.
- [ ] Investigate conflicting Intent parameters.
- [ ] Investigate inaccessible content URIs.
- [ ] Investigate expired URI permissions.
- [ ] Investigate temporary URI permissions.
- [ ] Investigate persistable URI permissions.
- [ ] Investigate external Intent handling with an existing draft.
- [ ] Investigate Android Direct Share support.
- [ ] Investigate launcher shortcut support.
- [ ] Investigate conversation shortcut support.
- [ ] Investigate Android Auto integration.
- [ ] Investigate smartwatch integration.
- [ ] Investigate Bluetooth message access.
- [ ] Investigate system blocked number provider integration.
- [ ] Investigate synchronization with other SMS applications.

## Link Handling

### URL Detection

- [ ] Identify the URL detection implementation.
- [ ] Identify supported URL schemes.
- [ ] Investigate HTTP URL detection.
- [ ] Investigate HTTPS URL detection.
- [ ] Investigate URL detection without a protocol.
- [ ] Investigate URL detection without www.
- [ ] Investigate internationalized domain name detection.
- [ ] Investigate Punycode domain detection.
- [ ] Investigate IPv4 URL detection.
- [ ] Investigate IPv6 URL detection.
- [ ] Investigate localhost URL detection.
- [ ] Investigate URL detection with custom ports.
- [ ] Investigate URL detection with fragments.
- [ ] Investigate URL detection with query parameters.
- [ ] Investigate URL detection with percent-encoded characters.
- [ ] Investigate URL detection with Unicode characters.
- [ ] Investigate URL detection with trailing punctuation.
- [ ] Investigate URL detection inside parentheses.
- [ ] Investigate URL detection inside quotation marks.
- [ ] Investigate URL detection across line breaks.
- [ ] Investigate extremely long URL detection.
- [ ] Investigate malformed URL detection.
- [ ] Investigate multiple adjacent URL detection.
- [ ] Investigate mailto URI detection.
- [ ] Investigate tel URI detection.
- [ ] Investigate geo URI detection.
- [ ] Investigate custom application URI detection.

### URL Opening

- [ ] Identify supported URL opening actions.
- [ ] Identify supported external application handlers.
- [ ] Investigate Android App Links handling.
- [ ] Investigate browser selection behavior.
- [ ] Investigate URL opening without a default browser.
- [ ] Investigate URL opening without an available handler.
- [ ] Investigate URL opening with multiple available handlers.
- [ ] Investigate custom URI scheme handling.
- [ ] Investigate intent URI handling.
- [ ] Investigate file URI restrictions.
- [ ] Investigate content URI restrictions.
- [ ] Investigate javascript URI restrictions.
- [ ] Investigate data URI restrictions.
- [ ] Investigate URL confirmation dialog content.
- [ ] Investigate long URL truncation in confirmation dialogs.
- [ ] Investigate displayed URL and actual destination consistency.
- [ ] Investigate URL copying in each link opening mode.
- [ ] Investigate network activity before link opening confirmation.
- [ ] Investigate network activity after cancelling link opening.
- [ ] Investigate link handling with internet access disabled.

## YouTube Link Previews

- [ ] Identify supported YouTube URL patterns.
- [ ] Identify the preview retrieval implementation.
- [ ] Identify external preview service endpoints.
- [ ] Identify data transmitted in preview requests.
- [ ] Identify preview request triggers.
- [ ] Identify preview cache behavior.
- [ ] Identify preview cache expiration.
- [ ] Identify preview retry behavior.
- [ ] Investigate standard YouTube video links.
- [ ] Investigate shortened YouTube links.
- [ ] Investigate YouTube Shorts links.
- [ ] Investigate YouTube playlist links.
- [ ] Investigate YouTube live stream links.
- [ ] Investigate YouTube links containing timestamps.
- [ ] Investigate YouTube links containing additional parameters.
- [ ] Investigate multiple YouTube links in one message.
- [ ] Investigate duplicate YouTube links.
- [ ] Investigate malformed YouTube links.
- [ ] Investigate unavailable videos.
- [ ] Investigate private videos.
- [ ] Investigate age-restricted videos.
- [ ] Investigate geographically restricted videos.
- [ ] Investigate preview loading failures.
- [ ] Investigate preview loading without network connectivity.
- [ ] Investigate preview loading after network recovery.
- [ ] Investigate preview generation for existing messages.
- [ ] Investigate preview generation for outgoing messages.
- [ ] Investigate preview cache behavior after disabling previews.
- [ ] Investigate preview network activity with previews disabled.

## Security And Privacy

- [ ] Identify all application network requests.
- [ ] Identify all externally transmitted application data.
- [ ] Identify application data storage locations.
- [ ] Identify telephony provider data dependencies.
- [ ] Identify temporary file storage locations.
- [ ] Identify cached media storage locations.
- [ ] Investigate data availability before the first device unlock.
- [ ] Investigate data availability while the device is locked.
- [ ] Investigate message content exposure in Recent Apps.
- [ ] Investigate sensitive notification content exposure.
- [ ] Investigate widget content exposure.
- [ ] Investigate contact information exposure after permission revocation.
- [ ] Investigate attachment access after permission revocation.
- [ ] Investigate cross-profile data isolation.
- [ ] Investigate external Intent input validation.
- [ ] Investigate malformed URI handling.
- [ ] Investigate unauthorized content URI access prevention.
- [ ] Investigate attachment metadata exposure.
- [ ] Investigate location metadata exposure.
- [ ] Investigate message content exposure in diagnostic logs.
- [ ] Investigate phone number exposure in diagnostic logs.
- [ ] Investigate attachment information exposure in diagnostic logs.
- [ ] Investigate sensitive data exposure in crash reports.
- [ ] Investigate clipboard privacy behavior.
- [ ] Investigate deleted attachment cleanup.
- [ ] Investigate temporary file cleanup after sending failures.
- [ ] Investigate temporary file cleanup after application crashes.
- [ ] Investigate application behavior after revoking network access.

## Performance And Resource Consumption

### Test Data Volumes

- [ ] Prepare a dataset containing 100 messages.
- [ ] Prepare a dataset containing 1,000 messages.
- [ ] Prepare a dataset containing 10,000 messages.
- [ ] Prepare a dataset containing 50,000 messages.
- [ ] Prepare a dataset containing 100,000 messages.
- [ ] Prepare a dataset containing 100 conversations.
- [ ] Prepare a dataset containing 1,000 conversations.
- [ ] Prepare a dataset containing 5,000 conversations.
- [ ] Prepare a dataset containing 1,000 contacts.
- [ ] Prepare a dataset containing 10,000 contacts.
- [ ] Prepare conversations containing large numbers of attachments.
- [ ] Prepare mixed SMS and MMS datasets.
- [ ] Prepare datasets containing long Unicode messages.
- [ ] Prepare datasets containing archived conversations.
- [ ] Prepare datasets containing failed and pending messages.

### Performance Measurements

- [ ] Measure cold application startup time.
- [ ] Measure warm application startup time.
- [ ] Measure application startup with large message histories.
- [ ] Measure conversation list loading time.
- [ ] Measure large conversation loading time.
- [ ] Measure message history pagination time.
- [ ] Measure contact list loading time.
- [ ] Measure contact search response time.
- [ ] Measure attachment selection response time.
- [ ] Measure large image processing time.
- [ ] Measure video processing time.
- [ ] Measure Media Viewer opening time.
- [ ] Measure notification delivery delay.
- [ ] Measure widget update delay.
- [ ] Measure bulk message deletion time.
- [ ] Measure bulk conversation deletion time.
- [ ] Measure bulk conversation archiving time.

### Resource Measurements

- [ ] Measure memory usage during application startup.
- [ ] Measure memory usage with large conversation lists.
- [ ] Measure memory usage during prolonged scrolling.
- [ ] Measure memory usage during image processing.
- [ ] Measure memory usage during video playback.
- [ ] Measure memory usage after repeated screen transitions.
- [ ] Investigate potential memory leaks.
- [ ] Investigate memory release after closing Media Viewer.
- [ ] Measure background battery consumption.
- [ ] Measure battery consumption during continuous message receiving.
- [ ] Measure battery consumption with multiple active widgets.
- [ ] Measure storage usage before and after attachment processing.
- [ ] Measure cache growth during prolonged application usage.
- [ ] Investigate automatic cache cleanup.
- [ ] Investigate temporary file accumulation.
- [ ] Investigate application behavior under low-memory conditions.
- [ ] Investigate application behavior under low-storage conditions.
- [ ] Investigate ANR conditions.
- [ ] Investigate application crashes under sustained load.

## Accessibility

- [ ] Identify all accessibility semantics used by Compose screens.
- [ ] Identify icon-only controls without accessibility labels.
- [ ] Investigate TalkBack navigation order.
- [ ] Investigate TalkBack message announcements.
- [ ] Investigate TalkBack notification announcements.
- [ ] Investigate TalkBack widget announcements.
- [ ] Investigate accessibility announcements for sending failures.
- [ ] Investigate accessibility announcements for dynamic content updates.
- [ ] Investigate focus restoration after navigation.
- [ ] Investigate focus restoration after dialog dismissal.
- [ ] Investigate accessibility alternatives for swipe gestures.
- [ ] Investigate Switch Access support.
- [ ] Investigate external keyboard navigation.
- [ ] Investigate keyboard shortcuts.
- [ ] Investigate maximum font scaling.
- [ ] Investigate maximum display scaling.
- [ ] Investigate combined font and display scaling.
- [ ] Investigate high-contrast system settings.
- [ ] Investigate color inversion behavior.
- [ ] Investigate color correction behavior.
- [ ] Investigate reduced animation settings.

## Localization

- [ ] List all supported application languages.
- [ ] Identify missing translations.
- [ ] Identify hardcoded application strings.
- [ ] Identify locale-dependent formatting rules.
- [ ] Investigate RTL layout support.
- [ ] Investigate mixed-direction text rendering.
- [ ] Investigate Unicode bidirectional control character handling.
- [ ] Investigate combining character rendering.
- [ ] Investigate complex script rendering.
- [ ] Investigate CJK character rendering.
- [ ] Investigate locale-specific digits.
- [ ] Investigate locale-specific date formats.
- [ ] Investigate locale-specific time formats.
- [ ] Investigate locale-specific phone number formats.
- [ ] Investigate locale changes while Messaging is running.
- [ ] Investigate time zone changes.
- [ ] Investigate daylight saving time transitions.
- [ ] Investigate manual system time changes.
- [ ] Investigate message ordering after system time changes.

## Compatibility

- [ ] Identify supported Android versions.
- [ ] Identify supported device configurations.
- [ ] Identify responsive layout breakpoints.
- [ ] Identify two-pane layout activation conditions.
- [ ] Investigate small-screen layouts.
- [ ] Investigate large-screen layouts.
- [ ] Investigate tablet layouts.
- [ ] Investigate foldable device layouts.
- [ ] Investigate foldable device posture changes.
- [ ] Investigate portrait and landscape transitions.
- [ ] Investigate Split Screen support.
- [ ] Investigate desktop windowing support.
- [ ] Investigate dynamic window resizing.
- [ ] Investigate gesture navigation.
- [ ] Investigate three-button navigation.
- [ ] Identify supported Predictive Back scenarios.
- [ ] Investigate Predictive Back cancellation.
- [ ] Investigate Predictive Back with an existing draft.
- [ ] Investigate Predictive Back with an open keyboard.
- [ ] Investigate Predictive Back with an open attachment panel.
- [ ] Investigate Predictive Back during selection mode.
- [ ] Investigate system gesture conflicts.
- [ ] Investigate dynamic color support.
- [ ] Investigate system theme changes.
- [ ] Investigate alternative keyboard compatibility.
- [ ] Investigate floating keyboard compatibility.
- [ ] Investigate physical keyboard compatibility.

## Legacy To Compose Migration

- [ ] Identify remaining Legacy components.
- [ ] Identify completed Compose migrations.
- [ ] Identify Legacy database structures.
- [ ] Identify current database structures.
- [ ] Identify database migration operations.
- [ ] Identify Legacy application preferences.
- [ ] Identify current application preferences.
- [ ] Investigate SMS history migration.
- [ ] Investigate MMS history migration.
- [ ] Investigate attachment migration.
- [ ] Investigate group conversation migration.
- [ ] Investigate draft migration.
- [ ] Investigate message status migration.
- [ ] Investigate pinned conversation migration.
- [ ] Investigate archived conversation migration.
- [ ] Investigate blocked sender migration.
- [ ] Investigate notification settings migration.
- [ ] Investigate SIM settings migration.
- [ ] Investigate privacy settings migration.
- [ ] Investigate widget configuration migration.
- [ ] Investigate migration with large datasets.
- [ ] Investigate interrupted migration.
- [ ] Investigate repeated migration.
- [ ] Investigate migration failure recovery.
- [ ] Investigate duplicate data prevention.
- [ ] Investigate application downgrade behavior.

## Backup And Restore

- [ ] Identify supported backup mechanisms.
- [ ] Investigate Android backup configuration.
- [ ] Investigate Seedvault compatibility.
- [ ] Identify data included in application backups.
- [ ] Identify data excluded from application backups.
- [ ] Investigate SMS history backup.
- [ ] Investigate MMS history backup.
- [ ] Investigate attachment backup.
- [ ] Investigate draft backup.
- [ ] Investigate application settings backup.
- [ ] Investigate conversation settings backup.
- [ ] Investigate blocked sender backup.
- [ ] Investigate widget configuration backup.
- [ ] Investigate backup with large message histories.
- [ ] Investigate backup restoration on the same device.
- [ ] Investigate backup restoration on another device.
- [ ] Investigate backup restoration with a different SIM.
- [ ] Investigate backup restoration with existing messages.
- [ ] Investigate backup restoration after a clean installation.
- [ ] Investigate backup restoration in Secondary Profiles.
- [ ] Investigate interrupted backup operations.
- [ ] Investigate interrupted restoration operations.
- [ ] Investigate duplicate message prevention after restoration.
- [ ] Investigate application behavior after clearing application data.
- [ ] Investigate application behavior after uninstalling updates.

## Interruptions And Recovery

- [ ] Identify persistent sending queues.
- [ ] Identify persistent downloading queues.
- [ ] Identify background retry mechanisms.
- [ ] Identify pending operation recovery mechanisms.
- [ ] Investigate SMS recovery after process termination.
- [ ] Investigate MMS recovery after process termination.
- [ ] Investigate MMS download recovery after process termination.
- [ ] Investigate attachment processing recovery after process termination.
- [ ] Investigate pending operation recovery after device reboot.
- [ ] Investigate sending recovery after application updates.
- [ ] Investigate sending recovery after network loss.
- [ ] Investigate sending recovery after SIM removal.
- [ ] Investigate sending recovery after subscription changes.
- [ ] Investigate sending recovery after default SMS application changes.
- [ ] Investigate sending recovery after permission revocation.
- [ ] Investigate pending operations during Doze mode.
- [ ] Investigate pending operations under battery restrictions.
- [ ] Investigate pending operations during profile switching.
- [ ] Investigate pending operations after ending a profile session.
- [ ] Investigate interrupted audio recording recovery.
- [ ] Investigate interrupted attachment saving recovery.
- [ ] Investigate duplicate sending prevention.
- [ ] Investigate duplicate downloading prevention.
- [ ] Investigate corrupted data prevention after interrupted operations.

## Satellite Messaging

- [ ] Identify Satellite Messaging-related source code.
- [ ] Identify supported satellite connection states.
- [ ] Identify supported satellite messaging services.
- [ ] Identify device-specific satellite restrictions.
- [ ] Identify carrier-specific satellite restrictions.
- [ ] Identify Android-version-specific satellite restrictions.
- [ ] Identify satellite-specific UI states.
- [ ] Investigate satellite availability detection.
- [ ] Investigate satellite registration detection.
- [ ] Investigate satellite messaging availability detection.
- [ ] Investigate text-only satellite messaging states.
- [ ] Investigate text-and-media satellite messaging states.
- [ ] Investigate unavailable satellite messaging states.
- [ ] Investigate transitions between cellular and satellite connectivity.
- [ ] Investigate satellite-specific SMS restrictions.
- [ ] Investigate satellite-specific MMS restrictions.
- [ ] Investigate satellite-specific group messaging restrictions.
- [ ] Investigate satellite-specific attachment restrictions.
- [ ] Investigate satellite-specific message size limits.
- [ ] Investigate satellite-specific sending errors.
- [ ] Investigate satellite-specific retry behavior.
- [ ] Investigate satellite connectivity simulation.
- [ ] Identify scenarios requiring satellite-capable physical devices.

## Debug Settings

- [ ] List all available Debug Settings.
- [ ] Identify Debug Settings entry points.
- [ ] Identify Debug Settings availability conditions.
- [ ] Investigate MMS configuration overrides.
- [ ] Investigate maximum MMS size overrides.
- [ ] Investigate attachment count overrides.
- [ ] Investigate recipient count overrides.
- [ ] Investigate carrier configuration overrides.
- [ ] Investigate subscription configuration overrides.
- [ ] Investigate self phone number overrides.
- [ ] Investigate SMS-to-MMS conversion threshold overrides.
- [ ] Investigate debug configuration persistence.
- [ ] Investigate debug configuration reset.
- [ ] Investigate debug configuration isolation between SIM cards.
- [ ] Investigate debug configuration isolation between user profiles.
- [ ] Verify that debug-only functionality is unavailable in release builds where required.

## Test Environment And Simulation

### Android Emulator

- [ ] Identify supported Android emulator versions.
- [ ] Identify supported emulator system images.
- [ ] Investigate SMS injection through emulator console commands.
- [ ] Investigate bulk SMS generation.
- [ ] Investigate SMS generation from multiple senders.
- [ ] Investigate SMS generation with international phone numbers.
- [ ] Investigate SMS generation with alphanumeric senders.
- [ ] Investigate SMS generation with short codes.
- [ ] Investigate multipart SMS simulation.
- [ ] Investigate Class 0 SMS simulation.
- [ ] Investigate SMS delivery report simulation.
- [ ] Investigate SMS sending failure simulation.
- [ ] Investigate MMS simulation.
- [ ] Investigate network loss simulation.
- [ ] Investigate network recovery simulation.
- [ ] Investigate low-memory simulation.
- [ ] Investigate low-storage simulation.
- [ ] Investigate SIM configuration simulation.
- [ ] Investigate multiple SIM simulation.
- [ ] Investigate carrier configuration simulation.
- [ ] Investigate roaming simulation.
- [ ] Investigate satellite connectivity simulation.

### Physical Devices

- [ ] Identify scenarios requiring physical SIM cards.
- [ ] Identify scenarios requiring eSIM.
- [ ] Identify scenarios requiring two active SIM cards.
- [ ] Identify scenarios requiring different mobile carriers.
- [ ] Identify scenarios requiring SIM cards from different countries.
- [ ] Identify scenarios requiring international roaming.
- [ ] Identify scenarios requiring multiple physical devices.
- [ ] Identify scenarios requiring satellite-capable devices.
- [ ] Investigate bulk test message generation on physical devices.
- [ ] Investigate test data insertion through ADB.
- [ ] Investigate test data insertion through a dedicated test application.
- [ ] Investigate telephony provider test data generation.
- [ ] Investigate safe removal of generated test data.
- [ ] Investigate automated contact generation.
- [ ] Investigate automated attachment dataset generation.
- [ ] Investigate reusable SMS and MMS test scripts.
- [ ] Investigate automated log collection.
- [ ] Investigate automated crash and ANR collection.
- [ ] Investigate automated memory usage measurements.
- [ ] Investigate automated battery usage measurements.
- [ ] Investigate reusable device configuration scripts.