# CI & release

`main` is what is released; `dev` is where work lands. `main` moves only by a pull request from `dev`,
merged with a merge commit, and each merge is a release.

## Verification

Tests run locally, `./gradlew test`, before a push; nothing runs on a push to `dev`. A pull request to
`main` runs them again with the signed build it would publish, so a release never ships over a red
test or a key that does not sign, and Actions minutes are spent only there.

## Release

`dev` is always on the version it is building: `versionName` in `app/build.gradle.kts`, which debug
builds show with a `-dev` suffix. `versionCode` derives from it (`0.1.0` → `100`).

A release is a pull request from `dev` to `main` (`gh pr create --base main --head dev`), and
`.github/workflows/release.yml` runs on both ends of it: as the pull request's check, and on the
merge. One run per ref at a time; the runner sets up Java 17 for the build and 25 for the Gradle
daemon, and hands the keystore's secret to the script through `env:`.

On the pull request:

1. Fails one from any branch but `dev`.
2. Reads the version and fails if the tag `v<version>` already exists.
3. Runs the tests and builds the release APK, R8-optimized and signed.

On the merge, which is a merge commit so that `dev`'s commits reach `main` as they are:

1. Reads the version, runs the tests and builds the signed APK again, from the merge commit.
2. Creates the tag on the merge commit and a GitHub release holding `wor-scanner.apk` and
   `mapping-<version>.txt`, with notes generated since the previous tag. The APK's name carries no
   version, so `releases/latest/download/wor-scanner.apk` always downloads the newest one.
3. Commits `Start <next minor>` to `dev`: pull `dev` before working on. A patch or a major is set in
   `versionName` on `dev` by hand, before its pull request.

## Repository rules

Rulesets, available once the repository is public:

| Ruleset | Target | Rules |
| --- | --- | --- |
| main | `main` | Require a pull request (no approvals; merge method Merge only), require the `release` check, block force pushes, restrict deletions |
| dev | `dev` | Block force pushes, restrict deletions |
| release tags | `v*` | Restrict updates, restrict deletions, block force pushes; creations stay open for the workflow |

Nobody bypasses them. Pull requests are for collaborators only.

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

## Crashes

Release builds report crashes to Firebase Crashlytics; debug builds do not (`appModule` builds `CrashReports` collecting only
where the build is not debug). The Firebase project is `wor-scanner-9914f`, with two Android apps registered, package
`com.gloryapps.worscanner` and `com.gloryapps.worscanner.debug`, since the Google services plugin
refuses a build whose application id the file does not name. Its `app/google-services.json` is
committed: the keys in it are client keys, restricted by package. On `assembleRelease` the
Crashlytics plugin uploads the R8 `mapping.txt`, so crashes arrive already retraced.
