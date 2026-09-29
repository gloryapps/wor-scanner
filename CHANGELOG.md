# Changelog

What each release changes for whoever scans with it. Versions follow [Semantic Versioning](https://semver.org).

## 0.2.1

- Text is read by a model inside the app, so a scan no longer fails on a device where Google Play services had not downloaded it. The APK grows to about 20 MB.
- A failed reading's delete and export sit beside its text instead of collapsing into one column.

## 0.2.0

- The home screen offers a newer release beside the version, downloads it and hands it to Android to install.
- Heroes: epics are scanned with the legendaries, and the scan stops at the first hero below epic.
- Heroes: the roster is scanned as cards or as squares.
- Artifacts of every rarity are scanned, skipping those never enhanced.
- The grid is dragged faster from row to row.
- A scan whose app is killed keeps what it read, listed as stopped.

## 0.1.2

- A long scan on Android 9, as LDPlayer runs, no longer runs out of memory and gets killed.
- The app no longer crashes when Android stops the screen capture.

## 0.1.1

- The APK is published as `wor-scanner.apk`, so the README's link always downloads the latest release.

## 0.1.0

- Scans gear, legendary heroes and mythic artifacts off the game's screen and writes a JSON for Azhor’s Master Smithy.
- Exports a scan into the folder LDPlayer or BlueStacks shares with the PC, or through Android's share sheet.
