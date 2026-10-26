# DBdict
[English-czech](https://www.svobodneslovniky.cz/) offline dictionary for android. 
<br>Add dictionaries with the top-right **+** button using the system file picker. Supported files:

- UTF-8 text with one word and meaning per line, separated by a tab. LF, CRLF, and CR line endings and an initial UTF-8 BOM are accepted. Blank lines and `##` comments are ignored. Invalid rows reject the whole import. Meanings support HTML formatting and literal `\n` for line breaks; additional tabs remain part of the meaning.
- StarDict `.tar.gz` / `.tgz` archives containing matching `.ifo`, `.idx` (or `.idx.gz`), and `.dict` (or `.dict.dz`) files. Archives containing both translation directions import each as a separate dictionary filter, using the name you enter plus its book name.

StarDict versions 2.4.2 and 3.0.0, 32/64-bit offsets, and text/Pango/HTML/phonetic fields (`m`, `g`, `h`, `t`, `y`) are supported. Other definition types are rejected. Imports run in the background and commit only when the whole file is valid. Limits: 512 MiB per text file and of archive data, 2 GiB per expanded dictionary, 1 MiB per StarDict definition and 1,048,576 UTF-16 code units per text line, and one million entries. For other formats, export to tab-separated text first (for example with [pyglossary](https://github.com/ilius/pyglossary)).
<br>You can send vocabulary to ankidroid.
<br>If you select text and tab share you can select DBdict to search.

## Building

Use JDK 17 or 21 and install Android SDK Platform 37 and Build Tools 36.0.0.
Set `sdk.dir` in your untracked `local.properties`, or set `ANDROID_HOME`.
The checked-in wrapper uses Gradle 9.6.0 with Android Gradle Plugin 9.4.1.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest
```

The debug APK is written to `app/build/outputs/apk/debug/DBdict.apk`.
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

The flashcard screen separates card content and pronunciation settings, supports
light/dark themes, and provides an audio toggle and a separate voice preview.
Speech engines initialize directly with a 10-second timeout. Switching engines
invalidates old callbacks; a failed preferred engine falls back to the system
default. Missing engines/voices show a retry action and voice management rather
than launching setup automatically. Language preferences use locale tags, and
voice choices indicate whether a network connection is needed.

The dictionary search screen, result cards, and import/removal dialogs use the
same Material light/dark theme as the flashcard screen. The search filter uses
“All dictionaries” for the unfiltered search, while an empty query shows recent
lookups. Add dictionaries with the top-right **+** button. Select a dictionary in the
filter, then use **Rename selected dictionary** or **Remove selected dictionary**
in the toolbar menu. Long-pressing the filter also offers removal. Renaming
preserves entries and recent lookups; existing dictionary names cannot be reused.
