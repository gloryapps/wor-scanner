# wor-scanner

A native Android app that walks the Watcher of Realms gear storage on the device it runs on,
reads each piece off its panel and writes a JSON the azhor lab imports. Shipped as an APK on
GitHub.

| Area | Read before working there |
| --- | --- |
| Architecture & code layout | `bootstrap/architecture.md` |
| Design system (deferred) | `bootstrap/design-system.md` |
| CI & release (deferred) | `bootstrap/ci.md` |

## Rules

- `scanner` imports nothing from Android. `app` implements its interfaces.
- The catalogue's words arrive as a generated JSON asset from the azhor-wor monorepo; never type
  a set, slot or attribute name by hand.
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
