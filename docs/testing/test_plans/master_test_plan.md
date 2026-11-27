# Messaging - Master Test Plan

## 1. Overview

Messaging is the SMS and MMS application of GrapheneOS, originally based on AOSP Messaging and being rewritten using Kotlin and Jetpack Compose.

This test plan defines the overall testing strategy, scope, environments, test coverage, and quality criteria for the Messaging application.

The `main` branch is the primary baseline for functional test coverage.

## 2. Test Objectives

- Verify that SMS and MMS work correctly and reliably.
- Test all implemented user-facing features.
- Check integration with Android and GrapheneOS system components.
- Check that messages remain intact and user data is saved correctly.
- Prevent functional regressions.
- Check privacy and security-related behavior.
- Check app stability during normal use and unexpected situations.
- Verify compatibility across supported devices and configurations.
- Check accessibility, localization, and adaptive layouts.
- Ensure that application updates preserve existing functionality and user data.

## 3. Scope

### 3.1. Functional Testing

- Application launch and onboarding.
- Default SMS application configuration.
- Application permissions.
- Conversation management.
- Recipient selection and contacts.
- Message composition and drafts.
- SMS communication.
- MMS communication.
- Group messaging.
- Message management.
- Attachments and media.
- Notifications.
- Conversation and conversation list widgets.
- Application and conversation settings.
- SIM and eSIM management.
- Blocking and notification suppression.
- Privacy settings.
- System integration.
- User profile-specific behavior.

### 3.2. Non-functional Testing

- Performance.
- Stability and reliability.
- Security and privacy.
- Accessibility.
- Localization.
- Device and OS compatibility.
- Adaptive layouts.
- Data persistence.
- Update and migration behavior.
- Interruptions and recovery.
- Resource consumption.

### 3.3. Conditional Coverage

The following areas require specific device, carrier, build, or environment capabilities:

- Dual SIM and dual eSIM configurations.
- Carrier-specific MMS behavior.
- International roaming.
- Satellite messaging.
- Class 0 SMS.
- Debug-only functionality.
- Backup and restore.
- Work profile-specific behavior.

Feature availability and expected behavior must be confirmed before testing.

## 4. Out of Scope

- Planned functionality not implemented in the baseline build.
- Message history search until implementation is available.
- Features available exclusively in unmerged pull requests during baseline testing.
- Internal implementation of third-party applications.
- Carrier infrastructure beyond its interaction with Messaging.
- Android system functionality unrelated to Messaging.
- Formal security penetration testing.

Integration behavior remains within scope where it affects Messaging.

## 5. Test Basis

Use the following sources to design tests:

1. Source code from the `main` branch.
2. Application behavior in builds produced from `main`.
3. Android Manifest and application resources.
4. Existing unit and instrumentation tests.
5. Available product requirements and user stories.
6. Relevant GitHub issues.
7. Pull requests when clarification or change-specific testing is required.

The presence of a resource, permission, or legacy component does not independently confirm feature availability.

The tested commit, application build, OS build, and environment must be identifiable for each test run.

## 6. Test Strategy

Prioritize tests by risk while covering all implemented functionality.

Each applicable feature should include:

- Positive scenarios.
- Negative scenarios.
- Boundary value analysis.
- Edge cases.
- UI and UX validation.
- State transitions.
- Data integrity validation.
- Permission-related scenarios.
- System integration.
- Interruption and recovery.
- Compatibility.
- Accessibility and localization.

Simple scenarios are documented as checklist items.

Complex scenarios requiring specific preconditions, multiple steps, or detailed expected results are documented as test cases.

Existing test scenarios should be reused across smoke, regression, and release testing.

## 7. Test Types

### 7.1. Smoke Testing

Check critical features to determine whether the build is ready for further testing.

The smoke suite covers application launch, essential permissions, SMS and MMS communication, conversation management, notifications, and critical stability checks.

Reference: [Smoke Checklist](../test_checklists/smoke_checklist.md)

### 7.2. Functional Testing

Validate individual features, user flows, business logic, validation rules, error handling, and application states.

Reference: [Functional Test Plan](functional_test_plan.md)

### 7.3. Integration Testing

Validate interaction with:

- Android Telephony.
- Android Contacts.
- Android notification system.
- Android Launcher and widgets.
- Android permission management.
- SIM and subscription management.
- Camera and microphone.
- Media and file providers.
- Android sharing and intent handling.
- System settings.
- User profiles.

### 7.4. Exploratory Testing

Investigate unexpected behavior, complex state combinations, undocumented limitations, and potential regressions.

Relevant findings should be added to existing test suites.

### 7.5. Regression Testing

Validate existing functionality after application changes, bug fixes, dependency updates, and OS updates.

Regression scope is selected based on affected functionality, dependencies, and potential impact.

Reference: [Regression Checklist](../test_checklists/regression_checklist.md)

### 7.6. Release Testing

Validate the release candidate against critical functional, integration, compatibility, and stability requirements.

Reference: [Release Checklist](../test_checklists/release_checklist.md)

### 7.7. Non-functional Testing

Validate application performance, reliability, accessibility, privacy, security-related behavior, compatibility, and recovery.

Performance measurements should use reproducible configurations and representative datasets.

## 8. Test Environment

### 8.1. Physical Devices

GrapheneOS-supported physical devices should be used for:

