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
storage permissions. Generated speech is written to the app cache and imported
through AnkiDroid's media API after synthesis completes; no shared-storage path
is needed. Both screens apply system-bar and keyboard insets for edge-to-edge
layouts.

Anki integration uses the [AnkiDroid API](https://github.com/ankidroid/Anki-Android/wiki/AnkiDroid-API).
Audio export, speech engines, and keyboard/layout behavior still need device
testing on Android 10 and Android 17.
