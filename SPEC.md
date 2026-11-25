# Pauso 0.14 — Session Sleep timer

Goal: a new user can install Pauso and press Play without providing audio, while existing users retain their chosen files, volume, rhythms and manual-pause behavior.
Direction: native single-screen Android layout with Forest #203A32, Sage #344B40, Champagne #E9D8AE, Ivory #F4EFE4 and Muted #A8AFA4. Existing 2×2 rhythms and custom settings remain.
Stopping point: build and automated checks pass; deliver APK for one phone testing round. No calendar, weekday schedules, multi-segment schedules, external playback, accounts, new timers or decorative animation.

## Automatic update contract (0.12)
The approved contract and current blockers are in AUTOMATIC_UPDATES.md. Target private source wander-yd/pauso and public distribution wander-yd/pauso-releases; no other repository may be changed. The owner approved the public content and publishing switch; main now publishes after tests/signing checks pass.

Local checks: update/version/SDK and trusted-URL policy, strict integer metadata and bounded notes; release manifest schema/checksum/version generation; fail-closed guards for draft uploads, branch conditions and production Secrets. Existing audio/timing suites retained. App Internet permission is added for update checks only; REQUEST_INSTALL_PACKAGES is intentionally absent.

Automatic publication was approved and activated on 2026-11-09. Two consecutive signed releases (0.12.2 and 0.12.3) passed live CI and anonymous metadata/APK checksum verification; the former remains available. Physical Android update preservation remains device-only. See AUTOMATIC_UPDATES.md for credential expiry and recovery.

## Offline sound contract (0.11)
- Choose Sound has Noise (White/Pink/Brown), Nature (Forest Rain/Forest Birds), and My Audio. No empty music categories, online catalog, streaming API, ads, account or download manager.
- New users default to Brown Noise with player volume 35%; audio never starts merely by opening the app. Existing local audio/position/explicit volume survive upgrade; if the older default volume was never explicitly saved, it remains 75%. The initial effective volume is pinned so choosing a source never unexpectedly raises it.
- Noise is synthesized continuously in 44.1 kHz mono PCM and streamed by AudioTrack. A dedicated worker sleeps while silent, uses DC blocking/bounded levels, and ramps gain around pause/resume/volume/close. No short noise recording is looped.
- Two CC0 recordings are bundled as Ogg. Source/creator/license and editing steps are documented in licenses/SOUNDS.md and available in the Choose Sound Credits dialog. Audio does not need networking; only the updater uses the Internet permission.
- Tap a preview icon for a five-second preview while the main session is stopped or manually paused. Previews request transient audio focus, stop on focus loss/dismiss/background, and do not change sound selection, rhythm origin or pause state. While the main session is active/unpaused, ask the user to pause first rather than silently altering their session or playing over it.
- Tap Use to select. If active, switch the source in the existing service without resetting the rhythm, clearing manual pause or re-requesting focus unnecessarily. If stopped, selection does not start playback.
- My Audio retains the last chosen file and permission; choosing a new local file is committed only after a successful picker result. Built-in selection does not discard the last local URI/title. Media/nature position keys are separate; noise has no meaningful recorded position.
- Every source obeys the same gate for ready/focus/manual pause/current rest. Fade tails can extend roughly 60 ms past a requested transition; final timing/audio behavior remains device-only verification.
- Asynchronous preparation/error callbacks are invalidated when switching/releasing a source. No stale source may resume or stop the newer one. Actual renderer lifecycle behavior needs phone testing.
- Only next action: phone-test the five built-ins, switching while paused, preview cleanup, and one short locked-screen rhythm. Stop after this implementation and one device test.

## Preserved visual and control contract (0.10)
- Name Pauso; supplied forest-green vinyl-clock Android icons, adaptive API 26+ and monochrome API 33+, legacy icons retained. Package identity and signing key unchanged for an in-place upgrade.
- Existing brand mark in a modest header; large champagne countdown inside a thin ring; current Playing/Paused/Break state with the established semantic state colors and shape icons.
- Main-page primary Play starts the existing session. While running it becomes Pause; while manually paused it becomes Resume. Separate square Stop ends the session. Main and notification use the same existing service actions.
- Play uses the selected built-in or My Audio source; no picker is required on a fresh install. Sounds remains available while a session is active; source switches preserve the clock and pause state. Stop is disabled while idle.
- Main screen order: brand, countdown/state, direct playback controls, four rhythm cards, Custom Rhythm, next-break preview, audio row and volume. Single screen with scrolling fallback for smaller screens or larger system fonts.
- Terms: Rhythms / Rhythm / Custom Rhythm and Play / Break durations. The four durations do not change or acquire invented names.
- Custom fields validate inline at Apply, use the numeric keyboard, preserve live preview and Cancel behavior. Explicit dark app/dialog colors avoid the earlier theme-contrast problem.
- Notification state colors, icon controls and status-bar digits remain unchanged, except displayed app branding Pauso. No long labels added to the notification.
- Flat surfaces and clear hierarchy; no vinyl texture painted over controls, heavy shadows or new animation.

