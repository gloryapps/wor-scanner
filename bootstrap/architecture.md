# Architecture & code layout

Settled 2026-09-06, kinds added 2026-09-07. A native Android app that scans a Watcher of Realms
storage on the device it runs on (LDPlayer, BlueStacks or a phone), captures each tile's panel, reads it and
writes a JSON the azhor lab imports. Gear today; heroes and artifacts are kinds still to add.
Distributed as an APK on GitHub, not on the Play Store.

## Modules

- `scanner` — pure Kotlin, no Android import. Holds the scan (tap, wait, capture, read, scroll,
  register the row), written over interfaces for the screen, the touch and the text recogniser,
  and one package per kind with that kind's reader (OCR rows to a record, matched against its
  words). Tested on the JVM with recorded frames.
- `app` — Android. Implements those interfaces with MediaProjection (frames), an
  `AccessibilityService` using `dispatchGesture` (taps and swipes, blind: the game is Unity and
  exposes no view tree), ML Kit (text) and the file writer; holds the foreground service that runs
  the scan, the hairline and capsule drawn over the game, and the two screens.
- The words (sets, slots, attribute names, variants, factions) are `scanner`'s own, transcribed
  from the wiki's Gear page. The app depends on no other repository: what it shares with the azhor
  lab is the JSON it writes, not code. A word the catalogue lacks reads as `null` beside the raw
  lines, and the lab's picker settles it. The wiki's Gear page is behind Cloudflare; the
  transcription was made from azhor-wor's verbatim copy, `docs/gear.md`. If the two transcriptions
  drift, the option on the table is a third project, a library both read; not now.
- The `Exclusive` line's name is written as read; who that hero or faction is belongs to the lab.
- Named after what it is, never where it sits: `scanner`, not `core:domain`.

## Kinds

A kind is one thing the app scans: gear today, heroes and artifacts next. `scanner` is split so
that a kind is a package with its own model behind one small contract, and the scan knows none.

| Package | Holds | Knows |
| --- | --- | --- |
| `senses/` | `Screen`, `Touch`, `TextReader`, `Frame` | nothing |
| `text/` | `Line`, `Box`, `rowsOf`, matching, `numbersIn`, `wordIn`, `nameIn` | senses |
| `game/` | `Attribute`, `FACTIONS`, `Named`, `ReadAttribute`: the words every kind shares | text |
| `kinds/` | the contracts: `Kind`, `Scannable<T>`, `Reader<T>`, `GridLayout`, `Region` | text |
| `kinds/gear/` | `ScannedGear`, its words, `GearReader`, `GEAR_STORAGE`, `GearScannable` | kinds, text, game |
| `scan/` | `Kind.scannable()`, `Scan<T>`, `ScanEntry<T>`, `Outcome<T>`, tiles, registration, count | kinds, and every kind through one `when` |

- Arrows point down only. Every kind is a sub-package of `kinds/`, whose root holds the contracts; they sit below both the kinds and the scan: a kind implements
  them without knowing the scan, the scan consumes them without knowing a kind. `Kind.scannable()`
  in `scan/` is the one place every kind is named; `Kind.label()` in the app is the other, for the
  overlay's button.
- `Kind` is an enum: identity only. Its id is the JSON's `kind` and the scan folder's, `entries`
  is what the overlay lists, and a `when` over it is exhaustive.
- `Scannable<T>` is what the scan needs from a kind: a `GridLayout`, a serializer, a `Reader<T>`.
  One `object` per kind implements it; that object is the kind's single door.
- `Reader<T>` is `read(rows): T` and `closed(record)`. Readers share instruments, not an
  algorithm: each kind's panel is its own model, read by that kind's `object` with its own
  private helpers. There is no base reader and no shared card shape; a hero panel is not a gear
  panel.
- `text/` holds the instruments a reader is written with, model-free by construction: each takes
  a row or a block and a candidate list and knows nothing of what the row is part of. What a
  second reader turns out to need from `GearReader`'s private helpers is lifted to `text/`
  before that reader is written, never on a guess.
- The records have no supertype. Generics carry `T` through `ScanEntry<T>`, `Outcome<T>` and
  `ScanFile<T>` to the writer, which encodes with the kind's serializer; the app holds
  `Outcome<*>` where it only counts entries. On the wire `kind` is the discriminator.
- The scan is a grid with a side panel redrawn on tap, verified by the framed tile and read off
  the same frame. A kind whose tile opens a full screen makes `Scannable` say how a tile opens
  and the scan branch on it; that is written off a recorded frame of that screen, not before.

### Adding a kind

Written for heroes; the same for artifacts. The compiler enforces steps 4, 6 and 7.

1. Record the hero screen inside LDPlayer, grid and panel, and keep the frame under the tests as
   `ldplayer-storage-1280x720.json` is kept for gear. This is where the scan's assumption is
   checked: a grid with a side panel, or something else.
2. `kinds/hero/ScannedHero.kt`: the record, `@Serializable`, what the panel may fail to name nullable.
3. `kinds/hero/` words: the hero's own, transcribed from the wiki as it spells them; attributes and
   factions are in `game/` already.
4. `kinds/hero/HeroReader.kt`: `object HeroReader : Reader<ScannedHero>`, written against recorded
   panels with `text/`'s instruments.
5. `kinds/hero/HeroRoster.kt`: `HERO_ROSTER: GridLayout`, measured off the recorded frame, tested the
   way `LdPlayerReadingTest` tests gear's.
