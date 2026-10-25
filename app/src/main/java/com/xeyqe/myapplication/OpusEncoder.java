package com.xeyqe.myapplication;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;

/** Encodes streaming TTS PCM as Ogg Opus using Android's native encoder. */
final class OpusEncoder {
    private static final int BIT_RATE = 32_000;
    private static final long TIMEOUT_US = 10_000;

    private OpusEncoder() { }

    static File encode(File directory, InputStream input, int sampleRate, int channels,
                       PcmReader.Encoding encoding) throws IOException {
        File output = File.createTempFile("speech-", ".ogg", directory);
        boolean success = false;
        MediaCodec codec = null;
        MediaMuxer muxer = null;
        boolean codecStarted = false;
        boolean muxerStarted = false;
        try (PcmReader pcm = new PcmReader(input, sampleRate, channels, encoding)) {
            MediaFormat format = MediaFormat.createAudioFormat(
                    MediaFormat.MIMETYPE_AUDIO_OPUS, PcmReader.OUTPUT_SAMPLE_RATE, 1);
            format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
            format.setInteger(MediaFormat.KEY_PCM_ENCODING, android.media.AudioFormat.ENCODING_PCM_16BIT);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE,
                    MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS);
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG);
            codec.start();
            codecStarted = true;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            short[] samples = new short[960]; // 20 ms of mono PCM at 48 kHz
            long submittedSamples = 0;
            boolean inputEnded = false;
            boolean outputEnded = false;
            int track = -1;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
            while (!outputEnded) {
                if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline) {
                    throw new IOException("Opus encoding interrupted or stalled");
                }
                if (!inputEnded) {
                    int index = codec.dequeueInputBuffer(TIMEOUT_US);
                    if (index >= 0) {
                        ByteBuffer buffer = codec.getInputBuffer(index);
                        if (buffer == null || buffer.capacity() < samples.length * 2) {
                            throw new IOException("Opus encoder input buffer is too small");
                        }
                        buffer.clear();
                        buffer.order(ByteOrder.LITTLE_ENDIAN);
                        int count = pcm.read(samples);
                        for (int i = 0; i < count; i++) buffer.putShort(samples[i]);
                        long presentationTimeUs = submittedSamples * 1_000_000 / PcmReader.OUTPUT_SAMPLE_RATE;
                        inputEnded = count == 0;
                        codec.queueInputBuffer(index, 0, count * 2, presentationTimeUs,
                                inputEnded ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                        submittedSamples += count;
                        deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                    }
                }
                int index = codec.dequeueOutputBuffer(info, TIMEOUT_US);
                if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) throw new IOException("Opus format changed after muxing started");
                    track = muxer.addTrack(codec.getOutputFormat());
                    muxer.start();
                    muxerStarted = true;
                } else if (index >= 0) {
                    try {
                        if (info.size > 0 && (info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            ByteBuffer buffer = codec.getOutputBuffer(index);
                            if (!muxerStarted || buffer == null) throw new IOException("Missing Opus output format");
                            buffer.position(info.offset);
                            buffer.limit(info.offset + info.size);
                            muxer.writeSampleData(track, buffer, info);
                        }
                        outputEnded = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    } finally {
                        codec.releaseOutputBuffer(index, false);
                    }
                    deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                }
            }
            if (!muxerStarted) throw new IOException("Opus encoder produced no audio");
            muxer.stop();
            muxerStarted = false;
            success = true;
            return output;
        } catch (RuntimeException e) {
            throw new IOException("Could not encode Ogg Opus audio", e);
        } finally {
            if (codec != null) {
                if (codecStarted) {
                    try { codec.stop(); } catch (RuntimeException ignored) { }
                }
                try { codec.release(); } catch (RuntimeException ignored) { }
            }
            if (muxer != null) {
                if (muxerStarted) {
                    try { muxer.stop(); } catch (RuntimeException ignored) { }
                }
                try { muxer.release(); } catch (RuntimeException ignored) { }
            }
            if (!success) output.delete();
        }
    }
}
