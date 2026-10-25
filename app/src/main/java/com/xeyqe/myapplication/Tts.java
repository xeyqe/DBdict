package com.xeyqe.myapplication;

import android.app.Activity;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tts {
    private final Activity activity;
    private TextToSpeech tts;
    private boolean ready;
    private final EngineConnection<TextToSpeech> connection;
    private volatile ExportSession exportSession;
    private final ExecutorService audioEncoder = Executors.newSingleThreadExecutor();

    public Tts(Activity activity) {
        this.activity = activity;
        Handler handler = new Handler(Looper.getMainLooper());
        connection = new EngineConnection<>(new EngineConnection.Factory<TextToSpeech>() {
            @Override public TextToSpeech create(String name, EngineConnection.Result result) {
                return new TextToSpeech(activity.getApplicationContext(),
                        status -> result.complete(status == TextToSpeech.SUCCESS), name);
            }
            @Override public void shutdown(TextToSpeech engine) {
                engine.stop();
                engine.shutdown();
            }
        }, new EngineConnection.Scheduler() {
            @Override public void post(Runnable action) { handler.post(action); }
            @Override public void postDelayed(Runnable action, long delay) { handler.postDelayed(action, delay); }
            @Override public void remove(Runnable action) { handler.removeCallbacks(action); }
        });
    }

    public interface InitializationCallback {
        void onReady(Set<Voice> voices, Voice defaultVoice);
        void onFailure();
    }

    public void initializeTTS(String engine, InitializationCallback callback) {
        releaseEngine();
        connection.connect(engine, new EngineConnection.Listener<TextToSpeech>() {
            @Override public void onReady(TextToSpeech connected) {
                tts = connected;
                try {
                    if (tts.setSpeechRate(1) == TextToSpeech.ERROR) throw new IllegalStateException();
                    ready = true;
                    Set<Voice> voices = tts.getVoices();
                    callback.onReady(voices == null ? Collections.emptySet() : voices, tts.getDefaultVoice());
                } catch (RuntimeException error) {
                    releaseEngine();
                    callback.onFailure();
                }
            }
            @Override public void onFailure() {
                ready = false;
                tts = null;
                callback.onFailure();
            }
        });
    }

    public interface MediaCallback {
        void onComplete(File file);
    }

    public static String withoutSound(String word) {
        return word.replaceAll("\\s*\\[sound:[^\\]]*\\]", "").trim();
    }

    public static String mediaName(String word) {
        Matcher marker = Pattern.compile("\\[sound:([^\\]]+)\\]").matcher(word);
        String name = marker.find() ? marker.group(1) : "speech";
        name = name.replaceAll("(?i)\\.(wav|ogg|opus)$", "").replaceAll("[^\\p{L}\\p{N}_-]", "_");
        if (name.isEmpty()) name = "speech";
        return name.substring(0, Math.min(name.length(), 120));
    }

    public void createMediaFile(String word, MediaCallback callback) {
        if (!ready || tts.getVoice() == null) {
            callback.onComplete(null);
            return;
        }
        if (exportSession != null) exportSession.cancel();
        ExportSession session = new ExportSession(callback);
        exportSession = session;
        String utteranceId = UUID.randomUUID().toString();
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onBeginSynthesis(String id, int rate, int format, int channels) {
                if (utteranceId.equals(id)) session.begin(rate, format, channels);
            }
            @Override public void onAudioAvailable(String id, byte[] audio) {
                if (utteranceId.equals(id) && !session.completed.get()) {
                    if (!session.started.get() || !session.audio.offer(audio)) session.cancel();
                }
            }
            @Override public void onDone(String id) {
                if (!utteranceId.equals(id)) return;
                if (!session.started.get()) session.cancel();
                else session.audio.finish();
            }
            @Override public void onError(String id) {
                if (utteranceId.equals(id)) session.cancel();
            }
            @Override public void onStop(String id, boolean interrupted) {
                if (utteranceId.equals(id)) session.cancel();
            }
        });
        // onAudioAvailable supplies the original PCM even when playback is muted.
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0f);
        if (tts.speak(withoutSound(word), TextToSpeech.QUEUE_FLUSH, params, utteranceId)
                == TextToSpeech.ERROR) session.cancel();
    }

    private final class ExportSession {
        final PcmStream audio = new PcmStream();
        final AtomicBoolean started = new AtomicBoolean();
        final AtomicBoolean completed = new AtomicBoolean();
        private final MediaCallback callback;

        ExportSession(MediaCallback callback) {
            this.callback = callback;
        }

        void begin(int rate, int format, int channels) {
            if (completed.get()) return;
            if (!started.compareAndSet(false, true)) {
                cancel();
                return;
            }
            PcmReader.Encoding encoding;
            switch (format) {
                case AudioFormat.ENCODING_PCM_8BIT: encoding = PcmReader.Encoding.PCM_8BIT; break;
                case AudioFormat.ENCODING_PCM_16BIT: encoding = PcmReader.Encoding.PCM_16BIT; break;
                case AudioFormat.ENCODING_PCM_FLOAT: encoding = PcmReader.Encoding.PCM_FLOAT; break;
                default: cancel(); return;
            }
            try {
                audioEncoder.execute(() -> {
                    File encoded = null;
                    try {
                        File directory = new File(activity.getCacheDir(), "speech");
                        if (!directory.isDirectory() && !directory.mkdirs()) {
                            throw new IOException("Could not create speech cache");
                        }
                        encoded = OpusEncoder.encode(directory, audio, rate, channels, encoding);
                    } catch (IOException e) {
                        android.util.Log.e("Tts", "Could not stream speech as Opus", e);
                    } finally {
                        audio.close();
                    }
                    finish(encoded);
                });
            } catch (RejectedExecutionException e) {
                cancel();
            }
        }

        void cancel() {
            audio.fail(new IOException("Speech export cancelled"));
            finish(null);
        }

        void finish(File file) {
            if (!completed.compareAndSet(false, true)) {
                if (file != null) file.delete();
                return;
            }
            activity.runOnUiThread(() -> {
                if (exportSession == this) {
                    exportSession = null;
                    if (file == null && tts != null) tts.stop();
                }
                if (activity.isFinishing() || activity.isDestroyed()) {
                    if (file != null) file.delete();
                    return;
                }
                callback.onComplete(file);
            });
        }
    }

    public boolean speak(String text) {
        return ready && tts.getVoice() != null
                && tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview") == TextToSpeech.SUCCESS;
    }

    public boolean setVoice(Voice voice) {
        return ready && voice != null && tts.setVoice(voice) == TextToSpeech.SUCCESS;
    }

    public String audioMediaName(String text) {
        String voice = ready && tts.getVoice() != null ? tts.getVoice().getName() : "speech";
        return mediaName(text + " [sound:" + voice + "_" + text + ".ogg]");
    }

    public void destroyTTS() {
        releaseEngine();
        audioEncoder.shutdownNow();
    }

    private void releaseEngine() {
        ready = false;
        if (exportSession != null) exportSession.cancel();
        connection.close();
        tts = null;
    }
}
