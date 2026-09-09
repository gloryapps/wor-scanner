# CI & release

Deferred 2026-09-06: the functionality comes first. Until settled, an APK is built locally and
attached to a GitHub release by hand.

The release build is optimized by R8 (`optimization { enable = true }` in `app/build.gradle.kts`),
so its stack traces are obfuscated: keep `app/build/outputs/mapping/release/mapping.txt` next to
each APK released and retrace crashes with it. Keep rules live in `app/src/main/keepRules/*.keep`.

## Open decisions

- Interview the user to define where CI runs and what it verifies.
- Interview the user to define the release flow: trigger, version allocation, tag, APK upload to
  the GitHub release.
