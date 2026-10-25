package com.xeyqe.myapplication;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class OpusEncoderTest {
    @Test public void encodesTtsPcmAsPlayableOggOpus() throws Exception {
        File directory = InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir();
        File encoded = null;
        MediaExtractor extractor = new MediaExtractor();
        try (PcmStream stream = new PcmStream()) {
            int rate = 22_050;
            ByteBuffer data = ByteBuffer.allocate(rate * 2).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < rate; i++) {
                data.putShort((short) (Math.sin(2 * Math.PI * 440 * i / rate) * 16000));
            }
            CompletableFuture<File> encoding = CompletableFuture.supplyAsync(() -> {
                try {
                    return OpusEncoder.encode(directory, stream, rate, 1, PcmReader.Encoding.PCM_16BIT);
                } catch (java.io.IOException e) {
                    throw new java.util.concurrent.CompletionException(e);
                }
            });
            // Odd chunk sizes deliberately split 16-bit samples between callbacks.
            for (int offset = 0; offset < data.capacity(); offset += 777) {
                assertTrue(stream.offer(Arrays.copyOfRange(data.array(), offset,
                        Math.min(offset + 777, data.capacity()))));
            }
            stream.finish();
            encoded = encoding.get(10, TimeUnit.SECONDS);
            assertTrue(encoded.length() > 0);
            assertTrue(encoded.length() < data.capacity());
            extractor.setDataSource(encoded.getAbsolutePath());
            assertEquals(1, extractor.getTrackCount());
            MediaFormat format = extractor.getTrackFormat(0);
            assertEquals(MediaFormat.MIMETYPE_AUDIO_OPUS, format.getString(MediaFormat.KEY_MIME));
            assertEquals(48_000, format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
            assertEquals(1, format.getInteger(MediaFormat.KEY_CHANNEL_COUNT));
            extractor.selectTrack(0);
            assertTrue(extractor.readSampleData(ByteBuffer.allocate(8192), 0) > 0);
        } finally {
            extractor.release();
            if (encoded != null) encoded.delete();
        }
    }
}
