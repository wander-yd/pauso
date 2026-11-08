# Pauso automatic updates — prepared, not activated

## Current state
Local implementation and tests are prepared. Browser and connector both confirmed owner wander-yd. Created private wander-yd/pauso and public wander-yd/pauso-releases. The public repository contains the reviewed README only. PAUSO_PUBLISH_ENABLED is false. Source upload and first remote verification are in progress. No signing secret or publication credential has been uploaded; no APK has been published.

Only next action: finish source upload and remote verification, then confirm owner authorization for Actions signing credentials and create the restricted release token. Public APK release activation remains a final approval step. Two sequential real GitHub releases plus a phone upgrade are still unverified.

## Frozen outcome
- Private source: wander-yd/pauso.
- Public distribution: wander-yd/pauso-releases, containing README, release notes, Pauso.apk, update.json and SHA256SUMS only. No source ZIP, private source, signing key, credentials or private commit messages.
- Every branch/PR runs tests and compiles an unsigned, aligned APK without production signing credentials. No pull_request_target.
- Only a main push/manual dispatch can publish, and only after PAUSO_PUBLISH_ENABLED is explicitly true. Tests must pass first.
- A publishing job signs the verified APK, confirms the existing certificate, creates update metadata, uploads to a draft release, and makes it latest only when complete. Published versions are not silently replaced. Failed builds/uploads keep the prior latest release.
- CI versionCode = 1000 + workflow run number; versionName = 0.12.<run number>. Keep this workflow's numbering history. A publishing guard refuses versionCode regression if the workflow is recreated/reset.
- App checks at most once per 24h when opened and offers manual checking. At most one automatic prompt per available version. No app-downloaded installer or silent installation. Browser downloads, Android confirms.
- Manifest package, integer version, min Android version, SHA shape and exact approved GitHub asset URL are validated. Response size, redirect hosts and timeouts are bounded. Offline/errors do not affect sounds or rhythms.

## One-time GitHub setup
Use the authenticated owner account wander-yd. Create the source repo PRIVATE and release repo PUBLIC. Do not reuse or overwrite unrelated repositories. Public README content is prepared in distribution/README.md.

In private pauso, set repository Secrets:
- PAUSO_KEYSTORE_B64: base64 bytes of the EXISTING signing.p12.
- PAUSO_STORE_PASSWORD / PAUSO_KEY_PASSWORD: existing key passwords.
- PAUSO_RELEASE_TOKEN: a fine-grained PAT for ONLY pauso-releases, Contents read/write (metadata read is automatic). Choose a limited expiry and retain a reminder to renew it. No source-repository access is needed by this publishing token.

The built-in GITHUB_TOKEN is restricted to private-source contents read and does not provide cross-repository release access. Never put a PAT in the app or tracked code. Repository Secrets are used rather than paid private-repository environment features. Until other collaborators are added, only the owner should have source write access: someone who can change workflow files can request repository Secrets.

Set repository variable PAUSO_PUBLISH_ENABLED=false while staging/testing. After reviewing the exact public README, APK, release notes and publishing rules, set it true and run Pauso Android on main. This final switch begins automatic public releases. Do not enable paid Actions overage; use existing allowance and pause builds if it is exhausted.

An optional owner-run helper, scripts/bootstrap_github.py, handles the one-time setup using an already authenticated GitHub CLI. It stages with publication disabled unless --enable-publication is explicitly supplied. It reads signing material locally, passes it to gh secret set over stdin without printing it, and asks for the release PAT in a hidden terminal prompt. GitHub authentication for setup needs repository creation, source contents/workflow writing and Secrets/variables configuration permissions. The installed connector alone does not expose repository creation or Secrets APIs.

Do not paste passwords/PATs into ChatGPT. Browser sign-in uses the secure browser authentication form. Credential creation or security-sensitive access changes may require direct owner confirmation.

## Source safety
The existing key is intentionally not regenerated or rotated. It matches the installed apps. signing.local.env and all keystore formats are ignored by git. No key/password is embedded in build.sh. Production builds fail if signing configuration is missing; CI verification uses unsigned output.

Earlier source archives included the prototype signing key; do NOT publish them. New source packages must exclude signing.p12, signing.local.env, .env and any credentials. Keep an owner-controlled backup of the existing signing key separately. Existing protected Library source versions retain the earlier backup; do not distribute those archives.

## Review and acceptance
1. Local: ./test.sh; python3 -m unittest discover -s tests -p '*_test.py'; signed ./build.sh; scripts/check_audio.py.
2. Remote: verify non-main tests do not publish; main publish-disabled does not release; main enabled succeeds with the expected certificate and complete public assets.
3. Create a second reviewed main commit. Confirm versionCode increases and latest/download/Pauso.apk points to the newer release. Confirm the first release remains.
4. Install the earlier update-capable APK on a phone; use Check for updates, download and confirm Android's update install. File selection, volumes and rhythms must remain.
5. Check no-internet/manual 404, same/older version, incompatible minSdk and malformed manifest paths. Local policy tests cover these where possible; actual network/dialog/install behavior remains phone-only.

## Recovery
Set PAUSO_PUBLISH_ENABLED=false to pause publication. Revoke/replace only the cross-repo PAT if leaked; retain the Android signing identity. A bad public version is superseded by a FIX with a higher versionCode; old APKs cannot normally downgrade over a newer installed app. Failed drafts are not advertised as latest and can be repaired by rerunning the same run before publication.
