# WoR Scanner

Reads your Watcher of Realms account, its epic and legendary heroes, its gear and its artifacts, and
sends it to [Azhor’s Master Smithy](https://azhor-wor.netlify.app).

It runs on Windows, beside the game's own PC client. The Android app is deprecated for now.

## On Windows

1. Download [`wor-scanner-windows.zip`](../../releases/latest/download/wor-scanner-windows.zip) from the
   latest [release](../../releases/latest), unzip it anywhere, and open `WoR Scanner.exe` in the
   `WoR Scanner` folder. It brings its own Java, so nothing else needs installing. The app is not
   signed, so the first time Windows says it protected your PC: **More info**, then **Run anyway**.
2. Open Watcher of Realms on the same PC as usual, not as administrator, sign in and wait for the
   city; the app says it found the game.
3. Choose the gear and the artifacts to scan, and whether the scan replaces what the site holds or
   is added to it, and press **Scan the account**. The app reads the account straight from the game's
   memory, in seconds: there is no screen to prepare, and nothing in the game is touched. **Scan it
   again** after a change in the game.
4. **Send** puts the scan straight on your account on the site. The first time, the app asks for the
   site's code: open **Import a scan**, press **Link**, and type the code it shows within ten minutes.

Scans and a log are kept in `%LOCALAPPDATA%\WoR Scanner`; **Open folder** shows them, and a scan
kept there is imported by dropping it on that same dialog. If Windows will not let the app read the
game, the game was opened as administrator: open it as usual.

## Android

Deprecated for now.

## Privacy

Scans stay on the PC until you send them, and are not backed up. **Send** uploads a scan's JSON to
Azhor’s Master Smithy, into the account the scanner is linked to. Nothing else leaves the PC; the
log stays in the app's folder.

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
