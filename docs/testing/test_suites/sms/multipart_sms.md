# Multipart Sms
---

## Send an SMS containing 159 GSM-7 characters

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter ASCII A repeated for each specified character count in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- When sending succeeds, the recipient receives the complete message and the sender sees the correct status.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Send an SMS containing 160 GSM-7 characters

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter ASCII A repeated for each specified character count in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- When sending succeeds, the recipient receives the complete message and the sender sees the correct status.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Send an SMS containing 161 GSM-7 characters

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter ASCII A repeated for each specified character count in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- When sending succeeds, the recipient receives the complete message and the sender sees the correct status.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Send SMS messages containing 305, 306, and 307 GSM-7 characters

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter ASCII A repeated for each specified character count in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- When sending succeeds, the recipient receives the complete message and the sender sees the correct status.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Send SMS messages containing 69, 70, and 71 UCS-2 code units

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter Cyrillic Ж repeated for each specified code-unit count in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- When sending succeeds, the recipient receives the complete message and the sender sees the correct status.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Send SMS messages containing 133, 134, and 135 UCS-2 code units

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter Cyrillic Ж repeated for each specified code-unit count in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- When sending succeeds, the recipient receives the complete message and the sender sees the correct status.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Send an SMS containing GSM-7 extension table characters

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter QA-SMS-01 in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- When sending succeeds, the recipient receives the complete message and the sender sees the correct status.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Update the number of SMS segments when adding a Unicode character

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter Привет in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Update the number of SMS segments when adding an emoji

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter 🙂 in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Update the number of SMS segments when removing a Unicode character

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter Привет in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive a multipart SMS

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Assemble message segments in the correct order

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive message segments with delays

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Receive message segments out of order

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. On the receiving device, prepare the conversation and application/network state named in the title.
2. From the test sender, send an SMS containing QA-SMS-01. Use the sender and timing required by this test.
3. Open the receiving conversation and compare sender, content and message count with the transmitted records.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Verify that message segments are not duplicated

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter QA-SMS-01 in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
---

## Preserve the complete content of a multipart message

### Preconditions

- Use test phone numbers and an active SIM that supports the message type, unless the test requires otherwise.
- Use a second test device or an approved test setup. Record the carrier and SIM.

### Steps

1. Open Messaging and select a recipient for this test.
2. Enter QA-SMS-01 in the composer. Set up the required network and SIM conditions before sending.
3. Tap Send once unless testing repeated taps. Check the message status and the receiving device.

### Expected Result

- Compare the reconstructed message with the original payload, including segment order and completeness. Record the encoding and segment count; carrier conversion thresholds may apply.

### Review Status

Draft: confirm the expected result against the implementation and on a test device before release testing.
