# CI & release

`main` is what is released; `dev` is where work lands. A release moves `main` to the head of `dev`.

## Verification

`.github/workflows/ci.yml` runs `./gradlew test` on every push to `dev` and on every pull request.

## Release

`dev` is always on the version it is building: `versionName` in `app/build.gradle.kts`, which debug
builds show with a `-dev` suffix. `versionCode` derives from it (`0.1.0` → `100`).
`.github/workflows/release.yml` is run by hand (Actions → Release → Run workflow, or
`gh workflow run Release -f next=patch`), telling it which part of the version `dev` moves to
afterwards, `minor` by default:

1. Checks out `dev`, reads its version and fails if the tag `v<version>` already exists.
2. Runs the tests and builds the release APK, R8-optimized and signed.
3. Fast-forwards `main` to the `dev` commit, failing if `main` holds commits `dev` does not.
4. Creates the tag and a GitHub release holding `wor-scanner-<version>.apk` and
   `mapping-<version>.txt`, with notes generated from the commits since the previous tag.
5. Commits `Start <next version>` to `dev` with the bumped `versionName`: pull `dev` before working on.

Crashes reported against a release are retraced with that release's `mapping-<version>.txt`.

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
