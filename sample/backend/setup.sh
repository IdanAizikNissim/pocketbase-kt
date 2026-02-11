#!/bin/bash
set -e

PB_VERSION="0.22.6"
OS="linux"
ARCH="amd64"

if [[ "$OSTYPE" == "darwin"* ]]; then
    OS="darwin"
fi

if [[ "$(uname -m)" == "arm64" ]]; then
    ARCH="arm64"
fi

PB_FILE="pocketbase_${PB_VERSION}_${OS}_${ARCH}.zip"
URL="https://github.com/pocketbase/pocketbase/releases/download/v${PB_VERSION}/${PB_FILE}"

cd "$(dirname "$0")"

if [ ! -f "$PB_FILE" ]; then
    echo "Downloading PocketBase $PB_VERSION for $OS $ARCH..."
    curl -L -O "$URL"
fi

if [ ! -f "pocketbase" ]; then
    echo "Unzipping..."
    unzip -o "$PB_FILE"
    chmod +x pocketbase
fi

echo "Starting PocketBase at http://127.0.0.1:8090"
./pocketbase serve --http=127.0.0.1:8090
