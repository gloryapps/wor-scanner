# Architecture & code layout

Settled 2026-09-06, kinds added 2026-09-07, heroes and artifacts 2026-09-26. A native Android app that scans a
Watcher of Realms storage on the device it runs on (LDPlayer, BlueStacks or a phone), captures each
tile's panel, reads it and writes a JSON the azhor lab imports: gear, legendary and epic heroes,
and artifacts. Distributed as an APK on GitHub, not on the Play Store.

## Modules

- `scanner` — pure Kotlin, no Android import. Holds the scan (tap, wait, capture, read, scroll,
  register the row), written over interfaces for the screen, the touch and the text recogniser,
  and one package per kind with that kind's reader (OCR rows to a record, matched against its
  words). Tested on the JVM with recorded frames.
- `app` — Android. Implements those interfaces with MediaProjection (frames), an
  `AccessibilityService` using `dispatchGesture` (taps and swipes, blind: the game is Unity and
  exposes no view tree), ML Kit (text) and the file writer; holds the foreground service that runs
  the scan, the hairline and capsule drawn over the game, the screens, and the update to the
  latest GitHub release, handed to the system's installer.
- The words (sets, slots, attribute names) are `scanner`'s own, transcribed
  from the wiki's Gear page. The app depends on no other repository: what it shares with the azhor
  lab is the JSON it writes, not code. A word the catalogue lacks reads as `null` beside the raw
  lines, and the lab's picker settles it. The wiki's Gear page is behind Cloudflare; the
  transcription was made from azhor-wor's verbatim copy, `docs/gear.md`. If the two transcriptions
  drift, the option on the table is a third project, a library both read; not now.
- The `Exclusive` line's name is written as read; who that hero or faction is belongs to the lab.
- Heroes carry no words of their own: a hero's name is written as printed and its skills by place,
  a number or `"max"`. Which hero, which skill and what `max` stands for belong to the lab, which
  keeps the wiki's pages; a list in the app would be left behind by every hero the game adds.
- Named after what it is, never where it sits: `scanner`, not `core:domain`.

## Kinds

A kind is one thing the app scans: gear, heroes and artifacts. `scanner` is split so
that the walk over a grid is written once and a kind says only what its tiles hold.

| Package | Holds | Knows |
| --- | --- | --- |
| `text/` | `Line`, `Box`, `rowsOf`, matching, `numbersIn`, `wordIn`, `nameIn`, `readsAsCapitals` | nothing |
| `senses/` | `Screen`, `Touch`, `TextReader`, `Frame`, `Colour`; the recogniser hands back `text/`'s lines | text |
| `game/` | `Attribute`, `ReadAttribute`, `attributesIn`, `headOf`, `exclusiveIn`: the words every kind shares and the rows they are read off | text |
| `scan/` | `Scan<T>`, `Walk`, `Seen`, `Tapped`, `Read<T>`, `GridLayout`, `Region`, `Spot`, `ScanEntry<T>`, `Outcome<T>`, tiles, registration, count | senses, text |
| `kinds/gear/` | `ScannedGear`, its words, `GEAR_STORAGE`, `GearScan` | scan, text, game |
| `kinds/hero/` | `ScannedHero`, `HeroSkills`, `SkillLevel`, `HERO_CARDS`, `HERO_SQUARES`, their rank edges and spots, `HeroScan` and its two views | scan, text, senses |
| `kinds/artifact/` | `ScannedArtifact`, `ARTIFACT_STORAGE`, `ArtifactScan` | scan, text, game |
| `kinds/` | `Kind`, `Kind.scan()` | every kind |

- Arrows point down only. `scan/` knows no kind; each kind's package extends its `Scan<T>`;
  `Kind.scan()` in `kinds/` is the one place every kind is named, and `Kind.named` in the app's
  `ui/Kinds.kt` is the other, for its label, the home screen's steps and the menu's reminder.
- `Kind` is an enum: identity only. Its id is the JSON's `kind` and the scan folder's, `entries`
  is what the overlay lists, and a `when` over it is exhaustive.
