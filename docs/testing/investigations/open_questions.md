# Messaging - Open Questions

## Application

- [ ] Which Android versions are officially supported?
- [ ] Which devices are officially supported?
- [ ] Is Messaging expected to work on stock Android?
- [ ] Is Messaging expected to work on other Android-based operating systems?
- [ ] Which functionality should remain available when Messaging is not the default SMS application?
- [ ] Which permissions are mandatory?
- [ ] Which permissions are optional?
- [ ] What should happen after revoking mandatory permissions?
- [ ] Should the application restore the previously opened screen after process termination?
- [ ] Are there any differences between debug and release builds affecting functionality?

## Conversations

- [ ] What is the maximum number of pinned conversations?
- [ ] Can archived conversations remain pinned?
- [ ] What should happen when receiving a message in an archived conversation?
- [ ] What should happen when sending a message from an archived conversation?
- [ ] Should a conversation remain visible after deleting all its messages?
- [ ] Should an empty conversation be preserved if it contains a draft?
- [ ] How should conversations containing only failed messages be displayed?
- [ ] What should happen when receiving a message during conversation deletion?
- [ ] What determines whether two conversations belong to the same recipient?
- [ ] Are there any conversation count limits?
- [ ] Are there any message history limits?
- [ ] Is message history loaded incrementally?

## Contacts

- [ ] What phone number validation rules are used?
- [ ] Is international country calling code population supported?
- [ ] Which country calling code should be used when no SIM is available?
- [ ] Which country calling code should be used when two SIM cards from different countries are active?
- [ ] Does the system region affect phone number formatting?
- [ ] Does the selected SIM affect phone number formatting?
- [ ] How are national and international phone number formats matched?
- [ ] How are phone numbers containing leading zeros processed?
- [ ] How are phone numbers containing extensions processed?
- [ ] Are emergency numbers excluded from recipient selection?
- [ ] Are service numbers supported?
- [ ] Are USSD codes supported as recipients?
- [ ] Are email addresses supported as MMS recipients?
- [ ] Are contacts without phone numbers displayed?
- [ ] Are duplicate phone numbers displayed separately?
- [ ] How are contacts with multiple matching phone numbers handled?
- [ ] Which contact should be displayed when multiple contacts contain the same number?
- [ ] What determines the contact display name?
- [ ] What determines the default contact avatar?
- [ ] Should contact information update immediately after changes in Contacts?
- [ ] Should contact information remain cached after Contacts permission is revoked?
- [ ] Are contacts from other user profiles accessible?

## Message Composer

- [ ] Is there a maximum text input length?
- [ ] Is there a maximum draft size?
- [ ] Are whitespace-only messages allowed?
- [ ] Are messages containing only line breaks allowed?
- [ ] Is leading or trailing whitespace removed before sending?
- [ ] Are unsupported Unicode characters replaced or preserved?
- [ ] How are composite emojis counted?
- [ ] How are GSM-7 extension characters counted?
- [ ] How are Unicode characters outside the Basic Multilingual Plane counted?
- [ ] When should the character counter become visible?
- [ ] When should the SMS segment counter become visible?
- [ ] What determines the SMS-to-MMS conversion threshold?
- [ ] Does the conversion threshold depend on the carrier?
- [ ] Does the conversion threshold depend on the selected SIM?
- [ ] Should the draft be preserved after a sending failure?
- [ ] Should the draft be preserved after changing the selected SIM?
- [ ] Should attachment drafts survive device reboot?
- [ ] Should drafts be preserved after changing the default SMS application?
- [ ] How should existing drafts be handled when opening Messaging through Android Share?
- [ ] How should existing drafts be handled when opening Messaging through an external Intent?

## SMS

