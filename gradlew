#!/usr/bin/env sh
set -e

APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
GRADLE_VERSION="8.7"
GRADLE_DIR="$APP_HOME/.gradle/local/gradle-$GRADLE_VERSION"
GRADLE_ZIP="$APP_HOME/.gradle/local/gradle-$GRADLE_VERSION-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

mkdir -p "$APP_HOME/.gradle/local"

if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
  echo "Téléchargement de Gradle $GRADLE_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -L --fail -o "$GRADLE_ZIP" "$GRADLE_URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$GRADLE_ZIP" "$GRADLE_URL"
  else
    echo "Erreur: curl ou wget est nécessaire pour télécharger Gradle." >&2
    exit 1
  fi
  unzip -q -o "$GRADLE_ZIP" -d "$APP_HOME/.gradle/local"
fi

exec "$GRADLE_DIR/bin/gradle" "$@"
