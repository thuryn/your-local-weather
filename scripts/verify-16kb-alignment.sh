#!/bin/bash

# This script verifies that all .so files in an APK or AAB are 16KB page-aligned.
# Usage: ./verify-16kb-alignment.sh <path-to-apk-or-aab>

FILE=$1

if [ -z "$FILE" ]; then
    echo "Usage: $0 <path-to-apk-or-aab>"
    exit 1
fi

if [[ "$FILE" == *.apk ]]; then
    echo "Checking APK: $FILE"
    # Extract .so files and check alignment
    TMP_DIR=$(mktemp -d)
    unzip -q "$FILE" "lib/**/*.so" -d "$TMP_DIR" 2>/dev/null

    FIND_RESULT=$(find "$TMP_DIR" -name "*.so")
    if [ -z "$FIND_RESULT" ]; then
        echo "No .so files found in APK."
    else
        for so in $FIND_RESULT; do
            echo "Checking $so"
            # Check alignment using readelf
            # We look for the LOAD segments and check their alignment (Algn column)
            readelf -l "$so" | grep LOAD | awk '{print $NF}' | while read -r align; do
                if [[ "$align" == "0x4000" || "$align" == "0x10000" || "$align" -ge 16384 ]]; then
                    echo "  [OK] Alignment: $align"
                else
                    echo "  [FAIL] Alignment: $align (Expected >= 0x4000)"
                fi
            done
        done
    fi
    rm -rf "$TMP_DIR"

elif [[ "$FILE" == *.aab ]]; then
    echo "Checking AAB: $FILE"
    echo "Note: For AABs, the alignment is usually handled by Google Play during APK generation."
    echo "However, the native libs inside the AAB should still be aligned if you're targeting 16KB."

    TMP_DIR=$(mktemp -d)
    unzip -q "$FILE" "base/lib/**/*.so" -d "$TMP_DIR" 2>/dev/null

    FIND_RESULT=$(find "$TMP_DIR" -name "*.so")
    if [ -z "$FIND_RESULT" ]; then
        echo "No .so files found in AAB."
    else
        for so in $FIND_RESULT; do
            echo "Checking $so"
            readelf -l "$so" | grep LOAD | awk '{print $NF}' | while read -r align; do
                if [[ "$align" == "0x4000" || "$align" == "0x10000" || "$align" -ge 16384 ]]; then
                    echo "  [OK] Alignment: $align"
                else
                    echo "  [FAIL] Alignment: $align (Expected >= 0x4000)"
                fi
            done
        done
    fi
    rm -rf "$TMP_DIR"
else
    echo "Unsupported file type. Please provide an .apk or .aab file."
    exit 1
fi