- [ ] Which SMS encodings are supported?
- [ ] Is automatic SMS encoding selection implemented?
- [ ] Are national language shift tables supported?
- [ ] Are binary SMS messages supported?
- [ ] Are Class 0 SMS messages supported on all devices?
- [ ] Are Class 1, Class 2, and Class 3 SMS messages handled differently?
- [ ] Are alphanumeric senders supported?
- [ ] Can users reply to alphanumeric senders?
- [ ] Are short codes supported for outgoing messages?
- [ ] Are premium SMS messages supported?
- [ ] Are there any restrictions on international SMS?
- [ ] What is the maximum number of SMS segments?
- [ ] What happens when one multipart SMS segment is missing?
- [ ] What happens when multipart SMS segments arrive out of order?
- [ ] What happens when duplicate SMS segments are received?
- [ ] Is there a timeout for multipart SMS assembly?
- [ ] Are incomplete multipart messages displayed?
- [ ] Are failed SMS messages retried automatically?
- [ ] Is there a retry count limit?
- [ ] Is there a retry timeout?
- [ ] Can a pending outgoing SMS be cancelled?
- [ ] What determines the Sent status?
- [ ] What determines the Delivered status?
- [ ] What determines the Failed status?
- [ ] How are delayed delivery reports handled?
- [ ] How are delivery reports handled after message deletion?
- [ ] Are SMS delivery reports supported by all carriers?
- [ ] Are SMS messages supported over Wi-Fi Calling?
- [ ] Are SMS messages supported over IMS?
- [ ] Are SMS messages supported through satellite connectivity?

## MMS

- [ ] Which attachment formats are officially supported?
- [ ] Which MIME types are supported?
- [ ] Which image formats are supported?
- [ ] Which video containers and codecs are supported?
- [ ] Which audio formats and codecs are supported?
- [ ] Are animated GIFs preserved during MMS processing?
- [ ] Are animated WebP images supported?
- [ ] Are HEIC and HEIF images supported?
- [ ] Are AVIF images supported?
- [ ] Are PDF attachments supported?
- [ ] Are arbitrary file attachments supported?
- [ ] Are multiple audio attachments supported?
- [ ] Can one video be combined with other attachment types?
- [ ] What determines the maximum MMS size?
- [ ] What determines the maximum attachment count?
- [ ] Does the MMS size limit include message text and metadata?
- [ ] What happens when the selected SIM changes the available MMS limits?
- [ ] What exactly does Send anyway bypass?
- [ ] Which validation rules cannot be bypassed using Send anyway?
- [ ] Which image compression algorithm is used?
- [ ] Are images resized automatically?
- [ ] Is image metadata removed during processing?
- [ ] Is video transcoding supported?
- [ ] Are audio attachments compressed?
- [ ] Is MMS sending supported when mobile data is disabled?
- [ ] Can MMS use a different SIM from the default mobile data SIM?
- [ ] Is MMS sending supported over Wi-Fi Calling?
- [ ] Is MMS downloading supported over Wi-Fi?
- [ ] Are MMS messages retried automatically?
- [ ] What is the maximum number of download retries?
- [ ] What is the MMS download timeout?
- [ ] How is MMS expiration determined?
- [ ] Can expired MMS messages be recovered?
- [ ] Are duplicate MMS notifications handled?
- [ ] Can MMS attachments be downloaded individually?
- [ ] Are MMS attachments available in Secondary Profiles?

## Group Messaging

- [ ] What determines the maximum group recipient count?
- [ ] Does the recipient limit depend on the selected SIM?
- [ ] Does the recipient limit depend on carrier configuration?
- [ ] Can a group contain email addresses?
- [ ] Can a group contain short codes?
- [ ] Can a group contain the user's own phone number?
- [ ] Can participants be added to an existing group?
- [ ] Can participants be removed from an existing group?
- [ ] Does changing participants create a new conversation?
- [ ] Does changing participant order affect conversation matching?
- [ ] Can users rename group conversations?
- [ ] Can users leave a group conversation?
- [ ] How are incoming messages matched to existing groups?
- [ ] How are duplicate group participants handled?
- [ ] What happens when one group participant cannot receive MMS?
- [ ] What happens when only some individual SMS messages are successfully sent?
- [ ] How is the self phone number determined?
- [ ] Is manual self phone number configuration required for all carriers?

## Message Management

- [ ] Which actions are available for incoming messages?
- [ ] Which actions are available for outgoing messages?
- [ ] Which actions are available for failed messages?
- [ ] Which actions are available for pending messages?
- [ ] Which actions are available for undownloaded MMS?
- [ ] Which actions are available when multiple messages are selected?
- [ ] Is partial text selection supported?
- [ ] Is forwarding multiple messages supported?
- [ ] Is forwarding multiple attachments supported?
- [ ] Can failed messages be edited before resending?
- [ ] Can pending messages be deleted?
- [ ] Does deleting a pending message cancel the sending operation?
- [ ] Does deleting an MMS remove its locally stored attachments?
- [ ] Can deleted messages be restored?
- [ ] Which information should be displayed in Message Details?
- [ ] Should Message Details include carrier information?
- [ ] Should Message Details include SIM information?
- [ ] Should Message Details include delivery timestamps?

