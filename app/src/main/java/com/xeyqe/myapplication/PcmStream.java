package com.xeyqe.myapplication;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;

/** A bounded bridge between TTS callbacks and the background encoder. */
final class PcmStream extends InputStream {
    private static final int MAX_PENDING_BYTES = 1_048_576;
    private final ArrayDeque<byte[]> chunks = new ArrayDeque<>();
    private int pendingBytes;
    private int offset;
    private boolean finished;
    private boolean closed;
    private IOException failure;

    synchronized boolean offer(byte[] audio) {
        if (closed || finished || failure != null) return false;
        if (audio.length > MAX_PENDING_BYTES - pendingBytes) {
            fail(new IOException("TTS audio exceeded the streaming buffer"));
            return false;
        }
        if (audio.length > 0) {
            chunks.add(audio.clone());
            pendingBytes += audio.length;
            notifyAll();
        }
        return true;
    }

    synchronized void finish() {
        finished = true;
        notifyAll();
    }

    synchronized void fail(IOException error) {
        if (failure == null) failure = error;
        chunks.clear();
        pendingBytes = 0;
        notifyAll();
    }

    @Override public synchronized int read(byte[] buffer, int start, int length) throws IOException {
        if (start < 0 || length < 0 || start > buffer.length - length) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) return 0;
        while (chunks.isEmpty() && !finished && !closed && failure == null) {
            try {
                // A silent playback request can finish well after its last PCM chunk.
                // onDone, onError, onStop, or activity teardown will wake this reader.
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Audio encoding interrupted");
            }
        }
        if (failure != null) throw failure;
        if (closed) throw new IOException("Audio stream closed");
        if (chunks.isEmpty()) return -1;
        byte[] chunk = chunks.getFirst();
        int count = Math.min(length, chunk.length - offset);
        System.arraycopy(chunk, offset, buffer, start, count);
        pendingBytes -= count;
        offset += count;
        if (offset == chunk.length) {
            chunks.removeFirst();
            offset = 0;
        }
        return count;
    }

    @Override public int read() throws IOException {
        byte[] value = new byte[1];
        return read(value, 0, 1) == -1 ? -1 : value[0] & 0xff;
    }

    @Override public synchronized void close() {
        closed = true;
        chunks.clear();
        pendingBytes = 0;
        notifyAll();
    }
}
