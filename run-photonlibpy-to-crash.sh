#!/bin/bash

set -ex

# Path to the program
PROGRAM="./photon-lib/build/install/photonlibTest/linuxx86-64/release/lib/photonlibTest"

# Ensure the program exists
if [[ ! -x "$PROGRAM" ]]; then
    echo "Error: Program not found or not executable at $PROGRAM"
    exit 1
fi

# https://jvns.ca/blog/2018/04/28/debugging-a-segfault-on-linux/
ulimit -c unlimited
sudo sysctl -w kernel.core_pattern=/tmp/core-%e.%p.%h.%t

# Loop to execute the program until it returns a nonzero exit code
while true; do
    "$PROGRAM"
    EXIT_CODE=$?

    if [[ $EXIT_CODE -ne 0 ]]; then
        echo "Program exited with nonzero exit code: $EXIT_CODE"
        break
    fi
done

