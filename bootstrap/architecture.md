# Architecture & code layout

Settled 2026-09-06. A native Android app that walks the Watcher of Realms gear storage on the
device it runs on (LDPlayer or a phone), captures each piece's panel, reads it and writes a JSON
the azhor lab imports. Distributed as an APK on GitHub, not on the Play Store.

## Modules

- `scanner` — pure Kotlin, no Android import. Holds the reader (OCR lines to a piece, matched
  against the catalogue) and the walk (tap, wait, capture, read, scroll, register the row), written
  over interfaces for the screen, the touch and the text recogniser. Tested on the JVM with
  recorded frames.
- `app` — Android. Implements those interfaces with MediaProjection (frames), an
  `AccessibilityService` using `dispatchGesture` (taps and swipes, blind: the game is Unity and
  exposes no view tree), ML Kit (text) and the file writer; holds the foreground service that runs
  the walk, the floating button drawn over the game, and the two screens.
- The catalogue (sets, slots, attribute names, variants, factions) is `scanner`'s own, transcribed
  from the wiki's Gear page. The app depends on no other repository: what it shares with the azhor
  lab is the JSON it writes, not code. A word the catalogue lacks reads as `null` beside the raw
  lines, and the lab's picker settles it. The wiki's Gear page is behind Cloudflare; the
  transcription was made from azhor-wor's verbatim copy, `docs/gear.md`. If the two transcriptions
  drift, the option on the table is a third project, a library both read; not now.
- The `Exclusive` line's name is written as read; who that hero or faction is belongs to the lab.
- Named after what it is, never where it sits: `scanner`, not `core:domain`.

## State

- Unidirectional per screen: one `StateFlow` of a single state, user intents in.
- The walk runs in a foreground service, because the Activity is gone once the game is in front.
  The service exposes progress as a flow; the overlay and the screens only render it.

## Dependency injection

- Koin. Lifecycles: platform adapters and repositories `single`, use cases `factory`, one
  ViewModel per screen.
- A use case exists only when there is real logic; a pass-through call does not create a layer.
- The owner of heavy work owns its threading; presentation never shifts threads defensively.

## Persistence

- No database. A scan is a JSON file in the app's external files directory; a new scan is a new
  file, never a merge. The user takes it out through the share sheet or saves it to Downloads via
  MediaStore. Sending straight to the lab is a later option.
- Every entry in the JSON carries the raw OCR lines it was read from.
- The panel PNG is kept only for a piece the reader did not close: set or slot null, or the card
  refused. A full run keeps no other image.
- A scan leaves an emulator by the folder it shares with the PC: LDPlayer mounts `/mnt/shared/Pictures`
  inside Android and shows it under the Windows Documents folder, so "Save to Pictures" writes there.
  The clipboard does not cross that border, and the two apps are not linked over the network.
- Preferences in DataStore.
- Room enters only if scan history inside the app is ever wanted, and brings the no-destructive-
  migration rule with it.

## Navigation

- One Activity, Compose, Navigation 3 from the start: the back stack is a state list the app owns,
  which lets the service push the follow-up screen when a scan ends.
- The overlay belongs to the service, not the Activity, and is drawn in Compose too.

## Error handling

- `resultOf { }` in `scanner`, never `runCatching`: a `try` that returns `Result` but rethrows
  `CancellationException`, with `TimeoutCancellationException` counted as a failure. `runCatching`
  swallows cancellation, which in a minutes-long loop shows up as a stop button that sometimes does
  not stop.
- `resultOf` wraps the calls that throw: a capture, an OCR pass, a gesture.
- The end of a walk is a sealed type of its own, not a `Result`: finished, stopped for a named
  reason (storage closed, the grid no longer matches, projection consent lost), failed with cause.
  The UI renders each with its own verb.
- A piece that reads badly is not a walk error: it lands in the JSON with nulls and its PNG, and
  the walk goes on. Only what prevents the next tap stops a scan.

## What the storage screen fixes

- The piece's card is the right-hand panel, redrawn on tap, not a popup: the loop is tap, wait,
  capture the panel, next, with no back.
- The grid scrolls continuously, seven per row, under a scrollbar. A swipe does not move an exact
  number of rows: the walk drags slowly (no fling), then finds where the last row it had seen now
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
- Interview the user to define how a scan leaves the other emulators, BlueStacks first: LDPlayer's
  shared `/mnt/shared/Pictures` is the only door wired today.
