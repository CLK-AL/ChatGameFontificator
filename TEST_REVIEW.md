# Test Review: ChatGameFontificator

## Inventory

| Type | Present? | Notes |
| --- | --- | --- |
| Unit tests       | **No** | — |
| Integration tests | No | — |
| UI tests          | No | — |
| API/contract tests | No | — |
| End-to-end tests  | No | — |
| Load/perf tests   | No | — |

There are **no tests of any kind** in the repository. There is no
`src/test` directory, no test dependency in `pom.xml`, no test task in
the build, and no CI configuration. Every change ships based on manual
verification.

## Why this matters here

Unlike the other two repos, ChatGameFontificator carries several
categories of logic that are highly testable without a display:

- **Config parsing and validation** — `ConfigFont`, `ConfigMessage`,
  `Config.baseValidation` (already a known source of bugs; commit
  `83155b4` fixed a `trim()`-related regression that a single unit
  test would have caught).
- **Sprite geometry** — `calculateFixedCharacterDimensions`,
  `calculateVariableCharacterDimensions`, `getCharacterBounds`,
  pixel-advance math.
- **Character-key / codepoint logic** — `SpriteCharacterKey`,
  unknown-character fall-through, duplicate-character handling
  (finding 5 in `CODE_REVIEW.md`).
- **Color caching** — `Sprite.getColoredImage` / `coloredImgs`.

These are all pure enough to test in-process. Only the final draw call
to Swing's `Graphics` needs a real display; the rest can be covered
with JUnit + `BufferedImage` off-screen.

The UI layer (Swing panels, Twitch IRC event handling, video overlay)
genuinely is hard to test, but that is a fraction of the codebase.

## Findings

All findings here are about **gaps**, since no tests exist to review.

### Critical

**T1. Zero automated coverage for logic changed in recent bug-fix commits.**

