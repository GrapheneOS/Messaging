package com.android.messaging.mmslib.pdu;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

public class PduParserTest {
    @Test
    public void parseContentType_constrainedMediaWithLastKnownType_returnsKnownType() {
        final byte[] contentType = PduParser.parseContentType(
                new ByteArrayInputStream(new byte[] { (byte) 0xD2 }), null);

        assertArrayEquals("application/mikey".getBytes(StandardCharsets.UTF_8), contentType);
    }

    @Test
    public void parseContentType_constrainedMediaWithFirstUnknownType_returnsWildcard() {
        final byte[] contentType = PduParser.parseContentType(
                new ByteArrayInputStream(new byte[] { (byte) 0xD3 }), null);

        assertArrayEquals("*/*".getBytes(StandardCharsets.UTF_8), contentType);
    }

    @Test
    public void parseContentType_constrainedMediaWithHighestUnknownType_returnsWildcard() {
        final byte[] contentType = PduParser.parseContentType(
                new ByteArrayInputStream(new byte[] { (byte) 0xFF }), null);

        assertArrayEquals("*/*".getBytes(StandardCharsets.UTF_8), contentType);
    }

    @Test
    public void parse_malformedPushWithUnknownConstrainedMediaContentType_doesNotCrash() {
        final byte[] pdu = new byte[] { (byte) 0x8C, (byte) 0x82, (byte) 0x84, (byte) 0xD3 };

        assertNull(new PduParser(pdu, true).parse());
    }

    @Test
    public void parse_contentTypeParameterWithOversizedValueLength_doesNotCrash() {
        final byte[] pdu = new byte[] {
                (byte) 0x8C, (byte) 0x82, (byte) 0x84, (byte) 0x1F,
                (byte) 0x87, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x7F,
                (byte) 0x83, (byte) 0x00 };

        assertNull(new PduParser(pdu, true).parse());
    }

    @Test
    public void parse_malformedNotificationIndWithInvalidFromValueLength_doesNotCrash() {
        final byte[] pdu = new byte[] { (byte) 0x8C, (byte) 0x82, (byte) 0x89, (byte) 0x40 };

        assertNull(new PduParser(pdu, true).parse());
    }
}
