# Messaging QA Documentation

QA documentation for the GrapheneOS Messaging application.

## Documentation Structure

| Directory | Description |
| --- | --- |
| [Test Plans](test_plans/) | Overall testing strategy and functional coverage. |
| [Test Suites](test_suites/) | Detailed functional and non-functional test scenarios. |
| [Test Checklists](test_checklists/) | Smoke, regression, and release testing checklists. |
| [Test Resources](test_resources/) | Test configurations, test data, attachments, and scripts. |
| [Guides](guides/) | Environment setup, test data generation, and troubleshooting. |

## Test Plans

- [Master Test Plan](test_plans/master_test_plan.md)
- [Functional Test Plan](test_plans/functional_test_plan.md)

## Testing Principles

- Cover as much functional and non-functional behavior as possible.
- Prioritize testing based on risk and impact.
- Cover positive, negative, boundary, and edge-case scenarios.
- Avoid duplication of test cases and documentation.
- Reuse existing test cases across different test runs.
- Keep documentation concise and practical.
- Check system-specific behavior on GrapheneOS devices.
- Use emulators where relevant and physical devices for real telephony scenarios.
- Track defects and PR validation results directly in GitHub.
- Update test coverage as functionality evolves.

## Planned Functionality

Planned features are documented separately from implemented
functionality and are not included in active test runs until
their requirements and implementation are available.

- [Message Search](test_suites/search/README.md)

## References

- [Messaging Repository](https://github.com/GrapheneOS/Messaging)
- [Messaging Issues](https://github.com/GrapheneOS/Messaging/issues)
- [Messaging Pull Requests](https://github.com/GrapheneOS/Messaging/pulls)
- [GrapheneOS Issue Tracker](https://github.com/GrapheneOS/os-issue-tracker)
