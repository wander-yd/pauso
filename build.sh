#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
: "${SDK:?Set ANDROID_SDK_ROOT to your Android SDK directory}"
ECJ=${ECJ_JAR:-tools/ecj.jar}
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-35/android.jar"
mkdir -p build/classes build/dex build/gen
rm -rf build/classes/* build/dex/*
"$BT/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$BT/aapt2" link -0 ogg -o build/unsigned.apk -I "$JAR" --manifest app/src/main/AndroidManifest.xml --java build/gen build/resources.zip
java -jar "$ECJ" -8 -proc:none -nowarn -cp "$JAR" -d build/classes $(find app/src/main/java build/gen -name '*.java')
jar_path=build/classes.jar
python3 - <<'PY'
import zipfile,pathlib
with zipfile.ZipFile('build/classes.jar','w') as z:
 for p in pathlib.Path('build/classes').rglob('*.class'): z.write(p,p.relative_to('build/classes'))
PY
"$BT/d8" --lib "$JAR" --min-api 26 --output build/dex "$jar_path"
python3 - <<'PY'
import zipfile,pathlib
with zipfile.ZipFile('build/unsigned.apk','a') as z:
 for p in pathlib.Path('build/dex').glob('*.dex'):z.write(p,p.name)
PY
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ "${PAUSO_UNSIGNED:-0}" = "1" ]; then
 printf 'Built unsigned aligned APK: %s/build/aligned.apk\n' "$PWD"
 exit 0
fi
if [ -f signing.local.env ]; then
 set -a
 . ./signing.local.env
 set +a
fi
: "${PAUSO_KEYSTORE_PATH:?Set PAUSO_KEYSTORE_PATH (no key is generated automatically)}"
: "${PAUSO_STORE_PASSWORD:?Set PAUSO_STORE_PASSWORD}"
: "${PAUSO_KEY_PASSWORD:?Set PAUSO_KEY_PASSWORD}"
"$BT/apksigner" sign --ks "$PAUSO_KEYSTORE_PATH" --ks-key-alias "${PAUSO_KEY_ALIAS:-hourpause}" --ks-pass env:PAUSO_STORE_PASSWORD --key-pass env:PAUSO_KEY_PASSWORD --out build/Pauso.apk build/aligned.apk
"$BT/apksigner" verify build/Pauso.apk
printf 'Built %s/build/Pauso.apk\n' "$PWD"
