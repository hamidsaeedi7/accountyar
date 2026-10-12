#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/core-tests
if command -v javac >/dev/null 2>&1; then
  compiler=(javac)
else
  compiler=(java -m jdk.compiler/com.sun.tools.javac.Main)
fi
"${compiler[@]}" -encoding UTF-8 -d build/core-tests \
  app/src/main/java/ir/legendcoder/accountyar/Jalali.java \
  app/src/main/java/ir/legendcoder/accountyar/Crypto.java \
  tests/CoreTest.java tests/DateDump.java
java -cp build/core-tests CoreTest
