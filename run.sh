#!/usr/bin/env bash

set -euo pipefail

# ------------------------- Paths -------------------------------
ROOT_DIRECTORY="$(pwd)"
TARGET_JAR="$(pwd)/target/canine-backup-0.1.jar"

# ------------------------- Dependencies --------------------------
require() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing dependency: $1" >&2
    exit 1
  }
}
require git
require java
require mvn

# Java ≥ 21
JAVA_MAJOR="$(java -version 2>&1 | awk -F'[\".]' '/version/ {print $2}')"
if (( JAVA_MAJOR < 21 )); then
  echo "Java 21 or newer is required (found ${JAVA_MAJOR})." >&2
  exit 1
fi

# ------------------------- Build Jar -----------------------------
git pull --ff-only
echo "Building jar..."
mvn -q clean package
echo "Moving jar to $ROOT_DIRECTORY"
cp "$TARGET_JAR" "$ROOT_DIRECTORY"

# ------------------------- Run Jar -----------------------------
echo "Running $TARGET_JAR"
java -jar canine-backup-0.1.jar