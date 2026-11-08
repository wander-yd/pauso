#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
ECJ=${ECJ_JAR:-tools/ecj.jar}
mkdir -p build/testclasses
java -jar "$ECJ" -8 -proc:none -nowarn -d build/testclasses tests/stubs/android/content/*.java app/src/main/java/com/andrew/hourpause/Schedule.java app/src/main/java/com/andrew/hourpause/PlaybackGate.java app/src/main/java/com/andrew/hourpause/PlanStore.java app/src/main/java/com/andrew/hourpause/SoundStore.java app/src/main/java/com/andrew/hourpause/NoiseGenerator.java app/src/main/java/com/andrew/hourpause/UpdateInfo.java tests/*Test.java
for suite in ScheduleTest PlaybackGateTest RhythmTest PlanStoreTest SoundStoreTest NoiseTest UpdateInfoTest; do
 java -cp build/testclasses "com.andrew.hourpause.$suite"
done
