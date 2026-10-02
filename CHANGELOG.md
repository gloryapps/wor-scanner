# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.0] - 2026-10-02

### Added

- A first run that explains the scanner and its permissions.
- Send a scan straight to your account on Azhor’s Master Smithy from the home screen as it ends, or
  later from its export sheet; the first send links the scanner with the code the site's Import a
  scan shows.
- How to scan, in the home screen's header and on the first run, plays a video of a whole scan.

### Changed

- The home screen shows how to scan each kind; the kind is chosen on the capsule.
- The capsule says which kind it will scan, and its menu says in a line how to open it in the game.
- The scans kept on the device and the link to Azhor’s Master Smithy moved behind All scans, in
  the home screen's header.
- A permission that is off is asked for when Start is pressed, with why the scanner needs it.
- A newer version is offered in a banner on the home screen.
- The home screen always shows whether each permission is on.
- All scans, and a large scan's pieces, open faster once many scans are kept.

### Fixed

- An accessibility service Android shows as on but is not running is no longer taken for on: the
  home screen, the first run and the capsule's menu say to turn it off and on, or to restart the
  emulator when that is not enough, where Scan used to fail.

## [0.2.1] - 2026-09-29

### Changed

- The text model ships inside the app, which grows to about 20 MB.

### Fixed

- A scan no longer fails on a device where Google Play services had not downloaded the text model.
- A failed reading's delete and export sit beside its text instead of collapsing into one column.

## [0.2.0] - 2026-09-28

### Added

- The home screen offers a newer release beside the version, downloads it and hands it to Android to install.
- Heroes: epics are scanned with the legendaries, and the scan stops at the first hero below epic.
- Heroes: the roster is scanned as cards or as squares.
- Artifacts of every rarity are scanned, skipping those never enhanced.

### Changed

- The grid is dragged faster from row to row.

### Fixed

- A scan whose app is killed keeps what it read, listed as stopped.

## [0.1.2] - 2026-09-27

### Fixed

- A long scan on Android 9, as LDPlayer runs, no longer runs out of memory and gets killed.
- The app no longer crashes when Android stops the screen capture.

## [0.1.1] - 2026-09-27

### Changed

- The APK is published as `wor-scanner.apk`, so the README's link always downloads the latest release.

## [0.1.0] - 2026-09-27

### Added

- Scans gear, legendary heroes and mythic artifacts off the game's screen and writes a JSON for Azhor’s Master Smithy.
- Exports a scan into the folder LDPlayer or BlueStacks shares with the PC, or through Android's share sheet.

[unreleased]: https://github.com/gloryapps/wor-scanner/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/gloryapps/wor-scanner/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/gloryapps/wor-scanner/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/gloryapps/wor-scanner/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/gloryapps/wor-scanner/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/gloryapps/wor-scanner/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/gloryapps/wor-scanner/releases/tag/v0.1.0