Commit `83155b4` ("Bug fix: trim() was incorrectly invalidating configs
when they had spaces as unknown characters or user message dividers")
fixed a real user-visible bug in `Config.baseValidation`. Without a
test pinning the fix, the same class of bug can (and, per
`CODE_REVIEW.md` finding 9, still does) recur in adjacent code paths.

*Fix:* Add a `ConfigValidationTest` that exercises:

- unknown-character key = `" "` (single space) — must validate.
- message-divider = `" "` — must validate.
- empty string — must fail validation.
- whitespace-only multi-char — must fail validation.

**T2. Critical bugs in `CODE_REVIEW.md` are all unit-testable but
untested.**

The three critical findings in `CODE_REVIEW.md`
(`setBaselineOffset` writing through `getProperty`, the duplicated
`w > 0 && w > 0` height check, unchecked `getCharacterBounds` nulls)
are all trivially testable without UI. Each is one small test.

*Fix:* Add a test class per file under review — `ConfigFontTest`,
`SpriteCharacterKeyTest`, `SpriteFontTest` — and seed them with tests
that fail today and pass once the bugs are fixed.

### Major

**T3. No sprite-geometry tests.**

`SpriteFont.calculateVariableCharacterDimensions` and
`calculateFixedCharacterDimensions` are pure pixel-processing routines
over a `BufferedImage`. They can be tested in-process by constructing
a small `BufferedImage` and asserting the resulting `CharacterBounds`
map. This would pin down the `indexOf(c)` bug (finding 5) and the
zero-width glyph issue (finding 8).

*Fix:* Build tiny synthetic sprite sheets in test setup
(`new BufferedImage(w, h, TYPE_INT_ARGB)`), drive them through
`SpriteFont`, and assert on `characterBounds`.

**T4. Config round-trip is untested.**

ChatGameFontificator reads and writes `.properties` config files for
every component (`ConfigFont`, `ConfigMessage`, etc.). A simple
"load → save → reload" round-trip test would catch the
`setBaselineOffset` bug (critical #1 in `CODE_REVIEW.md`) and any
future copy-paste error in the setter family.

*Fix:* Add a `ConfigRoundTripTest` per `Config*` subclass, parameterised
over each setter: load a known-good properties file, mutate one field
through the setter, save, reload, assert the field survived.

**T5. No CI / test goal.**

`pom.xml` has no `surefire` or `failsafe` configuration and no test
dependency (JUnit/TestNG). Even if someone writes a test today, the
build does not execute it.

*Fix:* Add JUnit 5 (`org.junit.jupiter:junit-jupiter` test scope) and
rely on surefire's default conventions. Add a GitHub Actions workflow
running `mvn -B test` on push to the default branch.

**T6. UI-adjacent logic has no headless harness.**

The `SpriteFont.drawCharacter` path mixes pure math
(`characterBounds`, offsets) with actual `Graphics` calls. This makes
it harder than necessary to test because the math isn't separable from
the draw call.

*Fix:* Refactor the pure-math portion into a package-private
`computeDrawBounds(sck, ...)` method returning a value object, and
unit-test that method. Leave `drawCharacter` as a thin adapter around
it. Low-cost refactor, high-leverage for testability.

### Minor

**T7. No snapshot rendering test.**

A rendered chat frame is a `BufferedImage`; equality/hash comparison
against a known-good PNG is straightforward. A single "render one
message in the default font and compare image hash" test would serve
as a cheap regression net for many of the major findings in
`CODE_REVIEW.md` without requiring a display.

*Fix:* Add an optional `ChatPanelRenderSnapshotTest` guarded by
`GraphicsEnvironment.isHeadless()` detection — skip if headless, else
render, compute SHA-256 of pixel bytes, compare against committed
fixture.

**T8. No tests for the color cache growth (`Sprite.coloredImgs`).**

Memory growth under a long-running chat session (finding 12 in
`CODE_REVIEW.md`) is reproducible in a test: invoke
`getColoredImage` with N unique colours, assert the cache remains
bounded.

*Fix:* After adding an eviction policy (per the code review), pin it
with a test that fails when the cache grows unbounded.

### Nit

**T9. No linting / static analysis configured.**

Separate from tests but adjacent: no Checkstyle, SpotBugs, or
ErrorProne configuration. Several findings in `CODE_REVIEW.md`
(bitwise `&` on booleans, duplicated condition, getProperty-vs-setProperty)
are the sort of thing SpotBugs would flag on every build.

*Fix:* Add SpotBugs with a lightweight exclude file; the initial
findings should include most of the `CODE_REVIEW.md` list.

---

## Recommendations (priority order)

1. **Add JUnit 5 and a minimal `src/test/java` tree.** Zero-cost
   foundation for everything else.
2. **Write regression tests for every critical finding in
   `CODE_REVIEW.md`** — three small tests that currently fail and pass
   once the bugs are patched. These become permanent fences.
3. **Add config round-trip tests for every `Config*` subclass.** This
   is the single highest-leverage suite given the history of config
   bugs in this repo.
4. **Refactor `SpriteFont.drawCharacter` to separate pure math from
   `Graphics` calls**, then test the math.
5. **Add SpotBugs to the build**; it catches the class of bug the code
   review surfaces at near-zero effort.
6. **Add a headless snapshot test** gated on
   `!GraphicsEnvironment.isHeadless()` for CI, as a blunt-but-useful
   render regression net.

## Summary

| Severity | Count |
| --- | --- |
| Critical | 2 (no coverage for recent bug fix, no coverage for critical findings) |
| Major    | 4 (geometry/config/UI-adjacent untestability, no CI) |
| Minor    | 2 |
| Nit      | 1 |

This repository stands to benefit the most from adding tests: it has
real user-visible bugs documented in its commit history
(`83155b4`) and in `CODE_REVIEW.md`, and the bulk of its logic is
pure and eminently testable once a JUnit harness is in place.
