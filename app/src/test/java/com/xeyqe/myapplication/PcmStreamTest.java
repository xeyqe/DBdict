package com.xeyqe.myapplication;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PcmStreamTest {
    @Test public void preservesBytesAcrossChunksAndCopiesCallbackBuffers() throws IOException {
        try (PcmStream stream = new PcmStream()) {
            byte[] first = {1, 2};
            assertTrue(stream.offer(first));
            first[0] = 99;
            assertTrue(stream.offer(new byte[]{3}));
            stream.finish();
            assertEquals(1, stream.read());
            assertEquals(2, stream.read());
            assertEquals(3, stream.read());
            assertEquals(-1, stream.read());
            assertFalse(stream.offer(new byte[]{4}));
        }
    }

    @Test public void waitsForLiveAudioInsteadOfTreatingAnEmptyBufferAsEnd() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (PcmStream stream = new PcmStream()) {
            CountDownLatch reading = new CountDownLatch(1);
            Future<Integer> sample = worker.submit(() -> { reading.countDown(); return stream.read(); });
            assertTrue(reading.await(2, TimeUnit.SECONDS));
            assertTrue(stream.offer(new byte[]{42}));
            assertEquals(42, sample.get(2, TimeUnit.SECONDS).intValue());
            stream.finish();
            assertEquals(-1, stream.read());
        } finally {
            worker.shutdownNow();
        }
    }

    @Test public void cancellationWakesTheEncoderWithAnError() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (PcmStream stream = new PcmStream()) {
            CountDownLatch reading = new CountDownLatch(1);
            Future<Integer> sample = worker.submit(() -> { reading.countDown(); return stream.read(); });
            assertTrue(reading.await(2, TimeUnit.SECONDS));
            stream.fail(new IOException("cancelled"));
            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> sample.get(2, TimeUnit.SECONDS));
            assertTrue(error.getCause() instanceof IOException);
        } finally {
            worker.shutdownNow();
        }
    }

    @Test public void bufferOverflowFailsInsteadOfDroppingAudio() throws IOException {
        try (PcmStream stream = new PcmStream()) {
            assertFalse(stream.offer(new byte[1_048_577]));
            assertThrows(IOException.class, stream::read);
        }
    }
}
