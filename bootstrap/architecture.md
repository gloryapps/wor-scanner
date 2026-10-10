# Architecture & code layout

Settled 2026-09-06, Windows added 2026-10-05. Scans a Watcher of Realms storage, reads each tile's
panel and writes a JSON the azhor lab imports: an Android app on the device the game runs on (an
emulator or a phone), and a Windows app beside the game's own PC client. Distributed on GitHub, not
on a store.

## Modules

- `scanner` — pure Kotlin, no Android import: the scan, its kinds, the file a scan leaves and the
  lab's client, written over interfaces a platform implements. Tested on the JVM with recorded
  frames.
- `app` — Android: implements `scanner`'s interfaces and holds everything the player sees.
- `ui` — Kotlin Multiplatform, Android and the JVM: what every platform draws with and says the same
  way, in Compose Multiplatform. It holds no screen; screens are each platform's.
- `windows` — Windows, beside the game's own PC client: opens the game's memory as `scanner`'s
  `Memory`, reads the account off it and sends it to the lab, and holds everything the player sees
  there, in Compose for Desktop. It scans no screen.
- What the build knows once, every platform reads from `gradle.properties`: the app's name, its
  version and the lab's address.
- A module is named after what it is, never where it sits: `scanner`, not `core:domain`.

## Words

- The words a reader matches (sets, slots, attributes) are `scanner`'s own, transcribed from the
  wiki as it spells them. A word the catalogue lacks reads as `null` beside the raw lines, and the
  lab's picker settles it. The wiki's Gear page is behind Cloudflare; azhor-wor's `docs/gear.md` is
  its verbatim copy.
- What the lab can identify from what is printed is kept as printed: a hero's name, an exclusive's
  name, a skill by place. The app keeps no list of heroes or artifacts; it would fall behind every
  one the game adds.
- If the app's transcription and the lab's drift, the option is a third project both read; not now.
- An account read off the game's memory names everything by the client's ids. `account/`'s `Cards`
  turns them into the same cards a scan writes with `resources/game.json`, which wor-extract's
  `tools/export_scanner.py` writes from one version of the client: a hero, set or artifact the game
  adds reaches the lab once it is written again and shipped.

## Inside `scanner`

| Package | What it is | May know |
| --- | --- | --- |
| `text/` | the instruments a reader is written with: each takes a row or a block and a candidate list, and knows nothing of what the row is part of | nothing |
| `senses/` | the interfaces a platform implements: what the display shows, the hand, the recogniser, the game's memory | text |
| `game/` | the words every kind shares and the rows they are read off | text |
| `scan/` | the walk over a grid, written once for every kind | senses, text |
| `kinds/<kind>/` | one kind: its record, where its screen puts things, how its panel reads | scan, game, senses, text |
| `kinds/` | `Kind`, and `Kind.scan()` | every kind |
| `lua/` | Lua 5.3's strings and tables in the game's memory, as a 64-bit build lays them out | senses |
| `account/` | the account as the running game holds it: the table each list lives in, found by a field only that table has; and its lists as the cards each kind's scan writes | lua, senses, game, kinds |
| `runs/` | a scan run and kept on disk, as the lab reads it | everything above |
| `azhor/` | the lab's client | nothing |

- Arrows point down only. `scan/` knows no kind.
- Two places name every kind: `Kind.scan()` in `scanner`, and `Kind.named` in the `ui` module, for
  what the player reads on every platform. `Kind` is an enum, identity only: its id is the JSON's
  `kind`, and a `when` over it is exhaustive.
- A kind is one stateless `object` extending `Scan<T>`, or one per view where its screen shows the
  grid more than one way. It says what a tile holds; the walk taps, verifies, drags and registers.
- Kinds share instruments, not an algorithm: each reads its panel with its own private model of
  that panel. There is no base reader and no shared card shape.
- What a second kind needs from another kind's private helpers is lifted to `text/` or `game/`
  before that kind is written, never on a guess.
- The records have no supertype: generics carry `T` from the kind to the file, and on the wire
  `kind` is the discriminator.
- The walk assumes a grid with a side panel redrawn on tap, the tap verified by the framed tile.
  What a panel shows, and any tapping it takes to show it all, is the kind's.

### Adding a kind

Written after heroes and followed for artifacts. The compiler enforces step 5.