## Attachments And Media

- [ ] Which file sources are supported?
- [ ] Which attachment selection methods are supported?
- [ ] Is attachment selection from cloud storage supported?
- [ ] Is attachment selection from external storage supported?
- [ ] Is drag-and-drop attachment selection supported?
- [ ] Can users add captions to individual attachments?
- [ ] Can users reorder attachments?
- [ ] Can users edit images before sending?
- [ ] Can users crop images before sending?
- [ ] Can users record video directly from Messaging?
- [ ] Can users switch between front and rear cameras?
- [ ] Is camera flash control available?
- [ ] What is the maximum audio recording duration?
- [ ] What is the maximum audio recording size?
- [ ] What happens when audio recording reaches its limit?
- [ ] Is audio recording allowed in the background?
- [ ] Is audio playback allowed in the background?
- [ ] Is simultaneous audio playback supported?
- [ ] Which gestures are supported in Media Viewer?
- [ ] Is image zoom supported?
- [ ] Is double-tap zoom supported?
- [ ] Is video seeking supported?
- [ ] Is attachment saving supported?
- [ ] Where are saved attachments stored?
- [ ] How are duplicate attachment filenames handled?
- [ ] Are temporary attachment files deleted automatically?
- [ ] When are temporary attachment files deleted?
- [ ] Which vCard versions are supported?
- [ ] Which vCard fields are supported?
- [ ] Can multiple contacts be attached to one message?

## Notifications

- [ ] Which notification channels are created by Messaging?
- [ ] Which notification actions are supported?
- [ ] Is Mark as read supported?
- [ ] Is copying verification codes from notifications supported?
- [ ] Should notifications appear while the corresponding conversation is open?
- [ ] Should notifications appear while the conversation list is open?
- [ ] Should notification dismissal mark messages as read?
- [ ] Should Quick Reply mark the original message as read?
- [ ] Should Quick Reply preserve existing conversation drafts?
- [ ] What happens when Quick Reply sending fails?
- [ ] How are multiple notifications grouped?
- [ ] Are there notification count limits?
- [ ] Are notification previews truncated?
- [ ] Should MMS attachment previews appear in notifications?
- [ ] Should notification avatars update after contact changes?
- [ ] Should existing notifications update after contact deletion?
- [ ] How does Snooze interact with disabled conversation notifications?
- [ ] Does Snooze persist after device reboot?
- [ ] Does Snooze persist after changing the system time?
- [ ] What happens to notifications when a sender is blocked?
- [ ] Should blocked sender messages remain accessible?
- [ ] Should blocked sender conversations be archived automatically?
- [ ] Should notifications from blocked senders appear after unblocking?

## Widgets

- [ ] What is the maximum number of supported widget instances?
- [ ] What are the minimum and maximum widget dimensions?
- [ ] How many conversations can the Conversation List Widget display?
- [ ] How many messages can the Conversation Widget display?
- [ ] Should archived conversations appear in the Conversation List Widget?
- [ ] Should blocked conversations appear in widgets?
- [ ] Should pinned conversations be prioritized in the Conversation List Widget?
- [ ] Should drafts appear in widget previews?
- [ ] Should failed messages appear in widgets?
- [ ] Should MMS attachments appear as previews or placeholders?
- [ ] What is the expected widget update delay?
- [ ] Should widgets update independently of notification settings?
- [ ] Should widgets update while Messaging is force-stopped?
- [ ] Should widgets update immediately after contact changes?
- [ ] Should contact avatars be cached?
- [ ] Should widget contact information remain visible after Contacts permission is revoked?
- [ ] What happens to a Conversation Widget after its associated conversation is deleted?
- [ ] What happens to a Conversation Widget after its associated conversation is recreated?
- [ ] Should widget configuration survive application updates?
- [ ] Should widget configuration survive OS updates?
- [ ] Are both widget types supported in Secondary Profiles?
- [ ] Are widgets supported in Work Profiles?

## Settings

