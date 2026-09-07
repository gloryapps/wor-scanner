# wor-scanner

A native Android app that scans a Watcher of Realms storage on the device it runs on, reads each
tile off its panel and writes a JSON the azhor lab imports. Gear today, heroes and artifacts as
kinds to come. Shipped as an APK on GitHub.

| Area | Read before working there |
| --- | --- |
| Architecture & code layout | `bootstrap/architecture.md` |
| Design system (deferred) | `bootstrap/design-system.md` |
| CI & release (deferred) | `bootstrap/ci.md` |

## Rules

- `scanner` imports nothing from Android. `app` implements its interfaces.
- A kind is a sub-package of `scanner`'s `kinds/` behind one `Scannable` object; its reader is its own model,
  written with `text/`'s instruments. The scan names no kind: `Kind.scannable()` and
  `Kind.label()` are the only two places that do. Adding one is the checklist in
  `bootstrap/architecture.md`.
- A kind's words are transcribed from the wiki, spelled as the page spells them. No code or data is shared with the azhor lab: the JSON the app writes is the whole contract.
- Use `resultOf { }`, never `runCatching`: it swallows `CancellationException`.
- A piece's identity is its grid position, never its content.
- Positions are fractions of the display, never pixels.
- A comment is one line, and only what the code cannot say itself.
- Nothing is written twice: what a second caller would repeat is extracted before that caller is
  written.

## Commits

Concise, in English. One change is a single-line title; several changes are a title plus a few
one-line factual bullets. No prose bodies.

## Tests

Names are plain-English sentences in domain language. Test doubles are written inline, no mocking
framework. Nested grouping instead of comment separators.
