# Design system

Settled 2026-09-07 from the `Scanner App` and `Scanner Overlay C` boards of the WoR Lab design
project. One theme, dark: the app and the strip drawn over the game share it, so looking away from
one and back at the other reads as one thing. There is no light variant.

Values live in `ui/Colors.kt` and `ui/Lettering.kt`, one role per name. `ui/Theme.kt`'s `ScannerTheme`
is the same two objects as Material reads them, for the components that ask Material rather than the
roles. A screen names a role, never a value and never a size.

## Colour roles

| Role | Value | Where |
| --- | --- | --- |
| `screen` | `#0B0D12` | the screen itself |
| `raised` | `#14171F` | a card on the screen |
| `sunken` | `#0E1117` | a JSON block, an input, a segmented track |
| `lifted` | `#242A36` | the chosen half of a segmented control |
| `accent` | `#9EC5FF` | the one accent: a primary button, a link, progress, a mark |
| `onAccent` | `#0B0D12` | text on the accent |
| `accentWash` | `#9EC5FF` 8% | the ground of a selected row, or of a card that leads |
| `accentEdge` | `#9EC5FF` 28% | the border of the same |
| `text` / `muted` / `faint` | `#E9EDF5` at 100 / 62 / 45% | read, said beside it, found when looked for |
| `warning` | `#E8B34A` | a grant not given, a piece read badly |
| `failure` | `#E08585` | a scan that failed |
| `failureEdge` | `#E08585` 40% | the border of a send that failed |
| `hairline` / `edge` | white 9% / 14% | a rule between rows, a border on a control |
| `glass` / `glassSolid` / `glassThin` | `#0C0E14` at 86 / 92 / 72% | the capsule over the game; its menu; the close target |
| `glassEdge` / `glassEdgeStrong` | white 13% / 22% | the capsule's edge; the stop's and the close target's |
| `glassAccentEdge` / `glassWash` / `accentGlow` | `#9EC5FF` at 40 / 14 / 70% | the idle capsule's edge; the row under way and the ring over the target; under the hairline |
| `Colors.Json` key/string/number/punctuation | accent / `#B7C7A8` / warning / faint | the reading screen's JSON |

## Type roles

Two voices in `Fonts`, both the device's own: `sans` talks, `mono` shows what the app read or wrote.
No font file ships with the app.

| Role | Family, size | Where |
| --- | --- | --- |
| `title` | sans 19 semibold | what a card or a screen is called |
| `subtitle` | sans 16 medium | a reading's tile, a sheet's heading |
| `display` | sans 22 semibold | a first-run screen's heading |
| `stepName` | sans 13 medium, in lines | a step's name, a grant's, what the site answered |
| `brand` | sans 15 medium | the app's name in the header |
| `section` | sans 10 medium, tracked, upper-cased by `Section` | the line above a group |
| `body` | sans 13 | prose |
| `caption` | sans 11 | what is said under a line of prose |
| `footnote` | sans 10 | what is said under an action that fills its card |
| `action` / `actionSmall` | sans 13 / 12 medium | a button; a grant's, a strip's |
| `data` / `dataSmall` | mono 12 / 10 | a stamp, a count, an id, a file name |
| `count` / `code` / `numeral` | mono 44 / 18 tracked / 10, medium | how many a scan read; the link code; a step's number |
| `label` / `mark` | mono 11 / mono 9 tracked | a row of the menu over the game; a small word in it |
| `word` | mono 10 tracked | what a tap on the idle capsule does: `SCAN GEAR` |

## Shapes

6, 8, 10, 12, 14 dp, in Material's five slots from `extraSmall`; a pill is fully rounded. The capsule
over the game and its close target are pills; a card is 10, a screen-sized panel 12, a sheet 12.
A filled action has a `Reach`: `SMALL` 30 dp tall in a strip over the screen, `REGULAR` 44 on it.

## Frame

The screens are drawn for LDPlayer's window: 1280x720 px at 240 dpi, 853x480 dp, the previews' wide
frame. A larger window only adds room; below 720 dp wide the columns stack and scroll.