1. Record the kind's screen inside LDPlayer through the overlay's Read, on every tab its panel is
   read under, and keep one whole frame under the tests. The panels' lines go inline into the
   kind's test; colours the reading needs are measured then and written into the plan, not kept as
   images. This is where the walk's assumption is checked.
2. The record, `@Serializable`, with what the panel may fail to name nullable.
3. Its `GridLayout` and any spot it taps or samples, measured off the recorded frame and tested
   against it.
4. Its `Scan` object, written with `text/`'s instruments and `game/`'s readings. Words go in the
   kind's package where the record names catalogue entries, transcribed from the wiki.
5. `Kind.<KIND>`; the compiler then asks for its branch in `Kind.scan()` and in `Kind.named`.
6. Tell the lab the kind's shape: `kind` names it and `entries[].card` is shaped by it.

## What the game's screens fix

- A tile's card is a side panel redrawn on tap, not a popup: the loop is tap, wait, capture, next,
  with no back.
- A drag does not move an exact number of rows. The scan drags and holds still before lifting (no
  fling), then finds the row it had seen and takes its position from that.
- Two equal pieces give two equal panels, so a piece's identity is its grid position, never its
  content.
- Positions are fractions of the display, never pixels: the app runs on any resolution, and a
  second aspect ratio is to be checked against the first sample.

## State

- Unidirectional per screen, in one contract file: a `UiState` out on one `StateFlow`, `Event`s in
  through `on(event)`, one-shot `Effect`s out through a channel the screen collects. What screens
  share is a delegate the ViewModel holds, not a base class.
- The scan runs in a foreground service, because the Activity is gone once the game is in front.
  The service exposes progress as a flow; the overlay and the screens only render it.

## Dependency injection

- Koin. Platform adapters and repositories `single`, use cases `factory`, one ViewModel per screen.
- A use case exists only when there is real logic; a pass-through call does not create a layer.
- The owner of heavy work owns its threading; presentation never shifts threads defensively.

## Persistence

- No database. A scan is one JSON file, `version` 2 with the `kind` it scanned; a new scan is a
  new file, never a merge. Every entry carries the raw OCR lines it was read from, and a panel PNG
  is kept only for a piece the reader did not close.
- Until its file is written, a scan is its journal, appended the moment each entry is read and
  closed into the file when the next process starts: a process that dies loses at most one tile.
- A scan leaves the app by the folder an emulator shares with the PC, the share sheet, or the lab.
  The shared folders sit outside the sdcard, so the app asks for no storage permission; the
  clipboard does not cross an emulator's border.
- The lab is reached at its two addresses only: a code traded once for a token, and each scan
  posted with that token. A 401 forgets the token. A debug build never reaches the lab.
- An account read off the game's memory is one JSON file under `accounts/`, `kind` `account`, each
  entry in the game's own fields, with how many entries every table holding each list had, under
  `reads/`, apart from the folder of `accounts/` holding a scan of each kind made of it, its gear only
  in the bands of enhancement the player chose. A kind with no cards has no scan: the lab takes a
  scan as the whole of its kind. Those scans are what the Windows app sends the lab and opens for
  the player.
- Preferences in DataStore. Room enters only if scan history inside the app is ever wanted, and
  brings the no-destructive-migration rule with it.

## Navigation

- One Activity, Compose, Navigation 3: the back stack is a state list the app owns.
- The overlay belongs to the service, not the Activity, and is drawn in Compose too.

## Error handling

- `resultOf { }`, never `runCatching`: it rethrows `CancellationException` and counts a timeout as
  a failure. `runCatching` swallows cancellation, which in a minutes-long loop shows up as a stop
  button that sometimes does not stop. It wraps the calls that throw: a capture, an OCR pass, a
  gesture.
- The end of a scan is a sealed type of its own, not a `Result`: finished, stopped for a named
  reason, or failed with its cause. The UI renders each with its own verb.
- A tile that reads badly is not a scan error: it lands in the JSON with nulls and its PNG, and the
  scan goes on. Only what prevents the next tap stops a scan.

## Open decisions

- Which game languages the reader must know beyond English. The catalogue's ids stay English, and
  each language adds its own words for the same entries.
- Which other emulators to wire beyond LDPlayer and BlueStacks.