- [ ] What are the default values for all application settings?
- [ ] Which settings are global?
- [ ] Which settings are SIM-specific?
- [ ] Which settings are conversation-specific?
- [ ] Which settings are user-profile-specific?
- [ ] Which settings depend on carrier configuration?
- [ ] Which settings should be hidden when no SIM is available?
- [ ] Which settings should be disabled in Secondary Profiles?
- [ ] Should settings persist after changing the default SMS application?
- [ ] Should SIM-specific settings persist after removing a SIM?
- [ ] Should SIM-specific settings be restored after reinserting the same SIM?
- [ ] How are settings handled when a SIM is replaced?
- [ ] Are blocked numbers shared with the system blocked number provider?
- [ ] Does blocking a contact block all associated phone numbers?
- [ ] Does blocking affect group messages?
- [ ] Are blocked numbers shared between user profiles?
- [ ] Are blocked numbers synchronized with the Phone application?

## SIM And eSIM

- [ ] Which physical SIM and eSIM combinations are officially supported?
- [ ] Is Dual SIM Dual Standby supported?
- [ ] Is Dual SIM Dual Active supported on compatible hardware?
- [ ] Is there a default SIM selection for new conversations?
- [ ] Is the last selected SIM stored per conversation?
- [ ] Should the selected SIM persist after application restart?
- [ ] Should the selected SIM persist after device reboot?
- [ ] What happens when the selected SIM is disabled?
- [ ] Is another SIM selected automatically when the selected SIM becomes unavailable?
- [ ] Should sending be blocked until the user selects an available SIM?
- [ ] How are incoming messages associated with SIM cards?
- [ ] How are messages associated with removed SIM cards?
- [ ] What happens when subscription IDs change?
- [ ] What happens when two SIM cards have identical phone numbers?
- [ ] What happens when the SIM phone number is unavailable?
- [ ] How does the system default SMS SIM affect existing conversations?
- [ ] How does the default mobile data SIM affect MMS?
- [ ] How does roaming affect SMS and MMS behavior?
- [ ] Which carrier-specific configurations affect Messaging?
- [ ] Can carrier configurations be overridden for testing?

## User Profiles

- [ ] Which Android user profile types are supported?
- [ ] Is Messaging supported in Secondary Profiles?
- [ ] Is Messaging supported in Work Profiles?
- [ ] Is Messaging supported in Private Space?
- [ ] Is Messaging supported in Guest Profiles?
- [ ] Can Messaging be the default SMS application in Secondary Profiles?
- [ ] Can Secondary Profiles send SMS?
- [ ] Can Secondary Profiles send MMS?
- [ ] Can Secondary Profiles receive SMS?
- [ ] Can Secondary Profiles receive MMS?
- [ ] Which MMS operations are restricted to the Owner Profile?
- [ ] Is message history shared between profiles?
- [ ] Are read and unread states shared between profiles?
- [ ] Are deleted messages synchronized between profiles?
- [ ] Are archived conversations synchronized between profiles?
- [ ] Are pinned conversations synchronized between profiles?
- [ ] Are drafts isolated between profiles?
- [ ] Are blocked contacts isolated between profiles?
- [ ] Are application settings isolated between profiles?
- [ ] Are notification settings isolated between profiles?
- [ ] Are SIM selections isolated between profiles?
- [ ] Are attachment files accessible across profiles?
- [ ] What happens when a Secondary Profile session is ended?
- [ ] What happens to messages received while a profile session is ended?
- [ ] What happens when access to phone calls and SMS is revoked?
- [ ] What happens when a Secondary Profile is deleted?
- [ ] Are there restrictions when multiple Secondary Profiles are active?

## System Integration

- [ ] Which Android Intent actions are supported?
- [ ] Which Android Intent URI schemes are supported?
- [ ] Are sms, smsto, mms, and mmsto URI schemes supported?
- [ ] Which MIME types are supported through Android Share?
- [ ] Is ACTION_SEND_MULTIPLE supported?
- [ ] Can external applications prefill multiple recipients?
- [ ] Can external applications prefill a message subject?
- [ ] Can external applications prefill attachments?
- [ ] How are malformed external Intents handled?
- [ ] How are inaccessible content URIs handled?
- [ ] Are persistable URI permissions required for attachment drafts?
- [ ] Is Direct Share supported?
- [ ] Are conversation shortcuts supported?
- [ ] Are launcher shortcuts supported?
- [ ] Is Android conversation notification integration supported?
- [ ] Are notification bubbles supported?
- [ ] Is Android Auto integration supported?
- [ ] Is smartwatch integration supported?
- [ ] Is external Bluetooth message access supported?

## Security And Privacy

### Link Handling

