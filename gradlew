#!/bin/sh
set -e

GRADLE_VERSION="8.7"
GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_HOME_DIR="${HOME}/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}-bin"
GRADLE_BIN="${GRADLE_HOME_DIR}/gradle-${GRADLE_VERSION}/bin/gradle"

if [ ! -f "${GRADLE_BIN}" ]; then
    echo "Downloading Gradle ${GRADLE_VERSION}..."
    mkdir -p "${GRADLE_HOME_DIR}"
    TMPZIP="${GRADLE_HOME_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
    curl -fsSL "${GRADLE_DIST_URL}" -o "${TMPZIP}"
    unzip -q "${TMPZIP}" -d "${GRADLE_HOME_DIR}"
    rm -f "${TMPZIP}"
    chmod +x "${GRADLE_BIN}"
fi

exec "${GRADLE_BIN}" "$@"
