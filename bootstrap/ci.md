# CI & release

`main` is what is released; `dev` is where work lands. A release is a merge of `dev` into `main`.

## Verification

`.github/workflows/ci.yml` runs `./gradlew test` on every push to `dev` and on every pull request.

## Release

`.github/workflows/release.yml` runs on every push to `main`:

1. Reads `versionName` from `app/build.gradle.kts`. `versionCode` derives from it (`0.1.0` → `100`),
   so a version is bumped in that one field, on `dev`, before the merge.
2. Fails if the tag `v<versionName>` already exists.
3. Runs the tests and builds the release APK, R8-optimized and signed.
4. Creates the tag and a GitHub release holding `wor-scanner-<versionName>.apk` and
   `mapping-<versionName>.txt`, with notes generated from the commits since the previous tag.

Crashes reported against a release are retraced with that release's `mapping-<versionName>.txt`.

## Signing

`app/build.gradle.kts` reads the release keystore from the environment and builds unsigned when
`KEYSTORE_FILE` is absent, which is the case locally. The workflow sets it from repository secrets:

| Secret | Content |
| --- | --- |
| `KEYSTORE_BASE64` | the `.jks` file, base64-encoded |
| `KEYSTORE_PASSWORD` | its store password |
| `KEY_ALIAS` | the key's alias |
| `KEY_PASSWORD` | the key's password |

The keystore lives outside the repository (`*.jks` is ignored). Android only updates an app over a
build signed with the same key: losing the keystore means every user uninstalls to update, so keep a
copy off this machine. To create it and load the secrets:

```bash
keytool -genkeypair -v -keystore ~/wor-scanner-release.jks -alias wor-scanner -keyalg RSA -keysize 2048 -validity 10000
gh secret set KEYSTORE_BASE64 < <(base64 -i ~/wor-scanner-release.jks)
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS --body wor-scanner
gh secret set KEY_PASSWORD
```
