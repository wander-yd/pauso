# Pauso — Quiet Rhythm · Android 0.15.0

Offline music with scheduled quiet intervals, selectable rhythms, and notification controls. Android 8 or later.

## Install and use
Install Pauso.apk over the existing Hour Pause app; do not uninstall first. The package and signing identity are the same, so file access and rhythm preferences can be retained. New users can tap Play immediately with built-in Brown Noise. Existing users retain their previous audio. Tap the compact rhythm control to choose a rhythm, and use the Music tab to select Sounds, Songs or Playlists. Allow notifications. Audio loops and pauses for the selected quiet intervals, resuming from the same track position.

The rhythm picker shows **work / rest minutes**: 14/1, 25/5, 50/10, 90/30. All presets align to the local clock; the default remains 14/1. The line below previews the next rest time or the current rest end.

Custom lets you enter 1–720 minutes for work and rest and choose:
- **Align to clock:** start cycles at today's 00:00 and continue them across days.
- **Start now:** begin a full work interval when Apply is tapped. Reapply this option if you want a new start later. Restarting the app retains the chosen origin.

Cancel leaves the current plan unchanged. Switching a rhythm updates the timer immediately without changing the file position. If manually paused, it stays paused.

In the notification, the LEFT colored icon is status: green music = playing; amber pause = manually paused (or audio interrupted); blue cup = scheduled rest. The RIGHT buttons are actions: pause/play and square Stop. Manual pause keeps the clock counting; only Resume restarts music, and the selected quiet intervals still apply. Stop ends the session and removes the notification.

The status-bar number is remaining whole minutes rounded up; values above 99 appear as 99+. Actual notification layout depends on the phone system.

## Quiet player (0.15.0)
Icon-only bottom navigation opens Player, Music and Settings. Tap empty Player space to hide or show controls; tap the record in immersive view to return to controls. Settings also offers Immersive view and Reduce motion. The volume icon opens the player-volume slider. The live timer stays upright while the vinyl rotates counterclockwise with a subtle wobble; the arm lifts when playback is silent.

Sounds has Noise and Nature categories that remember their last choice. Long-press songs to select multiple entries; the circle icon selects all visible results. Removing tracks from a playlist does not delete audio files. Explicit playlist Shuffle chooses a random first track.

## Sleep timer
Start playback, then tap the **moon icon** on the Player page. Choose 15/30/60/90 minutes, Custom (1–720), or finish this play period. If currently in a break, the period option explicitly says Stop now. Idle Sleep settings explain that Play is required; choosing Sleep never starts audio automatically.

- Sleep stops the entire session, saves the recorded track position and removes the notification. Nothing automatically resumes after it finishes. Explicit Stop, playback errors, an exhausted queue or service termination cancel the armed Sleep setting.
- Duration counts elapsed time even while manually paused or screen-off; clock/timezone/Rhythm changes do not extend it. Finish-period stops before the upcoming rest; explicit Rhythm/clock/timezone changes recalculate that boundary. Merely reaching a boundary never rolls the deadline into another cycle.
- The moon row shows the remaining time, local stop time and Cancel; tap it to change the timer. Playing / Paused / Break remains independent. Compact notification keeps its large Rhythm countdown, adding the moon/end time on the track line. Expand for a separate Sleep countdown and cancel action.
- A finished message remains until the next Play or Sleep choice. Only the last selected duration is remembered across sessions. There is no Wake mode, alarm, recurring timer or restart after reboot/process death. No new permissions are needed.

### Bounded Sleep phone test
1. Start a recording, set Custom Sleep to 1 minute and lock the screen. Confirm it fully stops near that deadline and does not restart after the next rest. Press Play and confirm the recording continues from the saved position.
2. Repeat while manually paused: Sleep must still expire and remove the notification. Try cancellation from both main screen and expanded notification; normal playback/rest timing must remain.
3. With a short 1/1 Start now Rhythm, choose This play period. Confirm it stops at the beginning of rest. Set it while already resting and verify Stop now ends the session immediately.
4. While Sleep is active, switch tracks/sounds, then change Rhythm: duration end stays fixed, period end follows the new Rhythm. Stop and start another session; the old deadline must never stop it.