- Real SMS and MMS communication.
- Carrier-specific behavior.
- SIM and eSIM configurations.
- Mobile network transitions.
- Notifications and widgets.
- User profile testing.
- Application and OS update validation.

### 8.2. Android Emulators

Emulators may be used for:

- UI and navigation testing.
- Simulated incoming SMS.
- Large test dataset generation.
- Screen size and orientation testing.
- Accessibility and localization.
- Application lifecycle testing.
- Supported debug scenarios.

Emulator results do not replace physical device validation of real telephony behavior.

### 8.3. Application Builds

- Builds from `main`.
- Debug builds.
- Release candidates.
- Released application versions.
- PR-specific builds when required.

Build artifacts should be identifiable by build number and target environment. Branch and commit identifiers should be included when relevant.

### 8.4. Test Configurations

Testing should cover applicable combinations of:

- Device model.
- OS version.
- Application version.
- SIM configuration.
- Carrier and network conditions.
- User profile.
- Permission state.
- Application lifecycle state.
- Screen orientation and size.
- Navigation mode.
- System theme.
- Language and locale.
- Dataset size.

Reference: [Test Configurations](../test_resources/test_configurations.md)

## 9. Test Data

Test data should include:

- Saved and unknown contacts.
- Contacts with multiple phone numbers.
- National and international phone numbers.
- Alphanumeric senders.
- Short codes.
- Plain text and multiline messages.
- GSM-7 and Unicode messages.
- Emoji and combined Unicode sequences.
- Multipart SMS.
- Long messages.
- Images, videos, audio files, and contact cards.
- Invalid and corrupted attachments.
- Empty and large conversation histories.
- Multiple simultaneous conversations.
- Synthetic datasets for performance testing.

Use synthetic or authorized test data. Personal messages and contact information must not be included in shared test artifacts.

Reference: [Test Data](../test_resources/test_data.md)

## 10. Test Priorities

| Priority | Description |
| --- | --- |
| P0 | Critical communication, message integrity, privacy, and application availability. |
| P1 | Major functional flows, settings, notifications, and system integration. |
| P2 | Secondary functionality, edge cases, compatibility, and UI behavior. |
| P3 | Low-risk visual details and uncommon configurations. |

Priority determines execution order, not whether a scenario should be documented.

## 11. Test Execution

### 11.1. Main Branch Testing

- Identify the baseline commit and build.
- Run smoke testing.
- Check implemented functionality.
- Investigate unexpected behavior.
- Run relevant regression scenarios.
- Update test coverage when functionality changes.

### 11.2. Change-specific Testing

- Review the affected functionality.
- Identify related components and dependencies.
- Check the intended change.
- Run relevant positive, negative, and edge-case scenarios.
- Perform targeted regression testing.
- Add reusable scenarios to the relevant test suite.

A complete historical PR review is not required for baseline testing.

### 11.3. Release Candidate Testing

- Confirm build and environment information.
- Run the smoke suite.
- Run the required regression suite.
- Check critical device and network configurations.
- Review unresolved defects and limitations.
- Record the release testing outcome.

## 12. Entry Criteria

- The target build is available.
- The tested commit or application version is identifiable.
- Required devices and environments are available.
- Required test data is prepared.
- The application can be installed or accessed.
- Expected behavior is sufficiently defined for the selected scenarios.

## 13. Exit Criteria

- All applicable P0 scenarios have been executed and passed.
- Required regression scenarios have been executed.
- No unresolved release-blocking defects remain.
- Known failures and blocked scenarios are documented.
- Message integrity and critical privacy-related behavior have been validated.
- Remaining risks are reviewed before release.

Blocked and unexecuted scenarios must not be reported as passed.

## 14. Test Results

Supported execution statuses:

- Passed.
- Failed.
- Blocked.
- Not Run.
- Not Applicable.

Planned functionality is excluded from active test execution until implementation is available.

Test results may be recorded directly in the relevant GitHub issue, pull request, or release testing activity.

Separate test reports are not required unless specifically requested.

## 15. Defect Management

Defects are tracked in GitHub.

Defect reports should include, where relevant:

- Clear title.
- Environment and build information.
- Preconditions.
- Steps to reproduce.
- Actual result.
- Expected result.
- Reproducibility.
- Relevant screenshots, recordings, or logs.
- Severity and priority.
- Affected configurations.

Sensitive information must be removed from attached evidence.

## 16. Risks and Limitations

- Differences between emulator and physical device behavior.
- Carrier-dependent SMS and MMS behavior.
- Limited availability of specific SIM configurations.
- Limited access to roaming and satellite networks.
- Differences between debug and release builds.
- Incomplete or evolving requirements.
- Legacy and Compose implementation differences.
- User profile-specific restrictions.
- OS-level behavior outside application control.
- Limited reproducibility of rare telephony failures.

Emergency messaging must only be tested using approved, controlled test environments.

## 17. Test Documentation

- [Functional Test Plan](functional_test_plan.md)
- [Test Suites](../test_suites/)
- [Smoke Checklist](../test_checklists/smoke_checklist.md)
- [Regression Checklist](../test_checklists/regression_checklist.md)
- [Release Checklist](../test_checklists/release_checklist.md)
- [Test Configurations](../test_resources/test_configurations.md)
- [Test Data](../test_resources/test_data.md)
- [Testing Guides](../guides/)

Test documentation should remain practical, reusable, and aligned with implemented functionality.

Avoid duplicate documentation and unnecessary reports.