- `Scan<T>` is an abstract class: a kind's `GridLayout`, its serializer, `readTile` (what it reads
  off the tile the walk just tapped), `readScreen` (what it makes of a whole frame, for the
  overlay's Read), `tileAt` (whether a tile sits at a place, by default the word it prints
  below its centre; an artifact's tile prints none and is told by the colour of its face, a hero's
  by the rank its edge shows) and `viewOn` (the scan for the view the first frame shows, where the
  screen shows its grid more than one way, as the hero roster's cards and squares). One
  stateless `object` per kind, or per view, extends it. Its `run` starts a `Walk`, created
  per scan with the senses of the moment, which holds where the grid is and taps, verifies,
  drags and registers for every kind.
- `readTile` gets a `Tapped`: the frame the tap left, the tile's rectangle on it, `show(tab)`
  (taps a tab until the panel changes, a dropped tap tapped again) and `regrip()` (once the kind's
  taps have moved the grid, finds the tile again by its frame anywhere in its column and takes the
  grid's place from it). It returns a `Read<T>`: `Card` (the record, its rows, its frames, whether
  it is closed), `Skipped` (nothing kept, the walk goes on), `Beyond` (the scan finishes before this
  tile) or `Lost` (the scan stops, the grid lost).
- A kind's `Scan` object reads its panel with its own private helpers, its model of that panel:
  kinds share instruments, not an algorithm. There is no base reader and no shared card shape; a
  hero panel is not a gear panel.
- `text/` holds the instruments a reader is written with, model-free by construction: each takes
  a row or a block and a candidate list and knows nothing of what the row is part of. What a
  second kind turns out to need from `GearScan`'s private helpers is lifted to `text/` or `game/`
  before that kind is written, never on a guess.
- The records have no supertype. Generics carry `T` through `ScanEntry<T>`, `Outcome<T>` and
  `ScanFile<T>` to the writer, which encodes with the kind's serializer; the app holds
  `Outcome<*>` where it only counts entries. On the wire `kind` is the discriminator.
- The walk assumes a grid with a side panel redrawn on tap, the tap verified by the framed tile.
  It begins on the framed tile, found anywhere in the grid, and only where none is framed on the
  first whole row by its words.
  What a tile's panel shows, and any tapping it takes to show it all, is the kind's `readTile`,
  with what `Tapped` lends it.

### Adding a kind

Written after heroes and followed for artifacts. The compiler enforces step 5.

1. Record the kind's screen inside LDPlayer through the overlay's Read, on every tab its panel is
   read under, and keep one whole frame under the tests as `ldplayer-storage-1280x720.json`,
   `ldplayer-heroes-1280x720.json` and `ldplayer-artifacts-1280x720.json` are kept. The panels'
   lines go inline into the kind's test; colours the reading needs are measured then and written
   into the plan, not kept as images. This is where the walk's assumption is checked: a grid with a
   side panel redrawn on tap, and a word printed below each tile to find it by.
2. `kinds/<kind>/Scanned<Kind>.kt`: the record, `@Serializable`, what the panel may fail to name
   nullable.
3. `kinds/<kind>/<Kind>Roster.kt` (or `Storage`): its `GridLayout`, and any spot it taps or samples,
   measured off the recorded frame and tested the way `LdPlayerReadingTest` tests gear's.
4. `kinds/<kind>/<Kind>Scan.kt`: one `object` extending `Scan<T>`, its `readTile` using what
   `Tapped` lends and its model of the panel written with `text/`'s instruments and `game/`'s
   readings; its `tileAt` where its tiles print no word below them, as the artifacts' face. Words go in the
   kind's package where the record names catalogue entries, as gear's sets do, transcribed from the
   wiki as it spells them; where the lab can identify from what is printed, it is kept as printed.
5. `Kind.<KIND>`; the compiler then asks for its branch in `Kind.scan()` and in the app's
   `Kind.named`.
6. Tell the lab the kind's shape: `kind` names it and `entries[].card` is shaped by it.

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
  `kind` it scanned; a new scan is a new file, never a merge. The user takes it out through the share
  sheet, saves it into the folder the emulator shares with the PC, or sends it to the lab once the
  scanner is linked.
- What leaves the app is named `wor-<kind>-<stamp>`: `wor-gear-20260907-130812.json`, with a scan's
  kept panels beside it as `wor-gear-20260907-130812-<tile>.png`. On disk the names stay `scan.json`
  and `<tile>.png`; `Exports` copies each file under the name its caller gives, which is what keeps
  two exports of the same kind apart in one folder. The share sheet stages its copies in
  the cache so the other app is shown those same names.
- Until `scan.json` is written, a scan is its `journal.jsonl`: a first line holding the file as it
  reads should the process die, `stopped:interrupted`, then one line per entry, appended by the
  walk's `Keeper` the moment the entry is read. Writing `scan.json` deletes it. `Readings` is made as
  the process starts and closes every journal it finds into its `scan.json` before anything is
  listed: no scan of the new process has begun by then. A process that dies loses at most the tile
  it was reading.
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
- The clipboard does not cross that border.
- The network does, once the scanner is linked. The lab's Import a scan shows a code; the home
  screen, the first time a scan that just ended is sent, trades it at the lab's `POST /scanner/link`
  for a token, and `azhor/Link` keeps that token in a DataStore of its own, `link`. Send, there or
  in the export sheet, posts each scan's `scan.json`, gzipped, to
  `POST /scanner/scans` with the token as a bearer; the pictures stay behind. A 401 means the lab
  no longer knows the token, and the scanner forgets it. The lab's address is `BuildConfig.AZHOR_URL`,
  from the Gradle property `azhor.url`, and a debug build allows cleartext so it can point at the lab
  running on the PC.
- Preferences in DataStore. `Chosen` holds which kind the next scan reads, picked in the overlay's
  menu, held at once and written behind; `link` holds the lab's token; `first_run` whether the
  first run was seen, set at once on an install that already held the grants a scan needs.
- Room enters only if scan history inside the app is ever wanted, and brings the no-destructive-
  migration rule with it.

## Navigation

- One Activity, Compose, Navigation 3 from the start: the back stack is a state list the app owns.
  It starts on the first run until that is seen, on Home after; leaving the first run clears it.
- `HowToScan` plays `res/raw/how_to_scan.mp4`, a whole scan filmed on LDPlayer, in the platform's
  `VideoView`; Home's header and the first run's first page lead to it.
- `Home(stage)` opens Home in a state it is otherwise only reached by playing. Only a debug build's
  Debug screen, behind a link in Home's header, opens one: `ui/debug/Debug.kt` is given by `src/debug`
  and by `src/release`, whose screen and link draw nothing.
- The overlay belongs to the service, not the Activity, and is drawn in Compose too. It is four
  windows: the hairline pinned to the top, the capsule a finger drags, the menu its tap opens beside
  it, and the close target that appears under it while it is held.

## Error handling

- `resultOf { }` in `scanner`, never `runCatching`: a `try` that returns `Result` but rethrows
  `CancellationException`, with `TimeoutCancellationException` counted as a failure. `runCatching`
  swallows cancellation, which in a minutes-long loop shows up as a stop button that sometimes does
  not stop.
- `resultOf` wraps the calls that throw: a capture, an OCR pass, a gesture.
- The end of a scan is a sealed type of its own, not a `Result`: finished, stopped for a named
  reason (the screen not open, the grid lost, stopped by the player, the app closed mid-scan),
  failed with cause.
  The UI renders each with its own verb.
- A tile that reads badly is not a scan error: it lands in the JSON with nulls and its PNG, and
  the scan goes on. Only what prevents the next tap stops a scan.

## What the storage screen fixes

- The piece's card is the right-hand panel, redrawn on tap, not a popup: the loop is tap, wait,
  capture the panel, next, with no back.
- The grid scrolls continuously, seven per row, under a scrollbar. A swipe does not move an exact
  number of rows: the scan drags and holds the finger still before it lifts (no fling), then finds
  where the last row it had seen now sits by the framed tile, else by matching tiles' words, and
  takes its position from that; only the frame the grid settles on is read. The
  header's count (`1,169/2,500`) says the screen is open and what the progress counts to; the scan
  ends where the rows run out, or where the kind says the tiles it scans end.
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
