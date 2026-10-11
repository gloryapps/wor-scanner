# WoR Scanner

Reads your Watcher of Realms account, its epic and legendary heroes, its gear and its artifacts, and
sends it to [Azhor’s Master Smithy](https://azhor-wor.netlify.app).

Releases ship the Windows app, for the game's own PC client on the same PC: it is [right
below](#on-windows). The Android app, which reads the game's screen in an emulator, is deprecated for
now: please don't use it while we work on it ([Android](#android)).

## On Windows

1. Download [`wor-scanner-windows.zip`](../../releases/latest/download/wor-scanner-windows.zip) from the
   latest [release](../../releases/latest), unzip it anywhere, and open `WoR Scanner.exe` in the
   `WoR Scanner` folder. It brings its own Java, so nothing else needs installing. The app is not
   signed, so the first time Windows says it protected your PC: **More info**, then **Run anyway**.
2. Open Watcher of Realms on the same PC as usual, not as administrator, sign in and wait for the
   city; the app says it found the game.
3. Choose the gear and the artifacts to scan, and press **Scan the account**. The app reads the
   account straight from the game's memory, in seconds: there is no screen to prepare, and nothing in
   the game is touched. **Scan it again** after a change in the game.
4. **Send** puts the scan in **Import a scan** on the site, in your account, where it waits until you
   import it. The first time, the app asks for the site's code: open **Import a scan**, press
   **Link**, and type the code it shows within ten minutes.

Scans and a log are kept in `%LOCALAPPDATA%\WoR Scanner`; **Open folder** shows them, and a scan
kept there is imported by dropping it on that same dialog. If Windows will not let the app read the
game, the game was opened as administrator: open it as usual.

## Android

> **Deprecated for now: please don't use the Android app.** It stays at
> [0.3.0](../../releases/tag/v0.3.0) and is no longer kept up with the game or the site while we work
> on it; use the [Windows app](#on-windows) instead. What follows describes 0.3.0.

1. Download `wor-scanner.apk` from the [0.3.0 release](../../releases/tag/v0.3.0).
2. LDPlayer or BlueStacks: drag the APK onto the emulator's window. A phone: open the APK and allow
   installing apps from that source.
3. Open WoR Scanner. The first time, it shows how a scan works and asks for its permissions, saying
   why; one turned off later is asked for again when **Start** is pressed.

When a newer release is out, a banner under the home screen's header offers it. **Update now**
downloads the release and Android asks to install it over the one you have, your readings kept; the
first time, Android also asks to let WoR Scanner install apps.

### What it asks for, and why

| Permission | Why |
| --- | --- |
| Accessibility service | To tap the tiles and scroll the grid in the game while a scan runs. It asks for no events and reads nothing from any app. |
| Draw over other apps | For the small capsule over the game that starts, shows and stops a scan. |
| Screen capture | To read the game's screen. Frames are read on the device and never sent anywhere. |
| Notifications (Android 13+) | For the notification that shows a scan's progress and its Stop. |
| Install apps | To install a newer release it downloaded, once you confirm. |

### Scanning

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

### Export

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
The Android app's release builds send crash reports to Firebase Crashlytics: the crash, the kind
being scanned, the display's size and how a scan ended. Nothing else leaves the device. The Windows
app sends nothing but a scan you send; its log stays in its folder.

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
