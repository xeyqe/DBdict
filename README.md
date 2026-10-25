# DBdict
[English-czech](https://www.svobodneslovniky.cz/) offline dictionary for android. 
<br>You can add more dictionaries, but it has to be plaintext divided by tab
<br>If you want to use diffferent format, you have to export it first \(e.g. by [pyglossary](https://github.com/ilius/pyglossary)\).
<br>You can send vocabulary to ankidroid.
<br>If you select text and tab share you can select DBdict to search.

## Building

Use JDK 17 or 21 and install Android SDK Platform 37 and Build Tools 36.0.0.
Set `sdk.dir` in your untracked `local.properties`, or set `ANDROID_HOME`.
The checked-in wrapper uses Gradle 9.6.0 with Android Gradle Plugin 9.4.1.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
Run `./gradlew connectedDebugAndroidTest` with a device or emulator attached
to execute the instrumented tests.

Android 10 (API 29) is the minimum supported version. Compile and target SDKs
are 37 (Android 17). Dictionary imports use the system document picker without
storage permissions. Generated speech is imported through AnkiDroid's media API; no shared-storage
path is needed. Both screens apply system-bar and keyboard insets for edge-to-edge
layouts.

Anki integration uses the [AnkiDroid API](https://github.com/ankidroid/Anki-Android/wiki/AnkiDroid-API).
Generated speech is encoded in the background as mono Ogg Opus (`.ogg`),
48 kHz at 32 kbps VBR. A silent TTS request supplies raw PCM through
`onBeginSynthesis` and `onAudioAvailable`; chunks pass through a bounded 1 MiB
memory buffer and are resampled/encoded as they arrive. No intermediate WAV
or complete in-memory recording is created. Only the final Ogg file is saved
for AnkiDroid import. Completion waits for the silent TTS request to finish,
which can take the speech duration. Encoding failures leave the note unsent
so it can be retried.

Audio export, speech engines, and keyboard/layout behavior still need device
testing on Android 10 and Android 17.
