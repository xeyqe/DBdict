package com.xeyqe.myapplication;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Incrementally downmixes and resamples raw TTS chunks to mono PCM16 at 48 kHz. */
final class PcmReader implements Closeable {
    static final int OUTPUT_SAMPLE_RATE = 48_000;
    enum Encoding { PCM_8BIT, PCM_16BIT, PCM_FLOAT }

    private final DataInputStream input;
    private final int sampleRate;
    private final int channels;
    private final Encoding encoding;
    private final byte[] frame;
    private long sourceFrames;
    private long sourceIndex;
    private long outputIndex;
    private double left;
    private double right;
    private double decodedFrame;
    private boolean initialized;
    private boolean ended;

    PcmReader(InputStream input, int sampleRate, int channels, Encoding encoding) throws IOException {
        if (sampleRate < 8_000 || sampleRate > 192_000 || channels < 1 || channels > 2
                || encoding == null) throw new IOException("Unsupported TTS PCM format");
        this.input = new DataInputStream(new BufferedInputStream(input));
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.encoding = encoding;
        int bytes = encoding == Encoding.PCM_8BIT ? 1 : encoding == Encoding.PCM_16BIT ? 2 : 4;
        frame = new byte[channels * bytes];
    }

    int read(short[] samples) throws IOException {
        if (samples.length == 0) return 0;
        if (!initialized) {
            if (!readFrame()) throw new IOException("TTS produced no PCM audio");
            left = decodedFrame;
            ended = !readFrame();
            right = ended ? left : decodedFrame;
            initialized = true;
        }
        int count = 0;
        while (count < samples.length) {
            long position = outputIndex * sampleRate;
            long index = position / OUTPUT_SAMPLE_RATE;
            while (sourceIndex < index && !ended) {
                left = right;
                sourceIndex++;
                ended = !readFrame();
                right = ended ? left : decodedFrame;
            }
            if (ended && position >= sourceFrames * OUTPUT_SAMPLE_RATE) break;
            double fraction = (position % OUTPUT_SAMPLE_RATE) / (double) OUTPUT_SAMPLE_RATE;
            double value = left + (right - left) * fraction;
            samples[count++] = (short) Math.max(Short.MIN_VALUE,
                    Math.min(Short.MAX_VALUE, Math.round(value * 32768)));
            outputIndex++;
        }
        return count;
    }

    private boolean readFrame() throws IOException {
        int first = input.read();
        if (first == -1) return false;
        frame[0] = (byte) first;
        input.readFully(frame, 1, frame.length - 1);
        double sum = 0;
        int bytes = frame.length / channels;
        for (int channel = 0; channel < channels; channel++) {
            int offset = channel * bytes;
            int value = 0;
            for (int i = 0; i < bytes; i++) value |= (frame[offset + i] & 0xff) << (8 * i);
            double sample;
            if (encoding == Encoding.PCM_FLOAT) {
                sample = Float.intBitsToFloat(value);
                if (!Double.isFinite(sample)) throw new IOException("Invalid floating-point PCM");
            } else if (encoding == Encoding.PCM_8BIT) {
                sample = (value - 128) / 128.0;
            } else {
                sample = (short) value / 32768.0;
            }
            sum += Math.max(-1, Math.min(1, sample));
        }
        decodedFrame = sum / channels;
        sourceFrames++;
        return true;
    }

    @Override public void close() throws IOException {
        input.close();
    }
}
