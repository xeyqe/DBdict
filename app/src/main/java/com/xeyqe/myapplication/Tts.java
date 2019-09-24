package com.xeyqe.myapplication;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Tts {
    private Activity activity;
    private Context context;
    private TextToSpeech tts;
    private File file;


    public Tts(Activity activity) {
        this.activity = activity;
        this.context = activity;
    }


    public void initializeTTS(String ttsEngine, final Callable<Void> methodParam) {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {

                    tts.setSpeechRate(1);

                    Button buTTS = activity.findViewById(R.id.buTTS);
                    buTTS.setEnabled(true);

                    try {
                        methodParam.call();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, ttsEngine);
    }

    public void createMediaFile(String path, String word) {
        Pattern pattern = Pattern.compile("\\[sound:(.*?)\\]");
        Matcher matcher = pattern.matcher(word);
        if (matcher.find()) {
            try {
                String text = word.replaceAll(" \\[sound:.*\\.wav\\]", "");

                file = new File(path+matcher.group(1));
                if (PermissionChecker.doIHaveWritePermission(activity)) {

                    file.createNewFile();
                    if (file.exists()) {
                        tts.synthesizeToFile(text, null, new File(path + matcher.group(1)), null);
                    }
                } else {
                    EditText editTextFront = activity.findViewById(R.id.editTextFront);
                    editTextFront.setText(text);
                }
            } catch (IOException e){
                Log.e("NWFILE", e.toString());
            }
        }
    }

    public void speak(String text) {
        String filename = text.replaceAll(" ", "_") + ".wav";

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        EditText editTextFront = activity.findViewById(R.id.editTextFront);

        editTextFront.setText(text + " [sound:" + tts.getVoice().getName() + "_" + filename + "]");
    }

    public List<TextToSpeech.EngineInfo> listOfEngines() {
        return tts.getEngines();
    }

    public Set<Voice> setOfVoices() {
        return tts.getVoices();
    }

    public void setVoice(Voice voice) {
        tts.setVoice(voice);
    }

    public void destroyTTS() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
