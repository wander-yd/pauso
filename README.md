# Pauso — Quiet Rhythm · Android 0.12

Offline music with scheduled quiet intervals, selectable rhythms, and notification controls. Android 8 or later.

## Install and use
Install Pauso.apk over the existing Hour Pause app; do not uninstall first. The package and signing identity are the same, so file access and rhythm preferences can be retained. New users can tap Play immediately with built-in Brown Noise. Existing users retain their previous audio. Select a rhythm, and tap Sounds to choose another built-in or local file. Allow notifications. Audio loops and pauses for the selected quiet intervals, resuming from the same track position.

Four buttons show **work / rest minutes**: 14/1, 25/5, 50/10, 90/30. All presets align to the local clock; the default remains 14/1. The line below previews the next rest time or the current rest end.

Custom lets you enter 1–720 minutes for work and rest and choose:
- **Align to clock:** start cycles at today's 00:00 and continue them across days.
- **Start now:** begin a full work interval when Apply is tapped. Reapply this option if you want a new start later. Restarting the app retains the chosen origin.

Cancel leaves the current plan unchanged. Switching a rhythm updates the timer immediately without changing the file position. If manually paused, it stays paused.

In the notification, the LEFT colored icon is status: green music = playing; amber pause = manually paused (or audio interrupted); blue cup = scheduled rest. The RIGHT buttons are actions: pause/play and square Stop. Manual pause keeps the clock counting; only Resume restarts music, and the selected quiet intervals still apply. Stop ends the session and removes the notification.

The status-bar number is remaining whole minutes rounded up; values above 99 appear as 99+. Actual notification layout depends on the phone system.

## Update checking (0.12 — active)
Pauso checks for a new release at most once per 24 hours when opened. Use the Check for updates button at the bottom for a manual check. New versions show a plain update summary; Download update opens the browser and Android asks you to confirm installation. Playback and timers do not depend on successful checks. There is no silent installer permission or in-app download manager.

The live release repository is https://github.com/wander-yd/pauso-releases/releases. The private source repository is wander-yd/pauso. GitHub Actions tests and builds each push; main builds are signed with the existing identity and automatically published only after validation succeeds. Published release versions are retained. Friends install the current APK once, then use the app's update prompts for subsequent releases.

For signing, CI setup, credential renewal and recovery, read AUTOMATIC_UPDATES.md. The release credential expires 2027-02-07 and must be renewed in repository Secrets. Physical Android upgrade and notification/dialog behavior still require the bounded phone test.

## Offline Sounds (0.11)
Tap **Sounds** for Noise (White/Pink/Brown), Nature (Forest Rain/Forest Birds), and My Audio. Everything works offline. New installs start with Brown Noise selected at 35% player volume; opening the app does not auto-play. Upgrades retain the last file and volume. Audio never needs a network connection and has no ads. The Internet permission is used only to check for updates.

Tap the small play icon for a five-second preview, then **Use** to select. Pause or stop the main session before previewing. Closing the panel or leaving the app stops previews. Selection while stopped does not start audio; switching while active keeps the rhythm origin and manual pause state.

Built-in noise is streamed from a local generator, not looped from a file. Nature recordings are edited CC0 loops by stevenparry and Magnesus, credited in the Sounds panel and licenses/SOUNDS.md. Short fades reduce switching/enable clicks; actual sound and loop perception need the device test below.

### Bounded Sounds phone test
1. Upgrade: confirm your old local audio/position, volume and rhythm remain. A fresh install should have Brown Noise selected and play without asking for a file.
2. Pause/stop, open Sounds, and preview each sound for five seconds. Check that Close or leaving the app stops audition. Press Use, then Play. Previewing must not reset the rhythm or automatically resume a manually paused session.
3. While Playing, select another built-in; confirm the countdown does not restart. Manually Pause, switch again, and confirm it remains silent until Resume.
4. Apply a 1/1 Start now rhythm, lock the screen, and check one work/rest cycle with Brown Noise, then a nature recording. Listen through at least one complete recording loop for audible seams.
5. Return to My Audio and confirm the last file remains available. Check notification pause/resume/stop, volume and headphone disconnection.

The APK is compiled and signed, with generated-signal and migration tests. Real AudioTrack/MediaPlayer output, preview audio focus, subjective loudness, loop audibility and screen-off behavior remain unverified until this test.

