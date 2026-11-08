#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_HOME:?GitHub runner Android SDK is required}"
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
"$SDKMANAGER" 'platforms;android-35' 'build-tools;35.0.0' >/dev/null
mkdir -p tools
curl --fail --location --retry 3 --max-time 120 -o tools/ecj.jar https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar
printf '%s  %s\n' 05cc22a24e7982970f63a405fc6c820bc80b806f27f3c5a6236fc475f8f7152b tools/ecj.jar | sha256sum --check
