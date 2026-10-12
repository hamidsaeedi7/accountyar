#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to your Android SDK directory}"
BT="$ANDROID_SDK_ROOT/build-tools/35.0.0"
PLATFORM="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
mkdir -p build/generated build/classes build/dex
"$BT/aapt2" compile --dir app/src/main/res -o build/resources.zip
python3 - <<'PY'
s=open('app/src/main/AndroidManifest.xml').read()
open('build/AndroidManifest.xml','w').write(s.replace('<manifest xmlns:android=', '<manifest package="ir.legendcoder.accountyar" xmlns:android='))
PY
"$BT/aapt2" link -o build/unsigned.apk -I "$PLATFORM" --manifest build/AndroidManifest.xml --version-code 1 --version-name 1.0 --java build/generated -A app/src/main/assets build/resources.zip
find app/src/main/java build/generated -name '*.java' > build/sources.txt
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -source 11 -target 11 -classpath "$PLATFORM" -d build/classes @build/sources.txt
find build/classes -name '*.class' > build/classes.txt
"$BT/d8" --min-api 30 --lib "$PLATFORM" --output build/dex @build/classes.txt
(cd build/dex && zip -q -j ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
if [[ -n "${ACCOUNTYAR_KEYSTORE:-}" ]]; then
 : "${ACCOUNTYAR_SIGNING_PASS:?Set signing passphrase in environment}"
 "$BT/apksigner" sign --ks "$ACCOUNTYAR_KEYSTORE" --ks-key-alias accountyar --ks-pass env:ACCOUNTYAR_SIGNING_PASS --key-pass env:ACCOUNTYAR_SIGNING_PASS --out build/AccountYar.apk build/aligned.apk
 "$BT/apksigner" verify --verbose build/AccountYar.apk
else
 echo 'Unsigned aligned APK: build/aligned.apk. Sign it before installation.'
fi
