"""Release-signing contract, evaluated in GitHub Actions before packaging."""
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
workflow=(ROOT/".github/workflows/build.yml").read_text()
gradle=(ROOT/"app/build.gradle.kts").read_text()

def ensure(name, condition):
    assert condition, name
    print("PASS:",name)

ensure("stable PKCS12 signing type", 'storeType = "pkcs12"' in gradle)
ensure("release secret mandatory", 'if [[ -z "$KEYSTORE_BASE64" ]]' in workflow)
ensure("verify signed release before upload", 'apksigner verify --verbose --print-certs' in workflow)
ensure("pin signing certificate", 'NOX01_SIGNING_CERT_SHA256' in workflow)
ensure("publish signed release, never debug", 'dist/nox01-mobile.apk' in workflow and 'dist/nox01-mobile-debug.apk' not in workflow)
ensure("no Buddy-branded release", 'dist/buddy.apk' not in workflow and 'Buddy $GITHUB_REF_NAME' not in workflow)
