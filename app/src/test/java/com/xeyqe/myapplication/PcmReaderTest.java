package com.xeyqe.myapplication;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

public class PcmReaderTest {
    @Test public void resamplesStereoAcrossSingleByteChunks() throws IOException {
        ByteBuffer data = ByteBuffer.allocate(441 * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 441; i++) data.putShort((short) 16384).putShort((short) 0);
        InputStream chunks = new FilterInputStream(new ByteArrayInputStream(data.array())) {
            @Override public int read(byte[] bytes, int offset, int length) throws IOException {
                return super.read(bytes, offset, Math.min(length, 1));
            }
        };
        try (PcmReader reader = new PcmReader(chunks, 44_100, 2, PcmReader.Encoding.PCM_16BIT)) {
            short[] output = new short[137];
            int count, total = 0;
            while ((count = reader.read(output)) > 0) {
                for (int i = 0; i < count; i++) assertEquals(8192, output[i]);
                total += count;
            }
            assertEquals(480, total);
        }
    }

    @Test public void interpolatesAndFlushesTheLastFrameAtEndOfStream() throws IOException {
        byte[] data = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) 0).putShort((short) 16384).array();
        try (PcmReader reader = reader(data, 24_000, PcmReader.Encoding.PCM_16BIT)) {
            short[] output = new short[4];
            assertEquals(4, reader.read(output));
            assertArrayEquals(new short[]{0, 8192, 16384, 16384}, output);
            assertEquals(0, reader.read(output));
        }
    }

    @Test public void convertsUnsigned8BitAndFloatingPointPcm() throws IOException {
        try (PcmReader reader = reader(new byte[]{0, (byte) 255}, 48_000, PcmReader.Encoding.PCM_8BIT)) {
            short[] output = new short[2];
            assertEquals(2, reader.read(output));
            assertArrayEquals(new short[]{Short.MIN_VALUE, 32512}, output);
        }
        byte[] floats = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(0.5f).putFloat(-0.5f).array();
        try (PcmReader reader = reader(floats, 48_000, PcmReader.Encoding.PCM_FLOAT)) {
            short[] output = new short[2];
            assertEquals(2, reader.read(output));
            assertArrayEquals(new short[]{16384, -16384}, output);
        }
    }

    @Test public void rejectsPartialFramesEmptyAndInvalidAudio() throws IOException {
        try (PcmReader reader = reader(new byte[]{0}, 48_000, PcmReader.Encoding.PCM_16BIT)) {
            assertThrows(IOException.class, () -> reader.read(new short[960]));
        }
        try (PcmReader reader = reader(new byte[0], 48_000, PcmReader.Encoding.PCM_16BIT)) {
            assertThrows(IOException.class, () -> reader.read(new short[960]));
        }
        byte[] nan = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(Float.NaN).array();
        try (PcmReader reader = reader(nan, 48_000, PcmReader.Encoding.PCM_FLOAT)) {
            assertThrows(IOException.class, () -> reader.read(new short[960]));
        }
        assertThrows(IOException.class, () -> reader(new byte[0], 0, PcmReader.Encoding.PCM_16BIT));
    }

    private PcmReader reader(byte[] bytes, int rate, PcmReader.Encoding encoding) throws IOException {
        return new PcmReader(new ByteArrayInputStream(bytes), rate, 1, encoding);
    }
}
