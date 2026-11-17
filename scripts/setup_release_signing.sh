#!/usr/bin/env bash
# One-time NOX01 release-signing setup. Run locally on your own trusted Termux/Linux.
# Never commit or share the generated *.p12 or store-pass.txt.
set -euo pipefail
umask 077

REPO="levomm/nox01-mobile"
DIR="${HOME}/.config/nox01/signing"
for cmd in gh openssl base64 tr sed; do
  command -v "$cmd" >/dev/null || { echo "Missing $cmd; in Termux install with: pkg install gh openssl coreutils"; exit 1; }
done
gh auth status -h github.com >/dev/null 2>&1 || { echo "First authenticate: gh auth login"; exit 1; }
if [[ -e "$DIR/nox01-release.p12" || -e "$DIR/store-pass.txt" ]]; then
  echo "Existing NOX01 release key detected. Refusing to replace it."
  echo "Do not regenerate an existing signing identity."
  exit 1
fi

mkdir -p "$DIR"
chmod 700 "$DIR"
openssl rand -hex 32 > "$DIR/store-pass.txt"
openssl req -x509 -newkey rsa:3072 -sha256 -nodes \
  -keyout "$DIR/private-key.pem" -out "$DIR/certificate.pem" -days 36500 \
  -subj "/CN=NOX01 Mobile/O=OSX01/C=EE"
openssl pkcs12 -export -name nox01 \
  -inkey "$DIR/private-key.pem" -in "$DIR/certificate.pem" \
  -out "$DIR/nox01-release.p12" -passout "file:$DIR/store-pass.txt"
openssl pkcs12 -in "$DIR/nox01-release.p12" -noout \
  -passin "file:$DIR/store-pass.txt" >/dev/null 2>&1
rm "$DIR/private-key.pem"
chmod 600 "$DIR/store-pass.txt" "$DIR/nox01-release.p12"

CERT_SHA256=$(openssl x509 -in "$DIR/certificate.pem" -noout -fingerprint -sha256 | sed 's/.*=//' | tr -d ':[:space:]' | tr '[:upper:]' '[:lower:]')
[[ "${#CERT_SHA256}" == 64 ]] || { echo "Invalid certificate fingerprint"; exit 1; }

base64 < "$DIR/nox01-release.p12" | tr -d '\n' | gh secret set BUDDY_KEYSTORE_BASE64 -R "$REPO"
gh secret set BUDDY_KEYSTORE_PASSWORD -R "$REPO" < "$DIR/store-pass.txt"
printf %s nox01 | gh secret set BUDDY_KEY_ALIAS -R "$REPO"
printf %s "$CERT_SHA256" | gh secret set NOX01_SIGNING_CERT_SHA256 -R "$REPO"

echo "NOX01 release signing configured for $REPO"
echo "Certificate SHA-256: $CERT_SHA256"
echo "CRITICAL: Back up BOTH files offline, somewhere only you can access:"
echo "  $DIR/nox01-release.p12"
echo "  $DIR/store-pass.txt"
echo "Do not delete the backup. Losing either means future APKs cannot update existing installs."
