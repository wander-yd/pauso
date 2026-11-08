# Pauso 0.12 — Automatic update preparation

Goal: a new user can install Pauso and press Play without providing audio, while existing users retain their chosen files, volume, rhythms and manual-pause behavior.
Direction: native single-screen Android layout with Forest #203A32, Sage #344B40, Champagne #E9D8AE, Ivory #F4EFE4 and Muted #A8AFA4. Existing 2×2 rhythms and custom settings remain.
Stopping point: build and automated checks pass; deliver APK for one phone testing round. No calendar, weekday schedules, multi-segment schedules, external playback, accounts, new timers or decorative animation.

## Automatic update contract (0.12)
The approved contract and current blockers are in AUTOMATIC_UPDATES.md. Target private source wander-yd/pauso and public distribution wander-yd/pauso-releases; no other repository may be changed. Workflows and the update-capable signed bootstrap app are prepared locally. No public APK release activation occurs before the owner reviews the exact public content and confirms the publishing switch.

Local checks: update/version/SDK and trusted-URL policy, strict integer metadata and bounded notes; release manifest schema/checksum/version generation; fail-closed guards for draft uploads, branch conditions and production Secrets. Existing audio/timing suites retained. App Internet permission is added for update checks only; REQUEST_INSTALL_PACKAGES is intentionally absent.

Remote creation, Secrets, actual GitHub workflow execution, two consecutive real releases and phone update preservation are NOT yet verified. Only next action is GitHub authentication after local review artifacts are ready. No claim of a working online update channel until these checks pass.

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
Only next action: install Pauso 0.11 and perform the bounded Sounds phone test in README.md.

## 0.11 verification evidence
- Existing schedule/pause/rhythm/persistence regression suites pass.
- SoundStoreTest: ready-to-play new default; legacy file/position/volume migration; explicit-volume stability; persisted choices; preserved local URI; independent positions and invalid selection.
- NoiseTest: 12 sec each of actual generator output, finite/clamped samples, RMS/DC bounds, deterministic stream state and high-frequency ordering.
- scripts/check_audio.py: PSD slopes near 0/-1/-2 across 100–8000 Hz; nature decoding, RMS, peak, >20-sec length and small loop-edge step; Ogg entries uncompressed in APK for raw descriptors.
- APK compiles and retains signing identity; permissions unchanged. No emulator/physical-device audition was available. Numerical checks do not establish perceptual smoothness, bird-call distraction, hardware audio routing, preview focus return or screen-off reliability.
