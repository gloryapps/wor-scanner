# WoR Scanner

Reads your Watcher of Realms gear, legendary and epic heroes, and artifacts off the screen, one tile
at a time, and writes a JSON file you import into
[Azhor’s Master Smithy](https://azhor-wor.netlify.app).

It comes two ways: an Android app for the game running in an emulator on a PC, LDPlayer or
BlueStacks (it may work on a phone, but that has not been tried), and a Windows app for the game's
own PC client, beside it on the same PC. The Windows app is [further down](#on-windows).

## Install

1. Download [`wor-scanner.apk`](../../releases/latest/download/wor-scanner.apk) from the latest [release](../../releases/latest).
2. LDPlayer or BlueStacks: drag the APK onto the emulator's window. A phone: open the APK and allow
   installing apps from that source.
3. Open WoR Scanner. The first time, it shows how a scan works and asks for its permissions, saying
   why; one turned off later is asked for again when **Start** is pressed.

When a newer release is out, a banner under the home screen's header offers it. **Update now**
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

Press **Start** on the home screen, go to the game, tap the capsule to pick what to scan, and press
**Scan**.

- **Gear**: open Storage → Gear, filtered and sorted as you want it scanned, and select the first piece.
- **Heroes**: open Hero, as cards or as squares, sort by Rarity ↓ and select the first hero. The
  scan reads every legendary and epic and stops at the first hero below epic, so keep those out of
  your favourites.
- **Artifacts**: open Storage → Artifact, sorted as you want it scanned, and select the first
  artifact. The scan reads every artifact to the end of the grid, skipping those never enhanced.

The scan taps and scrolls the grid itself. Touching the screen only delays it; Stop, on the capsule or
the notification, stops it, and what it read so far is kept.

**How to scan**, in the home screen's header and on the first run's first page, plays a video of a
whole scan, from Start to Sent.

## Export

When a scan ends, the home screen shows it. **Send to Azhor’s Master Smithy** puts it in **Import a
scan** on the site, in your account, where it waits until you import it. **Save** puts the file in
the folder the emulator shares with the PC (LDPlayer: `Documents\LDPlayer\Pictures\WoR Scanner`;
BlueStacks: the Shared Folder's `WoR Scanner`), and **Share…** hands it to another app. A saved or
shared JSON is imported by dropping it on that same dialog.

The first send links the scanner to your account: open **Import a scan** on the site, press **Link**,
and type the code it shows within ten minutes. From then on, Send is one press.

Every scan is also listed under **All scans**, in the home screen's header, where **export**
saves or shares any of them, and sends it once the scanner is linked; unlink is there too.

A tile the scanner could not read in full keeps a picture of its panel beside the JSON, so it can be
fixed in Azhor’s Master Smithy.

## Privacy

Scans stay on the device until you export them, and are not backed up. **Send** uploads a scan's JSON
to Azhor’s Master Smithy, into the account the scanner is linked to; its pictures stay on the device.
Release builds send crash reports to Firebase Crashlytics: the crash, the kind being scanned, the
display's size and how a scan ended. Nothing else leaves the device. The Windows app sends nothing
but a scan you send; its log stays in its folder.

## On Windows

1. Download [`wor-scanner-windows.zip`](../../releases/latest/download/wor-scanner-windows.zip) from the
   latest [release](../../releases/latest), unzip it anywhere, and open `WoR Scanner.exe` in the
   `WoR Scanner` folder. It brings its own Java, so nothing else needs installing. The app is not
   signed, so the first time Windows says it protected your PC: **More info**, then **Run anyway**.
2. Open Watcher of Realms on the same PC, in a window or borderless; the app says it found it.
3. In the game, ready the screen as for the Android app (above), pick the kind in the app, and press
   **Scan**. The game comes to the front and the scan taps and scrolls it with the mouse, a small sign
   over the game saying how far it is. Its **Stop**, or Esc in the game, stops it, and what it read so
   far is kept. Another window brought to the front only pauses it: it goes on when the game is back.

The text is read by Windows' own text recognition, in the languages of your Windows profile. Scans,
their pictures and a log are kept in `%LOCALAPPDATA%\WoR Scanner`; **Open folder** shows them. The
newest scan waits under the buttons to be sent to Azhor’s Master Smithy, linking the app with the
site's code the first time, as the Android app does. If the game runs as administrator, the app must
too, or Windows keeps its clicks from reaching the game.

## Building

```bash
./gradlew test
./gradlew :app:assembleDebug
./gradlew :windows:run
```

How the code is laid out is in [`bootstrap/architecture.md`](bootstrap/architecture.md); how a release
is built and signed, in [`bootstrap/ci.md`](bootstrap/ci.md).

A debug build has **Debug** in the home screen's header: it opens the first run, and Home with the
newest scan as if it had just ended or with the permission ask open, each as it runs.

## Contributing

Issues are welcome, for a tile read wrong or a scan that stops. Pull requests are read, and merged by
the maintainer only.

## Licence

[MIT](LICENSE).
