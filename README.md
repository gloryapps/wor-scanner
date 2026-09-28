# WoR Scanner

An Android app that reads your Watcher of Realms gear, legendary and epic heroes, and artifacts off
the screen, one tile at a time, and writes a JSON file you import into
[Azhor’s Master Smithy](https://azhor-wor.netlify.app).

It is made for the game running in an emulator on a PC, LDPlayer or BlueStacks. It may work on a
phone, but that has not been tried.

## Install

1. Download [`wor-scanner.apk`](../../releases/latest/download/wor-scanner.apk) from the latest [release](../../releases/latest).
2. LDPlayer or BlueStacks: drag the APK onto the emulator's window. A phone: open the APK and allow
   installing apps from that source.
3. Open WoR Scanner and grant the two permissions its home screen lists.

When a newer release is out, the home screen's header offers it beside the version. Tapping it
downloads the release and Android asks to install it over the one you have, your readings kept; the
first time, Android also asks to let WoR Scanner install apps.

## What it asks for, and why

| Permission | Why |
| --- | --- |
| Accessibility service | To tap the tiles and scroll the grid in the game while a scan runs. It asks for no events and reads nothing from any app. |
| Draw over other apps | For the small capsule over the game that starts, shows and stops a scan. |
| Screen capture | To read the game's screen. Frames are read on the device and never sent anywhere. |
| Notifications (Android 13+) | For the notification that shows a scan's progress and its Stop. |
| Install apps | To install a newer release it downloaded, once you confirm. |

## Scanning

Pick what to scan on the home screen or in the capsule's menu, press **Start**, go to the game, and
press **Scan** on the capsule.

- **Gear**: open Storage → Gear, filtered and sorted as you want it scanned, and select the first piece.
- **Heroes**: open Hero, as cards or as squares, sort by Rarity ↓ and select the first hero. The
  scan reads every legendary and epic and stops at the first hero below epic, so keep those out of
  your favourites.
- **Artifacts**: open Storage → Artifact, sorted as you want it scanned, and select the first
  artifact. The scan reads every artifact to the end of the grid, skipping those never enhanced.

The scan taps and scrolls the grid itself; touching the screen stops it, and what it read so far is
kept.

## Export

Every scan is listed on the home screen. **Export** saves it into the folder the emulator shares with
the PC (LDPlayer: `Documents\LDPlayer\Pictures\WoR Scanner`; BlueStacks: the Shared Folder's
`WoR Scanner`), or hands it to another app. Import that JSON in Azhor’s Master Smithy.

A tile the scanner could not read in full keeps a picture of its panel beside the JSON, so it can be
fixed in Azhor’s Master Smithy.

## Privacy

Scans stay on the device until you export them, and are not backed up. Release builds send crash
reports to Firebase Crashlytics: the crash, the kind being scanned, the display's size and how a scan
ended. Nothing else leaves the device.

## Building

```bash
./gradlew test
./gradlew :app:assembleDebug
```

How the code is laid out is in [`bootstrap/architecture.md`](bootstrap/architecture.md); how a release
is built and signed, in [`bootstrap/ci.md`](bootstrap/ci.md).

## Contributing

Issues are welcome, for a tile read wrong or a scan that stops. Pull requests are read, and merged by
the maintainer only.

## Licence

[MIT](LICENSE).
