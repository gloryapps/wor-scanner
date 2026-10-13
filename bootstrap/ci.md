# CI & release

`main` is what is released; `dev` is where work lands. `main` moves only by a pull request from `dev`,
merged with a merge commit. A merge is a release when `CHANGELOG.md` has notes under
`## [Unreleased]`; one without, such as a README or a past version's notes, reaches `main` building
and publishing nothing.

The steps themselves are in `.github/workflows/`: `release.yml` checks and builds a release on its pull
request and publishes that build on the merge, and `windows.yml` is run by hand.

## Verification

Tests run locally, `./gradlew test`, before a push; nothing runs on a push to `dev`. The release's pull
request runs them again, so a red test stops a release before anything is published, and Actions
minutes are spent only there.

The tests that read a real Windows desktop, the game's window and memory, skip anywhere else. On any
branch, `gh workflow run windows.yml --ref <branch>` runs them on a Windows runner and keeps the
report and the built app as the run's artifacts.

A dev build of the Windows app, `-Pworscanner.dev=true`, is named WoR Scanner Dev: its window, its
folder in `%LOCALAPPDATA%` and its link to the lab are its own, beside the released app's. With
`-Pazhor.url=http://<the Mac's address>:<port>` it reaches the lab running on the Mac.

## Versions and notes

`CHANGELOG.md` follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), written for whoever
scans: a change lands with its line under `## [Unreleased]`, and a release's notes are that section.

`worscanner.released` in `gradle.properties` is the version last released, and every build is
versioned as it, a debug build with a `-dev` suffix. A release takes the next version from its notes:
a minor where they hold `### Added` or `### Removed`, a patch where they only change or fix. Once
published, the workflow commits that version and the notes' date to `dev`, so pull `dev` before
working on. No notes choose a major; the first one adds how.

## Releasing

Open a pull request from `dev` to `main`, `gh pr create --base main --head dev`, and merge it once
its check passes. The merge publishes a GitHub release holding `wor-scanner-windows.zip`, whose name
carries no version, so `releases/latest/download/wor-scanner-windows.zip` always downloads the newest.

If GitHub never starts the merge's run, `gh workflow run release.yml --ref main` publishes it by hand,
for as long as the pull request's build is kept: 90 days.

Since 0.4.0 a release ships the Windows app alone: the Android app is paused at 0.3.0, and an installed
one finds no newer `wor-scanner.apk` to offer. Bringing it back puts the keystore step,
`:app:assembleRelease` and the APK and mapping assets back into `release.yml`, as 0.3.0's has them.

## Repository rules

Rulesets, available once the repository is public:

| Ruleset | Target | Rules |
| --- | --- | --- |
| main | `main` | Require a pull request (no approvals; merge method Merge only), require the `release` check, block force pushes, restrict deletions |
| dev | `dev` | Block force pushes, restrict deletions |
| release tags | `v*` | Restrict updates, restrict deletions, block force pushes; creations stay open for the workflow |

Nobody bypasses them. Pull requests are for collaborators only.

## Signing

Only the Android app is signed. `app/build.gradle.kts` reads the release keystore from the environment
and builds unsigned when `KEYSTORE_FILE` is absent, which is the case locally. The workflow sets it
from repository secrets:

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

Android release builds report crashes to Firebase Crashlytics; debug builds do not (`appModule` builds
`CrashReports` collecting only where the build is not debug). The Firebase project is
`wor-scanner-9914f`, with two Android apps registered, package `com.gloryapps.worscanner` and
`com.gloryapps.worscanner.debug`, since the Google services plugin refuses a build whose application id
the file does not name. Its `app/google-services.json` is committed: the keys in it are client keys,
restricted by package. On `assembleRelease` the Crashlytics plugin uploads the R8 `mapping.txt`, so
crashes arrive already retraced.
