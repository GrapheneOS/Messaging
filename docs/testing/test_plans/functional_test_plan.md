# Messaging - Functional Test Plan

## 1. Purpose

Describe what to test in GrapheneOS Messaging, using the `main` branch as the reference.

See the corresponding test suites for detailed scenarios.

## 2. Functional Coverage

| ID | Functional Area | Coverage | Priority |
| --- | --- | --- | --- |
| F01 | Application | Launch, onboarding, default SMS role, permissions, lifecycle, navigation | P0 |
| F02 | Conversations | Conversation list, creation, history, previews, selection, deletion, read states | P0 |
| F03 | Message Composer | Text input, editing, drafts, keyboard, Unicode, emoji, segment counter | P0 |
| F04 | SMS | Sending, receiving, multipart messages, delivery reports, failures, retry | P0 |
| F05 | MMS | Sending, receiving, downloading, attachments, network conditions, errors | P0 |
| F06 | Group Messaging | Recipient selection, group SMS, group MMS, participants, group settings | P1 |
| F07 | Message Management | Selection, copying, forwarding, details, deletion, message actions | P1 |
| F08 | Contacts | Recipient search, phone numbers, names, avatars, contact updates | P1 |
| F09 | Attachments and Media | Images, videos, audio, vCards, camera, recording, saving, sharing | P1 |
| F10 | Notifications | Incoming messages, failures, grouping, actions, permissions, privacy | P0 |
| F11 | Widgets | Conversation list widget, conversation widget, updates, navigation | P1 |
| F12 | Settings | General, advanced, SMS, MMS, privacy, conversation and SIM settings | P1 |
| F13 | SIM and eSIM | Subscription selection, multiple SIMs, inactive SIMs, configuration changes | P0 |
| F14 | User Profiles | Owner, secondary users, restrictions, profile-specific behavior | P0 |
| F15 | System Integration | Telephony, Contacts, Launcher, intents, sharing, media providers | P1 |
| F16 | Blocking | Blocking, unblocking, blocked conversation behavior | P1 |
| F17 | Notification Suppression | Mute, snooze, expiration, notification recovery | P1 |
| F18 | Link Handling | Opening modes, confirmation, copying, link previews | P1 |
| F19 | Conversation Organization | Pinning, archiving, unpinning, unarchiving | P1 |
| F20 | Satellite Messaging | Supported connection states and messaging restrictions | P2 |

Features identified through resources or legacy components require implementation verification before inclusion in active test runs.

## 3. Non-functional Coverage

| ID | Area | Coverage | Priority |
| --- | --- | --- | --- |
| NF01 | Security and Privacy | Permissions, profile isolation, sensitive data, external access | P0 |
| NF02 | Performance and Stability | Large datasets, scrolling, launch time, crashes, ANR, memory | P1 |
| NF03 | Accessibility | TalkBack, focus, labels, touch targets, font scaling | P1 |
| NF04 | Localization | Translations, RTL, dates, time, Unicode, plural forms | P2 |
| NF05 | Compatibility | Devices, OS versions, themes, navigation modes, screen sizes | P1 |
| NF06 | Adaptive Layout | Portrait, landscape, tablets, two-pane layouts | P1 |
| NF07 | Updates and Persistence | Application updates, OS updates, messages, drafts, settings | P0 |
| NF08 | Interruptions and Recovery | Calls, network loss, reboot, process termination, low storage | P0 |

## 4. Test Suites

| Test Suite | Location |
| --- | --- |
| Application | [application](../test_suites/application/) |
| Conversations | [conversations](../test_suites/conversations/) |
| Message Composer | [message_composer](../test_suites/message_composer/) |
| SMS | [sms](../test_suites/sms/) |
| MMS | [mms](../test_suites/mms/) |
| Group Messaging | [group_messaging](../test_suites/group_messaging/) |
| Message Management | [message_management](../test_suites/message_management/) |
| Contacts | [contacts](../test_suites/contacts/) |
| Attachments and Media | [attachments_and_media](../test_suites/attachments_and_media/) |
| Notifications | [notifications](../test_suites/notifications/) |
| Widgets | [widgets](../test_suites/widgets/) |
| Settings | [settings](../test_suites/settings/) |
| SIM and eSIM | [sim_and_esim](../test_suites/sim_and_esim/) |
| User Profiles | [user_profiles](../test_suites/user_profiles/) |
| System Integration | [system_integration](../test_suites/system_integration/) |
| Security and Privacy | [security_and_privacy](../test_suites/security_and_privacy/) |
| Performance and Stability | [performance_and_stability](../test_suites/performance_and_stability/) |
| Accessibility and Localization | [accessibility_and_localization](../test_suites/accessibility_and_localization/) |
| Compatibility | [compatibility](../test_suites/compatibility/) |
| Updates and Data Persistence | [updates_and_data_persistence](../test_suites/updates_and_data_persistence/) |
| Interruptions and Recovery | [interruptions_and_recovery](../test_suites/interruptions_and_recovery/) |

## 5. Testing Across Configurations

Run relevant tests across the following configurations:

- Owner and secondary user profiles.
- No SIM, single SIM, and multiple SIM configurations.
- Available mobile network conditions.
- Granted, denied, and revoked permissions.
- Foreground and background application states.
- Locked and unlocked devices.
- Cold and warm application starts.
- Light and dark themes.
- Portrait and landscape orientations.
- Gesture and three-button navigation.
- Different font and display sizes.
- Different languages and locales.
- Empty, normal, and large datasets.

Choose configurations based on risk, feature dependencies, and the expected impact of changes.

Reference: [Test Configurations](../test_resources/test_configurations.md)

## 6. Planned Functionality

| Feature | Status | Reference |
| --- | --- | --- |
| Message history search | Planned | [Search](../test_suites/search/README.md) |

Recipient search and conversation selection search are separate from message history search.

## 7. Features Requiring Further Verification

The following areas require additional implementation or environment verification:

- Pin and unpin.
- Archive and unarchive.
- Blocking behavior.
- Snooze and mute interactions.
- Satellite messaging.
- Class 0 SMS.
- Work profile behavior.
- Debug SIM emulation.
- Debug MMS configuration.
- Backup and restore.
- Legacy data migration.
- Carrier-specific delivery and download behavior.

Confirmed functionality should be moved into the corresponding active test suite.

## 8. Test Design

Each test suite should contain applicable:

- Positive scenarios.
- Negative scenarios.
- Boundary values.
- Edge cases.
- State transitions.
- UI and UX checks.
- Permission-related scenarios.
- Data persistence checks.
- Interruption and recovery scenarios.
- Integration checks.
- Compatibility checks.

Detailed test cases are required when a checklist item cannot sufficiently describe the scenario and its expected behavior.

Test cases should not be duplicated between functional, smoke, regression, and release documentation.

## 9. Maintenance

Test coverage must be updated when functionality in `main` changes.

Newly implemented features should be added to the corresponding test suite.

Removed functionality should be excluded from active test runs.

Relevant defects should result in additional regression scenarios where existing coverage is insufficient.