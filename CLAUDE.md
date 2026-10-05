# wor-scanner

Scans a Watcher of Realms storage, reads each tile off its panel and writes a JSON the azhor lab
imports: gear, legendary and epic heroes, and artifacts. An Android app scans the game on the device
it runs on; a Windows app scans the game's own PC client beside it. Shipped as an APK and a Windows
zip on GitHub.

| Area | Read before working there |
| --- | --- |
| Architecture & code layout | `bootstrap/architecture.md` |
| Design system | `bootstrap/design-system.md` |
| CI & release | `bootstrap/ci.md` |

## Rules

- `scanner` imports nothing from Android. `app` implements its interfaces.
- A kind is a sub-package of `scanner`'s `kinds/` with one `Scan` object: its layout, its record's
  serializer, what it reads off a tile, and its own model of the panel, written with `text/`'s
  instruments. The walk over the grid names no kind: `Kind.scan()` in `scanner` and `Kind.named` in the
  `ui` module's `Kinds.kt` are the only two places that do. Adding one is the checklist in `bootstrap/architecture.md`.
- A kind's words are transcribed from the wiki, spelled as the page spells them. No code or data is shared with the azhor lab: the JSON the app writes and the lab's two addresses it is sent to, `/scanner/link` and `/scanner/scans`, are the whole contract.
- Use `resultOf { }`, never `runCatching`: it swallows `CancellationException`.
- A piece's identity is its grid position, never its content.
- Positions are fractions of the display, never pixels.
- A comment is one line, and only what the code cannot say itself.
- Nothing is written twice: what a second caller would repeat is extracted before that caller is
  written.

## Commits

Concise, in English. One change is a single-line title; several changes are a title plus a few
one-line factual bullets. No prose bodies.

A change a player notices adds its line to `CHANGELOG.md` under `## [Unreleased]`, in Keep a
Changelog's sections: those lines are the next release's notes (`bootstrap/ci.md`).

New work is on a new branch off `dev`; nothing is worked on or committed on `dev` itself. A branch
lands on `dev` squashed. `dev` reaches `main` only by a pull request merged with a merge commit, and
that merge is a release (`bootstrap/ci.md`).

## Tests

Names are plain-English sentences in domain language. Test doubles are written inline, no mocking
framework. Nested grouping instead of comment separators.
