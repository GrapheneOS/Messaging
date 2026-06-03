package com.android.messaging.mmslib.pdu;

import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PduPartsParserTest {

    @Test
    public void parseParts_partLengthExceedingRemainingData_isRejected() {
        final byte[] body = new byte[] {
                0x01, 0x01, (byte) 0x8F, (byte) 0xFF, (byte) 0xFF, 0x7F, (byte) 0x83, 0x41, 0x42 };

        assertNull(new PduParser(new byte[0], false).parseParts(new ByteArrayInputStream(body)));
    }
}
