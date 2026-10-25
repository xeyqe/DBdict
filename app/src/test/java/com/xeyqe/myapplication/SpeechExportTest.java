package com.xeyqe.myapplication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpeechExportTest {
    @Test
    public void speechTextExcludesExistingAudioReferences() {
        assertEquals("hello", Tts.withoutSound("hello [sound:voice_hello.wav]"));
        assertEquals("hello world", Tts.withoutSound("hello [sound:first.wav] world [sound:second.wav]"));
        assertEquals("[example]", Tts.withoutSound("[example]"));
    }

    @Test
    public void mediaNameCannotEscapeTheMediaDirectory() {
        assertEquals("______voice", Tts.mediaName("hello [sound:../../voice.wav]"));
        assertEquals("český_hlas", Tts.mediaName("hello [sound:český hlas.wav]"));
        assertEquals("speech", Tts.mediaName("hello"));
        assertEquals("voice_hello", Tts.mediaName("hello [sound:voice_hello.ogg]"));
        assertEquals("voice_hello", Tts.mediaName("hello [sound:voice_hello.opus]"));
    }
}
