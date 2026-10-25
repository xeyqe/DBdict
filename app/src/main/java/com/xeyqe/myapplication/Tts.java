package com.xeyqe.myapplication;

import android.app.Activity;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tts {
    private final Activity activity;
    private TextToSpeech tts;
    private boolean ready;

    public Tts(Activity activity) {
        this.activity = activity;
    }

    public void initializeTTS(String ttsEngine, final Callable<Void> methodParam) {
        destroyTTS();
        tts = new TextToSpeech(activity, status -> {
            ready = status == TextToSpeech.SUCCESS;
            if (ready) {
                tts.setSpeechRate(1);
                Button button = activity.findViewById(R.id.buTTS);
                button.setEnabled(true);
                try {
                    methodParam.call();
                } catch (Exception e) {
                    android.util.Log.e("Tts", "Could not initialize speech choices", e);
                }
            }
        }, ttsEngine);
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
        name = name.replaceAll("(?i)\\.wav$", "").replaceAll("[^\\p{L}\\p{N}_-]", "_");
        return name.substring(0, Math.min(name.length(), 120));
    }

    public void createMediaFile(String word, MediaCallback callback) {
        if (!ready || tts.getVoice() == null) {
            callback.onComplete(null);
            return;
        }
        final File file;
        try {
            File directory = new File(activity.getCacheDir(), "speech");
            if (!directory.isDirectory() && !directory.mkdirs()) {
                throw new IOException("Could not create speech cache");
            }
            file = File.createTempFile("speech-", ".wav", directory);
        } catch (IOException e) {
            callback.onComplete(null);
            return;
        }
        String utteranceId = file.getName();
        AtomicBoolean completed = new AtomicBoolean();
        MediaCallback finish = result -> {
            if (!completed.compareAndSet(false, true)) return;
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    file.delete();
                    return;
                }
                if (result == null) file.delete();
                callback.onComplete(result);
            });
        };
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) { }
            @Override public void onDone(String id) {
                if (utteranceId.equals(id)) finish.onComplete(file.length() > 0 ? file : null);
            }
            @Override public void onError(String id) {
                if (utteranceId.equals(id)) finish.onComplete(null);
            }
            @Override public void onStop(String id, boolean interrupted) {
                if (utteranceId.equals(id)) finish.onComplete(null);
            }
        });
        if (tts.synthesizeToFile(withoutSound(word), null, file, utteranceId) == TextToSpeech.ERROR) {
            finish.onComplete(null);
        }
    }

    public void speak(String text) {
        if (!ready || tts.getVoice() == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview");
        EditText front = activity.findViewById(R.id.editTextFront);
        String name = (tts.getVoice().getName() + "_" + text).replaceAll("[^\\p{L}\\p{N}_-]", "_");
        front.setText(text + " [sound:" + name + ".wav]");
    }

    public List<TextToSpeech.EngineInfo> listOfEngines() {
        return tts == null ? Collections.emptyList() : tts.getEngines();
    }

    public Set<Voice> setOfVoices() {
        Set<Voice> voices = ready ? tts.getVoices() : null;
        return voices == null ? Collections.emptySet() : voices;
    }

    public void setVoice(Voice voice) {
        if (ready && voice != null) tts.setVoice(voice);
    }

    public void destroyTTS() {
        ready = false;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