## Existing timing behavior (preserved)
- Work/rest presets in minutes: 14/1 (existing upgrade default), 25/5, 50/10, 90/30.
- Presets use the phone's local clock with cycle boundaries at local midnight. All four divide a 24-hour day evenly. The selected preset is highlighted.
- 14/1 rests at :14–:15, :29–:30, :44–:45 and :59–:00. 25/5 rests at :25–:30 and :55–:00. 50/10 rests at :50–:00. 90/30 rests in alternate hours at 01:30–02:00, 03:30–04:00, and so on.
- Custom: positive whole work/rest minutes, each 1–720, with Align to clock or Start now. Blank, nonnumeric, overflow and out-of-range values cannot be applied. Cancel does not change the current selection.
- Aligned custom begins from the local midnight on the date Apply is pressed and continues in full cycles across days; unlike presets, arbitrary cycles need not divide a day. It does not truncate a cycle at midnight.
- Start now begins a full work interval when Apply is pressed; its saved timestamp survives closing and reopening the app. It follows the phone's system clock. Stop/restart does not reset that origin; reapply custom Start now to create a fresh origin.
- The custom dialog previews the resulting next rest time (or current rest end), updating as fields/mode change and once per second while open. Apply recalculates the actual origin.
- Rhythm changes immediately recalculate main countdown, ring, status-bar digits, notification countdown, rest/play state and next refresh. They never seek or reload the file, and never clear a manual pause.
- Choice, durations, mode and origin persist. Last custom input is retained independently when a preset is selected.
- During manual pause the countdown keeps cycling; music stays paused until Resume. At Resume the current rhythm applies, including waiting until a rest finishes.
- Status bar uses ceiling whole minutes up to 99; 100+ minutes displays 99+. Manual pause/rest/interruption uses the existing pause status-bar icon.
- Notification state: green music (playing), amber bars (manual pause or audio interruption), blue cup (scheduled rest), blue hourglass (loading). Manual pause takes priority over rest. The right action controls and countdown stay white on a fixed dark background.
- Local file selection, looping, volume, notification controls and service interruptions retain existing behavior. No boot/process-death auto restart. Stop removes notification and releases the session CPU wake lock. The lock stays held during a manually paused active timer to preserve screen-off boundary updates.

## Acceptance/evidence
| Stage | Goal / direction / stopping point | Status | Evidence |
|---|---|---|---|
| Selection | Four presets, custom controls, exact time preview; stop when compiled | Implemented | Final APK compile; phone UI unverified |
| Schedule | Generalize timing and preserve 14/1; stop when boundary/property checks pass | Tested | RhythmTest: 4 presets × 48h × 5 zones; boundaries, 90/30, midnight/custom, start-now saved anchors, invalid ranges, 10,000 custom cases, 3.6m legacy positions |
| Persistence | Remember selection without losing file state | Tested with fixtures | PlanStoreTest executes actual store code against in-memory Android interface fixtures; physical SharedPreferences integration still device-only |
| Pause/integration | New rhythm never undoes manual pause; countdown/notification use same model | Tested in logic / implemented in app | Pause tests and source/resource inspection; audio/UI interaction requires phone |
| Delivery | Signed upgrade APK plus source; stop at phone handoff | Ready after save | Same signing identity; version 11 / 0.11 |

Automated checks: `ECJ_JAR=/path/to/ecj.jar ./test.sh`.
Only next action for 0.13: run the local music phone test in README.md. Historical 0.11 evidence below is retained.

## 0.11 verification evidence
- Existing schedule/pause/rhythm/persistence regression suites pass.
- SoundStoreTest: ready-to-play new default; legacy file/position/volume migration; explicit-volume stability; persisted choices; preserved local URI; independent positions and invalid selection.
- NoiseTest: 12 sec each of actual generator output, finite/clamped samples, RMS/DC bounds, deterministic stream state and high-frequency ordering.
- scripts/check_audio.py: PSD slopes near 0/-1/-2 across 100–8000 Hz; nature decoding, RMS, peak, >20-sec length and small loop-edge step; Ogg entries uncompressed in APK for raw descriptors.
- APK compiles and retains signing identity; permissions unchanged. No emulator/physical-device audition was available. Numerical checks do not establish perceptual smoothness, bird-call distraction, hardware audio routing, preview focus return or screen-off reliability.