## Pauso UI update (0.10)
The approved forest/sage/champagne/ivory palette is implemented in native Android views with a modest brand header, thin countdown ring and directly visible Play/Pause and Stop controls. The supplied Android icon pack is integrated. Existing timing and notification semantics are preserved; only displayed app branding changes there.

Playing, Paused and Break remain differentiated by colored shapes and text. The right notification controls stay white. Rhythm cards show explicit Play/Break durations; custom inputs use inline validation and the existing live time preview.

## Bounded phone UI test
1. Install Pauso.apk over Hour Pause. Check the launcher name/icon and confirm your selected audio/rhythm remain.
2. Check layout on your screen; the countdown and main Play/Pause/Stop controls should appear before Rhythms. Scrolling remains available on smaller screens or with large system fonts.
3. Tap Play, Pause, Resume and Stop on the main screen and compare the notification state. If resumed inside a Break, it must remain silent until the Break ends.
4. Open Custom Rhythm. Try a blank/0/721 field and check inline errors, live preview, Cancel and Apply. Check field/radio/button readability in the dialog.
5. Change a rhythm while manually paused; confirm it stays paused. Lock the screen and check the notification countdown/colors.

The app has been built and signed; no Android emulator or physical device is available here. Actual typography, layout, launcher masks, main-screen taps and audio/UI integration need the phone test above.

## Previous rhythm regression test
1. Install over 0.8. Confirm the selected default is 14/1 and the old file is retained.
2. Tap each preset; compare the next rest preview with the clock. 25/5 rests at :25/:55, 50/10 at :50, and 90/30 at 01:30, 03:30, …, 23:30.
3. Open Custom, try invalid values (blank/0/721), then Cancel; selection should remain unchanged.
4. Apply a short 1/1 custom Start now to check one complete cycle with the screen locked. Check the notification timer, cup/music states, and actual audio pause/resume.
5. Manually Pause, switch to another preset, and confirm no music starts. Resume should obey the new clock phase.
6. Close/reopen the app and check the last rhythm and file remain selected. Stop when finished.

No emulator or physical Android phone is available in this build environment. APK compile, signature and logic/persistence-fixture tests pass; visual layout, actual audio integration, background behavior and physical preference persistence need the test above.

## Build and test (Linux)
Requires Java 17+, Python 3, Android SDK platforms;android-35 and build-tools;35.0.0, plus Eclipse ECJ 3.40.0.

```sh
export ANDROID_SDK_ROOT=/path/to/android-sdk
export ECJ_JAR=/path/to/ecj-3.40.0.jar
./test.sh
./build.sh
```

ECJ download: https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar
Output: build/Pauso.apk. No Android Studio or Gradle is required. No third-party runtime dependencies or application server. Internet is used only for update checks.

`test.sh` runs ScheduleTest, PlaybackGateTest, RhythmTest, PlanStoreTest, SoundStoreTest and NoiseTest. The store tests use lightweight Android-interface fixtures, not an Android emulator. NoiseTest writes signal fixtures under build/. Optional signal checks: `python3 scripts/check_audio.py` (requires numpy, scipy and ffmpeg). See SPEC.md for the frozen behavior and evidence.

Current source packages exclude signing keys and signing.local.env. Keep the existing owner-controlled signing key backup private. Do not publish older source archives: earlier versions included the prototype key. Production builds now require explicit signing configuration; missing keys fail the build rather than generating an incompatible replacement. See AUTOMATIC_UPDATES.md.

## Limits and recovery
A foreground service and bounded, renewed CPU wake lock keep active schedules working while the screen is off, even when manually paused. Stop releases it. Manufacturers can still restrict background operation; if needed, test the phone's Unrestricted battery setting for the app. No auto restart after reboot, force-stop or process death; reopen and Start. For My Audio, the selected file must remain accessible on the phone. Clock-aligned schedules follow local clock/timezone changes; Start now follows the phone's clock timestamp.

To roll back, stop the app and reinstall the previous version if needed (Android may require uninstalling the newer version first, which clears app settings). Keep the audio file separately. No cloud account or data migration is involved.

State: update checking and automatic publication are active; local tests and the first live CI/signing/release passed. In-place phone upgrade remains unverified. No calendar, weekday schedules, playlists, YouTube, floating overlay or new scheduling behavior in this release.
