# Pauso built-in audio provenance

Verified 2026-11-08 against the individual Freesound pages. The app does not call a Freesound API, fetch recordings, show ads, or need an account/network connection. The developer curated and downloaded public HQ preview files exposed by each page; no login credentials or original-file download endpoints were used.

## Forest Rain
- Original title: Rain in the forest
- Creator: stevenparry
- Original page: https://freesound.org/people/stevenparry/sounds/760955/
- Source public HQ preview: https://cdn.freesound.org/previews/760/760955_7002468-hq.mp3
- Declared license on that page: Creative Commons Zero (CC0 1.0)
- License: https://creativecommons.org/publicdomain/zero/1.0/
- Original description: rain recorded in woods during summer with a TASCAM DR-40.
- Distributed derivative: app/src/main/res/raw/forest_rain.ogg

## Forest Birds
- Original title: Forest birds - ambient seamless loop
- Creator: Magnesus
- Original page: https://freesound.org/people/Magnesus/sounds/723913/
- Source public HQ preview: https://cdn.freesound.org/previews/723/723913_2008500-hq.mp3
- Declared license on that page: Creative Commons Zero (CC0 1.0)
- License: https://creativecommons.org/publicdomain/zero/1.0/
- Original description: birds recorded in a Polish forest; creator made a seamless loop and explicitly allows reuse under CC0.
- Distributed derivative: app/src/main/res/raw/forest_birds.ogg

The two pages explicitly state permission to copy, modify, distribute and perform the sounds, including commercially. Credit is retained voluntarily; it does not imply endorsement. CC0 does not guarantee an uploader owns every right; the named primary pages are the evidence available.

Both distributed files are edited: mono 44.1 kHz, trimmed and crossfaded, DC removed, gently peak-compressed, RMS aligned, a 10 ms seam guard applied, and Ogg/Vorbis encoded. Exact durations, checksums and decoded metrics are in audio-checks.json. Original descriptions and measured signal checks are not a substitute for a listener checking for distracting events or loop audibility.

## Noise
White/Pink/Brown use the local NoiseGenerator.java implementation, not downloaded samples. Pink uses multi-rate random rows with an independent white component; Brown uses leaky integration. A DC blocker and bounded levels are applied. Noise is streamed continuously rather than looping a short recording.

## Reproduce nature edits (optional developer workflow)
Install Python numpy and ffmpeg. Download the two public HQ previews listed above into audio-work/rain-source.mp3 and audio-work/forest-source.mp3 in the working directory, then run `python3 scripts/prepare_nature.py` from the project root. The resulting Oggs are already included; no network or audio-processing tools are needed for a normal APK build. This script is developer tooling, not shipped Android code.
