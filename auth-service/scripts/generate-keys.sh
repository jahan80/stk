#!/bin/bash
set -e

KEYS_DIR="$(dirname "$0")/../src/main/resources/keys"
mkdir -p "$KEYS_DIR"

if [ -f "$KEYS_DIR/jwt-private.pem" ]; then
    echo "✅ Keys already exist at $KEYS_DIR"
    exit 0
fi

echo "Generating RSA keys..."
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEYS_DIR/jwt-private.pem"
openssl rsa -pubout -in "$KEYS_DIR/jwt-private.pem" -out "$KEYS_DIR/jwt-public.pem"

echo "✅ Keys generated at $KEYS_DIR"