## Local music MVP (approved 2026-11-09)
Goal: browse indexed phone audio, save multiple playlists, and play sequentially, shuffled, repeat-one or repeat-all without changing Rhythms.
Allowed: on-demand MediaStore scan (audio permission API33+, legacy storage through API32), search, refresh, multi-select, persistent document multi-import, create/rename/delete playlists, remove/reorder entries with drag, queue state persistence, previous/next in app and expanded notification; compact notification preserves the large timer. No full-disk access, music uploads, artwork, lyrics, albums or continuous background scan. Playlist deletion/removal never deletes device audio. Denied scan permission retains file picker fallback. All accessible indexed audio is shown (not IS_MUSIC-only); private/hidden/unindexed/protected files may not appear.
Playback: selecting while active preserves manual pause and clock phase; while idle it only selects. Natural completion advances; repeat-one repeats current track; manual skip ignores repeat-one. Shuffle traverses a saved permutation with no repeated track per round; repeat-all makes another round without immediate boundary repetition when possible. Repeat-off stops at the end. Failed/inaccessible tracks are marked unavailable, skipped with a bounded one-round attempt limit; all-unavailable stops with a readable error. Current queue is a snapshot, so editing/deleting a saved playlist does not interrupt playback. Media preparation callbacks are generation-guarded. The existing selected local file migrates without losing position. File permissions referenced by saved queues/playlists are retained.
Acceptance: deterministic queue tests cover every mode, manual skip, previous, boundaries, no-repeat shuffle, invalid/missing tracks and serialization/restart/corruption. Compile full Android app; regress timing, sounds, updates and packaging; CI green before main publication. Physical permission dialogs, large libraries, drag accessibility, audio transitions, notifications and in-place upgrade require phone testing.
Time limit: four hours of implementation/checking; no subjective polishing beyond readable existing forest/sage style. Stop at tested release + bounded phone test.

0.13 limits: scan first 10,000 indexed tracks, up to 100 playlists / 10,000 entries each, document import up to 200 files per batch. Repeat defaults to playlist (retains looping for migrated singleton files). Tap a filtered song creates a queue of the shown search results; Use playlist selects its complete stored order. Player starts only on explicit Play when idle.

## Update connection recovery (2026-11-09)
Phone 0.12.3 reported generic update failure. Public asset route was observed to use two HTTPS redirects and curl timed out; the exact phone network exception is unavailable. Fix: try the public repository raw main/update.json first (12s total), then the existing release asset route (30s total, pinned HTTPS redirect hosts, bounded response). Parse and validate each source before accepting it; download URLs still use the exact approved repository asset policy. Successful automatic checks remain daily; failed checks can retry after one hour on opening. Manual failure offers Retry and Download page. Publisher writes direct metadata only after the signed complete release is public, can repair it on rerun, and refuses metadata regression. Old installed updater requires one manual in-place upgrade to obtain this transport fix. No certificates bypassed, no extra Android permissions.

## Sleep MVP (approved 2026-11-10)
Goal: finish the current playback session either after a chosen elapsed duration or at the start of its next scheduled rest, without automatic resume. Existing Rhythms, audio queues, focus and manual-pause semantics remain.
Scope: active-session Sleep only; presets 15/30/60/90 minutes plus custom 1–720; finish-this-play-period (already quiet => explicitly labelled Stop now). No Wake, fade-out, recurring timers, alarms, reboot/process-death auto start, or new permissions. Setting Sleep while idle does not start audio: explain that Play is required first. Only the last duration choice persists for convenience; armed session deadlines are cancelled on stop, error, queue end, service teardown or a fresh stopped launch after process death.
Duration uses elapsed realtime (includes screen-off and manual pause), so manual wall-clock/timezone changes do not extend it. Finish-period stores the current upcoming work-to-rest boundary; it is recalculated only on an explicit Rhythm or clock/timezone change, never repeatedly rolled forward at ticks. Selecting finish-period during a rest stops now. Sleep expiration wins before output start/resume, uses the normal position-saving stop path and removes the playback notification. Pending callbacks are invalidated. Five-second sound auditions are capped at an active Sleep deadline; opening Sleep settings or changing a Rhythm while Sleep is active ends any audition. Explicit Stop cancels Sleep. A small finished message remains until new Play or a new Sleep choice.
UI: dedicated moon Sleep row below Rhythm preview; visible kind, remaining time and local end time, Change/Cancel. Existing Playing/Paused/Break remains visible. Compact notification keeps large Rhythm countdown/status and displays moon + Sleep end time in the secondary line; expanded notification adds a distinct Sleep countdown and cancel control without replacing transport controls.
Acceptance: deterministic pure deadline and store tests cover both modes, pause/elapsed clock, exact expiry, already-quiet immediate end, plan/timezone changes, no deadline roll-forward, cancellation, invalid inputs, settings isolation. Compile complete app, existing regressions and CI release checks. Phone-only: short custom duration while playing/paused/screen locked, boundary ending, notification rendering/cancel, and retained playback position after Sleep. Stop after tested release and phone handoff; implementation/checking time limit two hours.