6. `kinds/hero/HeroScannable.kt`: `object HeroScannable : Scannable<ScannedHero>`.
7. `Kind.HEROES`; the compiler then asks for its branch in `Kind.scannable()` and its string in
   `Kind.label()`.
8. Tell the lab the `hero` shape: `kind` names it and `entries[].card` is shaped by it.

## State

- Unidirectional per screen, in one contract file: a `UiState` out on one `StateFlow`, `Event`s in through
  `on(event)`, one-shot `Effect`s back out through a channel the screen collects. What a screen shares
  with another, such as exporting, is a delegate the ViewModel holds, not a base class.
- The scan runs in a foreground service, because the Activity is gone once the game is in front.
  The service exposes progress as a flow; the overlay and the screens only render it.

## Dependency injection

- Koin. Lifecycles: platform adapters and repositories `single`, use cases `factory`, one
  ViewModel per screen.
- A use case exists only when there is real logic; a pass-through call does not create a layer.
- The owner of heavy work owns its threading; presentation never shifts threads defensively.

## Persistence

- No database. A scan is a JSON file in the app's external files directory, `version` 2 with the
  `kind` it scanned; a new scan is a new file, never a merge. The user takes it out through the share sheet or saves it to Downloads via
  MediaStore. Sending straight to the lab is a later option.
- What leaves the app is named `wor-<kind>-<stamp>`: `wor-gear-20260907-130812.json`, with a scan's
  kept panels beside it as `wor-gear-20260907-130812-<tile>.png`. On disk the names stay `scan.json`
  and `<tile>.png`; `Exports` copies each file under the name its caller gives, which is what keeps
  two exports of the same kind apart in one Downloads folder. The share sheet stages its copies in
  the cache so the other app is shown those same names.
- Every entry in the JSON carries the raw OCR lines it was read from.
- The panel PNG is kept only for a piece the reader did not close: set or slot null, or the card
  refused. A full run keeps no other image.
- A scan leaves an emulator by the folder that emulator shares with the PC. `Emulator` holds the mounts
  each one is known by and the file system says which is running: LDPlayer mounts `/mnt/shared/Pictures`,
  which Windows shows under `Documents\LDPlayer\Pictures`; BlueStacks mounts its Media Manager's Shared
  Folder at `/mnt/windows/BstSharedFolder`, shown under
  `ProgramData\BlueStacks_nxt\Engine\UserData\SharedFolder`; it has moved between versions, so the
  paths under `/sdcard` it used follow, and its own `/mnt/windows` closes the list. Files land in a `WoR Scanner` folder inside
  it, and the export sheet names the emulator it found, says where the PC shows it, and lets a second one
  be chosen over the first.
- Neither mount needs a grant, both sitting outside the sdcard, so the app asks for no storage
  permission at all; a device with no mount is told to use the share sheet instead.
- The clipboard does not cross that border, and the two apps are not linked over the network.
- Preferences in DataStore. `Chosen` is the only one so far: which kind the next scan reads, which
  the home screen picks and the overlay obeys.
- Room enters only if scan history inside the app is ever wanted, and brings the no-destructive-
  migration rule with it.

## Navigation

- One Activity, Compose, Navigation 3 from the start: the back stack is a state list the app owns,
  which lets the service push the follow-up screen when a scan ends.
- The overlay belongs to the service, not the Activity, and is drawn in Compose too. It is three
  windows: the hairline pinned to the top, the capsule a finger drags, and the close target that
  appears under it while it is held.

## Error handling

- `resultOf { }` in `scanner`, never `runCatching`: a `try` that returns `Result` but rethrows
  `CancellationException`, with `TimeoutCancellationException` counted as a failure. `runCatching`
  swallows cancellation, which in a minutes-long loop shows up as a stop button that sometimes does
  not stop.
- `resultOf` wraps the calls that throw: a capture, an OCR pass, a gesture.
- The end of a scan is a sealed type of its own, not a `Result`: finished, stopped for a named
  reason (storage closed, the grid no longer matches, projection consent lost), failed with cause.
  The UI renders each with its own verb.
- A tile that reads badly is not a scan error: it lands in the JSON with nulls and its PNG, and
  the scan goes on. Only what prevents the next tap stops a scan.

## What the storage screen fixes

- The piece's card is the right-hand panel, redrawn on tap, not a popup: the loop is tap, wait,
  capture the panel, next, with no back.
- The grid scrolls continuously, seven per row, under a scrollbar. A swipe does not move an exact
  number of rows: the scan drags slowly (no fling), then finds where the last row it had seen now
  sits by matching tiles, and takes its position from that. The header's count (`1,169/2,500`) gives
  the number of rows and the stop.
- Two equal pieces give two equal panels, so a piece's identity is its grid position, never its
  content.
- Positions are fractions of the display read at start, never pixels: the app runs on any
  resolution, and a second aspect ratio is to be checked against the first sample.
- The card's banner says `Variant` where the old reader knew `Ancient`; both are words of the
  reader.

## Open decisions

- Interview the user to define which game languages the reader must know beyond English. Many
  players run the game in another language; the catalogue's ids stay English and each language
  adds its own words for the same entries.
- Interview the user to define which other emulators to wire. LDPlayer and BlueStacks are the two
  `Emulator` knows; MEmu and Nox are mounts nobody has measured yet.
