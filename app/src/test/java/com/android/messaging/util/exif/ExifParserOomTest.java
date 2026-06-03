package com.android.messaging.util.exif;

import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ExifParserOomTest {

    @Test
    public void readExif_tagValueSizeExceedingApp1Segment_isRejected() {
        final ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        tiff.write('M');
        tiff.write('M');
        be16(tiff, 0x002A);
        be32(tiff, 8);
        be16(tiff, 1);
        be16(tiff, 0x0111);
        be16(tiff, 4);
        be32(tiff, 0x0FFFFFFFL);
        be32(tiff, 26);
        be32(tiff, 0);
        for (int i = 0; i < 8; i++) {
            tiff.write(0);
        }

        assertThrows(IOException.class,
                () -> new ExifInterface().readExif(wrapTiffInJpeg(tiff.toByteArray())));
    }

    @Test
    public void readExif_ifd0OffsetExceedingApp1Segment_isRejected() {
        final ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        tiff.write('M');
        tiff.write('M');
        be16(tiff, 0x002A);
        be32(tiff, 0x7FFFFFFFL);

        assertThrows(IOException.class,
                () -> new ExifInterface().readExif(wrapTiffInJpeg(tiff.toByteArray())));
    }

    @Test
    public void readExif_dataAboveIfd0TagOffsetOutOfBounds_isRejected() {
        final ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        tiff.write('M');
        tiff.write('M');
        be16(tiff, 0x002A);
        be32(tiff, 20);
        for (int i = 0; i < 12; i++) {
            tiff.write(0);
        }
        be16(tiff, 1);
        be16(tiff, 0x9999);
        be16(tiff, 7);
        be32(tiff, 10);
        be32(tiff, 0);
        be32(tiff, 0);

        assertThrows(IOException.class,
                () -> new ExifInterface().readExif(wrapTiffInJpeg(tiff.toByteArray())));
    }

    private static byte[] wrapTiffInJpeg(final byte[] tiff) {
        final ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        jpeg.write(0xFF);
        jpeg.write(0xD8);
        jpeg.write(0xFF);
        jpeg.write(0xE1);
        be16(jpeg, 2 + 6 + tiff.length);
        jpeg.write('E');
        jpeg.write('x');
        jpeg.write('i');
        jpeg.write('f');
        jpeg.write(0);
        jpeg.write(0);
        jpeg.write(tiff, 0, tiff.length);
        jpeg.write(0xFF);
        jpeg.write(0xD9);
        return jpeg.toByteArray();
    }

    private static void be16(final ByteArrayOutputStream o, final int v) {
        o.write((v >> 8) & 0xFF);
        o.write(v & 0xFF);
    }

    private static void be32(final ByteArrayOutputStream o, final long v) {
        o.write((int) ((v >> 24) & 0xFF));
        o.write((int) ((v >> 16) & 0xFF));
        o.write((int) ((v >> 8) & 0xFF));
        o.write((int) (v & 0xFF));
    }
}