- [ ] Which URL schemes are recognized?
- [ ] Are HTTP and HTTPS handled differently?
- [ ] Are URLs without a protocol recognized?
- [ ] Are internationalized domain names recognized?
- [ ] Are IP-based URLs recognized?
- [ ] Are URLs containing custom ports recognized?
- [ ] Are URLs containing authentication information recognized?
- [ ] Are mailto links recognized?
- [ ] Are tel links recognized?
- [ ] Are geo links recognized?
- [ ] Are intent links supported?
- [ ] Are custom application schemes supported?
- [ ] Are local file links explicitly blocked?
- [ ] Are content URIs explicitly blocked?
- [ ] How are malformed URLs handled?
- [ ] How are Unicode domain names displayed?
- [ ] How are potentially misleading URLs displayed?
- [ ] Does the confirmation dialog display the complete URL?
- [ ] Are long URLs truncated in the confirmation dialog?
- [ ] Can users copy links when link opening is disabled?
- [ ] Are verified Android App Links supported?
- [ ] Does Messaging always open the browser or can it open another registered application?
- [ ] Are network requests made before the user confirms opening a link?

### Link Previews

- [ ] Are YouTube previews the only supported link previews?
- [ ] Which YouTube URL formats are supported?
- [ ] Are YouTube Shorts supported?
- [ ] Are YouTube playlists supported?
- [ ] Are YouTube live streams supported?
- [ ] Are multiple previews supported in one message?
- [ ] Are previews generated for outgoing messages?
- [ ] Are previews generated for previously received messages?
- [ ] When are preview network requests initiated?
- [ ] Which external services receive preview requests?
- [ ] Which message data is included in preview requests?
- [ ] Are previews cached?
- [ ] What is the preview cache lifetime?
- [ ] Are cached previews removed when previews are disabled?
- [ ] Are failed preview requests retried automatically?

### Data Privacy

- [ ] Which message data is stored locally?
- [ ] Which message data is stored in the system telephony provider?
- [ ] Which application data is stored separately?
- [ ] Which data is available before the first device unlock?
- [ ] Which data is available while the device is locked?
- [ ] Which data is accessible to other applications?
- [ ] Are sensitive message contents included in logs?
- [ ] Are phone numbers included in diagnostic logs?
- [ ] Are attachment filenames included in logs?
- [ ] Is attachment metadata removed before sending?
- [ ] Are deleted attachment files removed from application storage?
- [ ] Are temporary files accessible to external applications?
- [ ] Does Messaging initiate any external network requests unrelated to MMS?
- [ ] Are screenshots restricted on any Messaging screens?
- [ ] Are sensitive previews visible in Recent Apps?
- [ ] Is clipboard content cleared or marked as sensitive?

## Performance And Stability

- [ ] Are there defined application startup performance targets?
- [ ] Are there defined conversation loading performance targets?
- [ ] Are there defined widget update performance targets?
- [ ] Are there defined contact search performance targets?
- [ ] Are there defined attachment processing performance targets?
- [ ] What is the maximum supported message history size?
- [ ] What is the maximum supported conversation count?
- [ ] What is the maximum supported contact count?
- [ ] Is message history pagination implemented?
- [ ] Are attachment previews loaded lazily?
- [ ] Are there defined memory consumption limits?
- [ ] Are there defined battery consumption targets?
- [ ] What should happen when storage is full?
- [ ] What is the expected behavior under low-memory conditions?
- [ ] Is there an automatic cleanup mechanism for cached media?
- [ ] Is there an automatic cleanup mechanism for temporary files?

## Accessibility And Localization

- [ ] Which accessibility features are officially supported?
- [ ] Is full TalkBack navigation expected?
- [ ] Is Switch Access supported?
- [ ] Is external keyboard navigation supported?
- [ ] Are accessibility actions available for swipe-only functionality?
- [ ] What is the maximum supported system font scale?
- [ ] Are all screens expected to support RTL layouts?
- [ ] Which languages are officially supported?
- [ ] Which locale determines phone number formatting?
- [ ] Which locale determines date and time formatting?
- [ ] Are accessibility labels available for all icon-only buttons?
- [ ] Are message status changes announced by accessibility services?

## Compatibility

