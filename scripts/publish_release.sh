#!/usr/bin/env bash
set -euo pipefail
# Called only by the protected main publishing job. Never echo credentials.
: "${GH_TOKEN:?Missing release-repository token}"
REPO=wander-yd/pauso-releases
TAG=$(python3 -c 'import json; print("v"+json.load(open("build/update.json"))["versionName"])')
DRAFT=$(gh release view "$TAG" --repo "$REPO" --json isDraft --jq .isDraft 2>/dev/null || true)
if [ "$DRAFT" = false ]; then
 echo 'This version is already published; leaving it unchanged.'
 exit 0
fi
# Compare against current public version; refuse accidental version-number reset.
LATEST=$(mktemp)
if gh release download --repo "$REPO" --pattern update.json --output "$LATEST" --clobber 2>/dev/null; then
 python3 - "$LATEST" <<'PY'
import json,sys
old=json.load(open(sys.argv[1]));new=json.load(open('build/update.json'))
if new['versionCode']<=old['versionCode']:raise SystemExit('Refusing non-increasing public versionCode')
PY
else
 # A first release is valid, but an API/auth failure must not masquerade as no release.
 COUNT=$(gh api "repos/$REPO/releases" --jq '[.[] | select(.draft == false and .prerelease == false)] | length')
 [ "$COUNT" = 0 ] || { echo 'Cannot verify current release metadata; refusing publication.'; exit 1; }
fi
rm -f "$LATEST"
if [ "$DRAFT" != true ]; then
 gh release create "$TAG" --repo "$REPO" --draft --title "Pauso ${TAG#v}" --notes-file RELEASE_NOTES.md
fi
gh release upload "$TAG" build/Pauso.apk build/update.json build/SHA256SUMS --repo "$REPO" --clobber
# Only now does the complete release become discoverable as latest.
gh release edit "$TAG" --repo "$REPO" --draft=false --latest