## Native quiet player + approved latest UIUX (2026-11-10, first phone-test milestone)
This section supersedes the older 0.10 single-screen visual contract, not the audio/timing contracts above.
Goal: use the C-direction turntable as a quiet native player, with the confirmed compact UI and real existing playback. Scope authorized: Player/Music/Settings icon navigation; dark ink-green surfaces; no repeated page/brand headings; compact `Play | Break` rhythm picker; volume popover/dialog; moon-only inactive Sleep entry; Sounds category recall; existing phone library/playlists with contextual multi-selection; tap blank to enter/exit immersion. No new folder scan, Wake, video, account, audio engine, permissions or updater transport work. Existing queue snapshots, persisted positions, permissions, focus, manually paused behavior and both Sleep endings must survive.

Rendering: native Canvas, separate chassis, rotor, fixed grazing light, anchored tonearm, timer and remaining-time ring. Rotor is counterclockwise. A sub-pixel once-per-revolution vertical/scale wobble belongs to the record only; numerals, ring, tonearm bearing and chassis remain fixed. Pause/Break/loading/interruption/stop park the arm and stop the rotor; wobble settles away. Near-dark arm parts and the bearing share scene lighting. Animation only on visible/resumed Player and respects system disabled animations plus a Reduce motion setting. No screen-wake/fullscreen permission. Remaining arc is for the current Play or Break segment, not elapsed full-cycle time. Manual pause still leaves schedule/Sleep counting.

Delivery modules: (1) native turntable and state model; (2) compact navigation/player + retained existing music controls; (3) regression/build/signature audit and phone handoff. One active module at a time. Development/checking cap three hours; stop and report partial evidence if exhausted. No subjective material-polish loop before the first real-device observation.

Acceptance: actual source tests for counterclockwise motion, stationary pause, bounded/single-cycle wobble, settle, anchored arm approach and offscreen clock reset; prior schedule/pause/queue/store/Sleep/update suites; complete APK build; same signing identity/package; preview version installable over official 0.14.7 without blocking the next monotonic official update. Phone-only: in-place upgrade; UI fit, actual rotation/wobble/touch discovery, native file permissions, lock screen/audio focus and 1-minute Sleep/1|1 break. Do not claim emulator or device evidence without running it.

Only next action: implement native visual model/view, then wire existing service state. Build stays on a test branch and is not automatically published to main.

Explicit playlist Play/Shuffle starts or resumes the session; selecting an ordinary sound/track retains the existing paused/stopped behavior. Explicit Shuffle chooses a random first track and leaves repeat-one for repeat-all. Folder browsing and active-session audition remain deferred.

Preview 2: short straight tonearm attached to a visible right-side plinth; subdued metal fades toward the dark bearing, small cartridge contacts the groove. Pause raises the arm slightly instead of swinging it. Disc motion, timer and playback behavior are unchanged. Physical-device visual review remains pending.

Preview 3 replaces procedural scene art with generated material layers: chassis, full transparent vinyl and long bent tonearm. Runtime registration places the arm on the socket and stylus on outer grooves; a fixed light overlay, live CCW remaining ring and upright timer stay independent of disc rotation/wobble. Pause/Break/interrupt lift the arm slightly with the mounting cap fixed. Three 1254px WebP resources are decoded once per view (~18MiB combined bitmap memory); no audio/service changes. Source artwork generated from the approved reference with the built-in image tool; WebP encoding preserves dimensions and alpha. Phone visual/performance verification remains pending.

Formal release 0.15.0 uses explicit versionCode 1020, exceeding public 1007 and all preview packages. CI reads committed manifest version rather than inventing 0.14 run-number versions. This release changes the native visual/music UI, not schedule/audio semantics. Automated checks passed; physical-device review remains pending. Future releases must intentionally bump versionCode and versionName together.

## Music browser update

- Music switches between Sounds, Songs, Folders and Playlists from a single view picker. Search filters the open view or collection, and sorting is local to that view. The existing bottom navigation stays icon-only.
- Add folder uses Android's folder picker and keeps read access to the selected folder. Audio is registered in place with progress updates, and rescanning the same folder skips previously saved document URIs. The existing Add files action still offers a playlist destination.
- Tapping a song in Music starts or resumes playback in place. The mini player shows the current local song across Music views. Its row is highlighted and the bars move only during playback, respecting reduced motion.
- Android acceptance: select Alternative Metal once (507 files), find Linkin Park, play a track, switch views, then return to the playlist and identify the current row.
