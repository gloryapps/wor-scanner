# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.6.2] - 2026-10-13

### Changed

- The site is now WoR Armory, at wor-armory.netlify.app, and scans are sent there.

## [0.6.1] - 2026-10-11

### Changed

- The scanner keeps its last 20 scans, deleting older ones, where the folder grew with every scan.
- **On the site** says the gear plans go with the rest: replaced by the game’s plan, or the game’s
  plan added beside them.

### Fixed

- With Watcher of Realms minimised, the scanner still finds it and scans the account, where it
  asked to open the game.
- The scanner finds the game whatever its client titles its window, where it looked for the
  global client's title only.
- **Try again** after a send that stopped halfway sends only what Azhor’s Master Smithy had not
  taken yet, so gear added to the site no longer arrives twice.

## [0.6.0] - 2026-10-11

### Added

- A scan also sends the game's gear recommendation rules, which Azhor’s Master Smithy keeps as a
  gear plan.

## [0.5.1] - 2026-10-11

### Fixed

- Where Watcher of Realms runs as administrator, the scanner says so, and **Restart as
  administrator** opens it again as administrator, so it can read the game.

## [0.5.0] - 2026-10-11

### Added

- Choose under **On the site** whether a scan replaces the heroes, gear and artifacts Azhor’s Master
  Smithy holds, as it does unless you choose otherwise, or is added to them.
- A scan sends how many heroes of each faction the account has collected, of every rarity, as the
  game’s Collection Progress shows them.

### Changed

- A scan sent is on your account on the site at once, with nothing left to import there.

## [0.4.0] - 2026-10-11

### Added

- WoR Scanner for Windows. With Watcher of Realms open on the same PC, **Scan the account** reads
  every epic and legendary hero, piece of gear and artifact straight from the game, in seconds and
  without opening a screen, and **Send** puts them in Azhor’s Master Smithy. The game runs as usual;
  nothing needs administrator rights.
- A variant piece carries its variant's name, the effect it rolled and the attributes that effect
  adds; an ancient piece's variant is marked ancient.

### Changed

- The gear to scan goes by the game's bands of enhancement, +16 alone unless you choose more, and a
  secondary the piece's level has not revealed yet is left off its card.
- The artifacts to scan are those worth wearing, every +25 and every exclusive from +10, unless you
  choose all.
- A scan the site will not take says to keep the file instead.

### Deprecated

- The Android app, for now.

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

[unreleased]: https://github.com/gloryapps/wor-scanner/compare/v0.6.2...HEAD
[0.6.2]: https://github.com/gloryapps/wor-scanner/compare/v0.6.1...v0.6.2
[0.6.1]: https://github.com/gloryapps/wor-scanner/compare/v0.6.0...v0.6.1
[0.6.0]: https://github.com/gloryapps/wor-scanner/compare/v0.5.1...v0.6.0
[0.5.1]: https://github.com/gloryapps/wor-scanner/compare/v0.5.0...v0.5.1
[0.5.0]: https://github.com/gloryapps/wor-scanner/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/gloryapps/wor-scanner/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/gloryapps/wor-scanner/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/gloryapps/wor-scanner/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/gloryapps/wor-scanner/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/gloryapps/wor-scanner/compare/v0.1.1...v0.1.2
[0.1.1]: https://github.com/gloryapps/wor-scanner/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/gloryapps/wor-scanner/releases/tag/v0.1.0
