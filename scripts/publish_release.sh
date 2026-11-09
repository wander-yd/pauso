#!/usr/bin/env bash
set -euo pipefail
# Called only by the protected main publishing job. Never echo credentials.
: "${GH_TOKEN:?Missing release-repository token}"
REPO=wander-yd/pauso-releases
TAG=$(python3 -c 'import json; print("v"+json.load(open("build/update.json"))["versionName"])')
# Direct CDN metadata is published only after the corresponding release is public.
# This is also rerun for an already-published tag, so a failed metadata write is recoverable.
sync_manifest() {
 LATEST_TAG=$(gh release view --repo "$REPO" --json tagName --jq .tagName)
 if [ "$LATEST_TAG" != "$TAG" ]; then
  echo 'A newer public release exists; leaving direct metadata unchanged.'
  return 0
 fi
 if ! gh api "repos/$REPO/contents/update.json" > build/current-update-file.json 2>/dev/null; then
  printf '{}' > build/current-update-file.json
 fi
 python3 - <<'PYMETA'
import json,base64,pathlib
new=json.loads(pathlib.Path('build/update.json').read_text())
current=json.loads(pathlib.Path('build/current-update-file.json').read_text())
if current.get('content'):
 old=json.loads(base64.b64decode(current['content']))
 if old['versionCode']>new['versionCode']:raise SystemExit('Refusing to replace newer direct update metadata')
payload={'message':'Update Pauso version metadata','content':base64.b64encode(pathlib.Path('build/update.json').read_bytes()).decode()}
if current.get('sha'):payload['sha']=current['sha']
pathlib.Path('build/direct-update-payload.json').write_text(json.dumps(payload))
PYMETA
 gh api "repos/$REPO/contents/update.json" --method PUT --input build/direct-update-payload.json > /dev/null
}
DRAFT=$(gh release view "$TAG" --repo "$REPO" --json isDraft --jq .isDraft 2>/dev/null || true)
if [ "$DRAFT" = false ]; then
 echo 'This version is already published; repairing direct metadata only.'
 sync_manifest
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

sync_manifest
