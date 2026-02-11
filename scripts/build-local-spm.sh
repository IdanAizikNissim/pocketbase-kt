#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

echo "Building PocketBase XCFramework for local SwiftPM usage..."
./gradlew :pocketbase:assemblePocketBaseXCFramework

XCFRAMEWORK_PATH="pocketbase/build/XCFrameworks/release/PocketBase.xcframework"
if [[ ! -d "${XCFRAMEWORK_PATH}" ]]; then
  echo "Expected XCFramework missing: ${XCFRAMEWORK_PATH}" >&2
  exit 1
fi

echo "Built local XCFramework:"
echo "  ${ROOT_DIR}/${XCFRAMEWORK_PATH}"
echo
echo "Package.swift will now resolve PocketBase as a local binary target."
