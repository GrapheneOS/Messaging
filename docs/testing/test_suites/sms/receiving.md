# Receiving
---

## Receive an SMS from a saved contact

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS from an unknown phone number

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing the test phone number. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS from an international phone number

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing the test phone number. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS from an alphanumeric sender

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS from a short code

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS containing a verification code

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS containing a hyperlink

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing https://example.com/?a=1. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS containing a phone number

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing the test phone number. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS containing emojis

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing 🙂. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS containing Unicode characters

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing Привет. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive a multiline SMS

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing Line 1\nLine 2. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive a long SMS

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive multiple consecutive SMS messages

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive SMS messages containing identical content

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive SMS messages from multiple senders simultaneously

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS while the conversation list is open

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS while the application is in the background

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS while the application is closed

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS while the device is locked

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS after cellular network coverage is restored

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS with mobile data disabled

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive an SMS while internationally roaming

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Attempt to reply to an alphanumeric sender

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter QA-SMS-01 in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- No false successful delivery is shown. Empty or whitespace-only input must not create an unintended empty SMS; for unavailable transport record whether dispatch is blocked, queued or fails.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Verify that received messages are not duplicated

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Each distinct delivered SMS is represented by the correct sender and complete content; identical text alone must not be treated as a duplicate.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