Deadline/store tests, previous regressions and APK compilation are automated. Lock-screen timing, visual layout, exact audio stop position and physical in-place upgrades still need the phone test above.

## Phone music and playlists (0.13)
Open **Sounds → Phone music & playlists**. Phone music requests audio read permission only when used; Android 13+ uses Music and audio permission. Android 8–12 uses legacy read storage permission. There is no all-files permission. Only indexed shared audio is scanned; hidden, private and DRM offline files may not appear. Denial leaves Add files available.

- Search title/artist; Refresh phone re-reads the media directory. The first 10,000 indexed tracks are shown. Scanning runs off the UI thread with a virtualized list.
- Tap a track to select the shown songs as a queue (including the current search filter). Long-press to multi-select, then Add to playlist. Select shown selects the filtered results.
- Add files supports persistent multi-selection (up to 200 files per batch) and choosing an existing/new playlist. Existing single-file selection remains available. Files are never copied or deleted.
- Playlists supports create, rename, delete, remove and reorder. Hold the ≡ handle to drag (clear search first), or tap it for accessible Move up / Move down / Remove actions. Up to 100 playlists and 10,000 tracks per list; duplicate URIs are ignored. Tap Use playlist for the complete list.
- The current queue is a snapshot: editing/deleting a playlist does not interrupt it. Selecting while stopped never starts audio. Manual pause and the Rhythm phase survive source selection and next/previous.
- Main-page Shuffle and Repeat controls apply to My Audio. Repeat cycles playlist → one → off → playlist. Repeat-one suspends automatic shuffle traversal; explicit next/previous still move through the queue. Shuffle visits each entry once per round; repeat-playlist starts another round. Repeat-off ends the session at the last track. A single-file upgrade retains looping by default.
- Queue, order, selection, modes and saved stop/pause position are remembered. Missing/inaccessible tracks are marked and skipped with a one-round failure limit. Refresh after regranting audio permission or re-add a document to retry repaired files.
- Compact notification retains the large countdown and pause/stop. Expand it for previous/next. Media-session headset skip actions are supported for local queues.

### Bounded local music phone test
1. Install over the existing app; confirm file, volume and Rhythm remain. Open the library, deny audio permission and use Add files; then allow permission and Refresh phone. Search and multi-select a few songs.
2. Create two playlists; import multiple files, add duplicates, rename, drag/reorder and remove a track. Delete one playlist and confirm original files remain. Reopen Pauso and confirm the other list persists.
3. With 2–3 short tracks, test off (stops after last), playlist loop, repeat-one and shuffled repeat. Try next/previous and expand the notification. A round of shuffle must not repeat entries.
4. While manually paused, select a different playlist and skip a song: no audio should start until Resume. Apply a 1/1 Start now Rhythm, lock the phone and verify rest pauses at the boundary and resumes at the same position. Skip during rest and confirm it stays silent until work resumes.
5. Remove/move one file, try playback and check it is skipped/flagged; an all-unavailable queue must stop without spinning. Restore permission/Refresh or re-add the file, then try again.

Queue/state/legacy migration policy tests and full APK compilation are automated. Physical permission prompts, file-provider grants, drag layout, audio callbacks and lock-screen integration remain unverified here.

## Update checking (0.12 — active)
Pauso checks for a new release at most once per 24 hours after a successful check when opened; failed checks may retry after one hour. A small direct CDN manifest is tried before the release-asset fallback. Use the Check for updates button at the bottom for a manual check. New versions show a plain update summary; Download update opens the browser and Android asks you to confirm installation. Playback and timers do not depend on successful checks. There is no silent installer permission or in-app download manager.

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

State: update checking and automatic publication are active; local tests and the first live CI/signing/release passed. In-place phone upgrade remains unverified. Local music and playlist regression tests/compilation pass; the bounded phone test above remains. No calendar, weekday schedules, YouTube, floating overlay, artwork or lyrics in this release.