- [ ] Which screen sizes are officially supported?
- [ ] Is tablet support required?
- [ ] Is foldable device support required?
- [ ] Which screen width activates the two-pane layout?
- [ ] Is landscape orientation supported on all screens?
- [ ] Is Split Screen officially supported?
- [ ] Is desktop windowing supported?
- [ ] Is Predictive Back supported on all screens?
- [ ] Which screens use custom Back navigation?
- [ ] Is dynamic color theming supported?
- [ ] Are high-contrast system settings supported?
- [ ] Are third-party keyboards officially supported?
- [ ] Is physical keyboard navigation supported?

## Updates And Data Persistence

- [ ] Which previous application versions must support direct upgrades?
- [ ] Is migration from Legacy Messaging to Compose Messaging supported?
- [ ] Which Legacy settings should be migrated?
- [ ] Which Legacy drafts should be migrated?
- [ ] Should Legacy widgets be preserved or recreated?
- [ ] Should pinned conversations be preserved during migration?
- [ ] Should archived conversations be preserved during migration?
- [ ] Should blocked contacts be preserved during migration?
- [ ] Should notification settings be preserved during migration?
- [ ] Are database migrations reversible?
- [ ] Is application downgrade supported?
- [ ] What happens when an application update fails?
- [ ] Is Android backup supported?
- [ ] Is Seedvault backup supported?
- [ ] Which Messaging data is included in backups?
- [ ] Which Messaging data is excluded from backups?
- [ ] Is restoring data to another device supported?
- [ ] Is restoring data to another user profile supported?
- [ ] How is duplicate data prevented after restoration?
- [ ] What happens after clearing Messaging application data?
- [ ] What happens after uninstalling application updates?
- [ ] How is message history synchronized with another default SMS application?

## Interruptions And Recovery

- [ ] What happens to pending SMS after application process termination?
- [ ] What happens to pending MMS after application process termination?
- [ ] What happens to active MMS downloads after process termination?
- [ ] What happens to pending messages after device reboot?
- [ ] Are interrupted sending operations resumed automatically?
- [ ] Are interrupted MMS downloads resumed automatically?
- [ ] Are interrupted attachment processing operations resumed automatically?
- [ ] What happens when the default SMS application changes during sending?
- [ ] What happens when the selected SIM changes during sending?
- [ ] What happens when mobile data changes during MMS downloading?
- [ ] What happens when the device enters Doze mode during sending?
- [ ] What happens when battery restrictions are applied during sending?
- [ ] What happens when a user profile session ends during sending?
- [ ] How are duplicate messages prevented after interrupted operations?

## Satellite Messaging

- [ ] Is Satellite Messaging currently supported?
- [ ] Which devices support Satellite Messaging?
- [ ] Which Android versions support Satellite Messaging?
- [ ] Which carriers support Satellite Messaging?
- [ ] Which countries and regions support Satellite Messaging?
- [ ] Is satellite connectivity available for SMS?
- [ ] Is satellite connectivity available for MMS?
- [ ] Are group messages supported through satellite connectivity?
- [ ] Are attachments supported through satellite connectivity?
- [ ] Are there satellite-specific message size limits?
- [ ] Are there satellite-specific recipient restrictions?
- [ ] How is satellite availability detected?
- [ ] How is satellite connectivity displayed?
- [ ] Is switching between cellular and satellite connectivity automatic?
- [ ] Can satellite connectivity be simulated for QA?

## Debug Settings And Test Environment

- [ ] Which Debug Settings are available?
- [ ] Are Debug Settings available only in debug builds?
- [ ] How can Debug Settings be enabled?
- [ ] Can carrier configurations be overridden?
- [ ] Can MMS size limits be overridden?
- [ ] Can recipient count limits be overridden?
- [ ] Can attachment count limits be overridden?
- [ ] Can SIM configurations be simulated?
- [ ] Can multiple active SIM cards be simulated?
- [ ] Can SMS delivery reports be simulated?
- [ ] Can SMS sending failures be simulated?
- [ ] Can MMS sending failures be simulated?
- [ ] Can MMS downloading failures be simulated?
- [ ] Can multipart SMS segments be injected individually?
- [ ] Can Class 0 SMS messages be generated locally?
- [ ] Can alphanumeric sender messages be generated locally?
- [ ] Can satellite connectivity be simulated?
- [ ] Can large message histories be generated without external services?
- [ ] Can test data be imported directly into the telephony provider?
- [ ] Which scenarios require physical devices?
- [ ] Which scenarios can be covered using an Android emulator?
- [ ] Which scenarios require multiple physical devices?
- [ ] Which scenarios require carrier-specific test SIM cards?
- [ ] Which existing automated tests can be reused for QA?