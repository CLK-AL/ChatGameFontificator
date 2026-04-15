# Migration Plan: ChatGameFontificator → Kotlin / KMP / Gradle

Execution companion to [`CODE_REVIEW.md`](CODE_REVIEW.md) and
[`TEST_REVIEW.md`](TEST_REVIEW.md). Converts every review finding into
a **failing** Kotlin test (TDD), drives the repository to 100 %
Kotlin coverage on JVM, then migrates production code to
**Kotlin Multiplatform `commonMain`**. The existing Java Swing app
stays buildable (legacy reference); a new Compose Desktop host plus a
Compose HTML / wasmJs web UI are introduced, and CI/CD runs in dual
`java-legacy` / `kmp` mode.

ChatGameFontificator is the highest-leverage of the three repos: no
tests exist today, recent commits (`83155b4`) document a class of
whitespace-handling config bug, and `CODE_REVIEW.md` lists three
Critical + five Major bugs all reproducible without a display.

---

## 0. Guiding principles

1. **Legacy Java is frozen.** `src/main/java/com/glitchcog/fontificator/**`
   stays untouched. It ships on the legacy release channel via the
   existing `pom.xml`.
2. **Strict TDD.** Every `CODE_REVIEW.md` finding is encoded as a
   failing Kotlin test *before* any Kotlin code ships. Each failing
   test is authored against a thin JVM delegate over the frozen Java
   classes, goes green only after the Kotlin re-implementation lands.
3. **100 % Kotlin coverage before KMP migration.** A subsystem moves
   from `jvmMain` (Java-delegating) to `commonMain` (pure KMP) only
   when Kover reports 100 % line + branch coverage and mutation
   coverage ≥ 85 %.
4. **Dual UI hosts.** The existing Swing chat window is preserved
   (adapted to Kotlin view-models) as the JVM UI; a new
   Compose Desktop host plus a Compose HTML / wasmJs host drive
   the same `commonMain` render pipeline.
5. **Dual CI/CD.** Two GitHub Actions workflows per PR:
   `java-legacy` (Maven, JDK 8) and `kmp` (Gradle, JDK 21, KMP
   matrix).

---

## 1. Repository layout after migration

```
ChatGameFontificator/
├── src/main/java/com/glitchcog/fontificator/**     ← legacy Java (frozen)
├── src/main/resources/**                            ← shared assets
├── pom.xml                                           ← legacy Maven build
├── settings.gradle.kts
├── build.gradle.kts
├── kmp/
│   ├── core/                                         ← render + config + sprite logic
│   │   ├── src/commonMain/kotlin/…
│   │   ├── src/commonTest/kotlin/…
│   │   ├── src/jvmMain/kotlin/…                      ← Java-delegate adapters + Swing interop
│   │   ├── src/jvmTest/kotlin/…
│   │   ├── src/jsMain/kotlin/…                       ← Canvas + Fetch adapters
│   │   ├── src/wasmJsMain/kotlin/…
│   │   └── src/nativeMain/kotlin/…
│   ├── chat/                                         ← Twitch IRC client (KMP) with expect/actual
│   ├── ui-swing/                                     ← adapted legacy Swing UI (view-models in KT)
│   ├── ui-compose-desktop/                           ← new Compose Desktop host
│   ├── ui-compose-html/                              ← new Compose for Web host
│   └── ui-shared/                                    ← shared Compose components
├── testdata/                                         ← sprite sheets + configs + sample chats
├── .github/workflows/
│   ├── java-legacy.yml
│   └── kmp.yml
```

Maven is authoritative for the legacy Swing build; Gradle owns KMP.

---

## 2. Toolchain

| Concern | Choice |
| --- | --- |
| Build          | Gradle 8.x Kotlin DSL, `gradle/libs.versions.toml` |
| Kotlin         | 2.x K2, multiplatform plugin (JVM / JS / wasmJs / Native) |
| Test framework | `kotlin.test` (common), JUnit 5 Jupiter (JVM), Kotest property |
| Coverage       | Kover 0.8.x, `minBound = 100` on line + branch for `commonMain` |
| Mutation       | Pitest on JVM, ≥ 85 % |
| Property tests | Kotest `property` for config + sprite geometry |
| Snapshot       | `compose-snapshot-testing` for Compose components; `javax.imageio` diff for `BufferedImage` |
| Static         | Detekt + ktlint; SpotBugs retained for legacy Java only |
| Fuzz           | Jazzer on `Config.baseValidation` + properties loader |
| UI (Desktop)   | Compose Multiplatform Desktop |
| UI (Web)       | Compose for Web wasmJs |
| UI (Legacy)    | Swing (retained, driven by Kotlin view-models) |
| Chat IRC       | Ktor client + OkHttp engine on JVM, Ktor JS engine on browser |
| Load / perf    | `kotlinx-benchmark` + JMH |
| E2E            | Playwright Kotlin for web, Compose Desktop UI test for desktop |

---

## 3. Test strategy — every level

| Level | Source set | Runner | Scope |
| --- | --- | --- | --- |
| Unit | `commonTest` / `jvmTest` | `kotlin.test` | Config parsing, `SpriteCharacterKey`, `SpriteFont` geometry |
| Integration | `jvmTest` | JUnit 5 | `.properties` round-trip, sprite-sheet → `CharacterBounds` map |
| UI (Swing) | `ui-swing:jvmTest` | AssertJ Swing | Legacy `ChatWindow` renders a canned message |
| UI (Desktop) | `ui-compose-desktop:jvmTest` | Compose UI test | Compose host renders canned chat, reacts to config change |
| UI (Web) | `ui-compose-html:wasmJsTest` | Compose Web test renderer | DOM snapshot of canned chat |
| API (contract) | `jvmTest` / `jsTest` | JUnit 5 | Public `ConfigFont`, `SpriteFont`, renderer surfaces |
| E2E (Web) | `e2e-web` | Playwright Kotlin | Drives published static site |
| E2E (Desktop) | `e2e-desktop` | Compose UI test + Robot | Drives the Compose Desktop app against a mocked Twitch feed |
| Load / perf | `benchmarks` | `kotlinx-benchmark` | `draw` throughput at 60 Hz simulation |
| Fuzz | `jvmTest` | Jazzer | Random `.properties` into `Config.baseValidation` |

---

## 4. TDD test plan — one failing test per `CODE_REVIEW.md` finding

Every test is first authored **red** against `…JavaAdapter` delegates
calling the legacy Java classes. The red test proves the bug
reproduces. The same test goes green once the pure-Kotlin
`commonMain` implementation lands.

### 4.1 Critical (red first, then green after Kotlin fix)

| Finding | Failing Kotlin test |
| --- | --- |
| 1. `setBaselineOffset` uses `getProperty` | `ConfigFontRoundTripTest.`​`set_baseline_offset_persists_to_properties()` |
| 2. `w > 0 && w > 0` instead of `h > 0` | `ConfigFontValidationTest.`​`invalid_grid_height_fails_validation()` |
| 3. `getCharacterBounds` returns `null` but callers deref | `SpriteFontBoundsTest.`​`unknown_codepoint_returns_fallback_not_NPE()` |

### 4.2 Major

| Finding | Failing Kotlin test |
| --- | --- |
| 4. Bitwise `&` in `isBadge` | `SpriteCharacterKeyTest.`​`isBadge_uses_short_circuit_evaluation()` (equality test vs reference) |
| 5. `indexOf(c)` loses duplicate characters | `SpriteFontGeometryTest.`​`duplicate_character_in_key_resolves_to_correct_position()` |
| 6. `Graphics` leak in `Sprite.setImage` | `SpriteResourceTest.`​`setImage_does_not_leak_graphics_instances()` (JVM-only, uses `Toolkit` leak probe) |
| 7. `letterIndex` overrun | `SpriteFontGeometryTest.`​`mismatched_grid_dimensions_report_error_not_SIOOBE()` |
| 8. Variable-width glyph zero width | `SpriteFontGeometryTest.`​`empty_variable_width_glyph_keeps_min_advance_1()` |

### 4.3 Minor + Nit

- `ConfigBaseValidationTest.single_space_value_accepted_when_key_allows_it()`
  (regression pin for the `83155b4` commit).
- `SpriteColorCacheTest.colored_cache_bounded_to_N_entries()`.
- `SpriteFontGeometryTest.character_bounds_cache_returns_same_instance_for_common_ascii()`.

### 4.4 Test sequencing

For each subsystem `S` (ConfigFont, ConfigMessage,
`Config.baseValidation`, SpriteFont, SpriteCharacterKey, Sprite):

1. **JVM delegate** in `jvmMain`: `class ${S}JavaAdapter` wrapping
   the legacy class.
2. **Red tests** from §4.1–4.3 authored against the adapter — CI
   must show them red.
3. **Pinning tests** for current correct behaviour (mostly around
   `calculateFixedCharacterDimensions`, `drawCharacter` output
   images).
4. **`commonMain` implementation** of `S`.
5. **Differential image tests:** for a fixture sprite sheet +
   fixture message, render a `BufferedImage` (JVM) / `ImageBitmap`
   (common) with both paths, compare via SHA-256 of ARGB pixel
   bytes.
6. Kover 100 % gate.
7. Delete JVM delegate for `S`.

### 4.5 Test corpus

- `testdata/configs/good/*.properties` — a handful of the shipped
  presets (one fixed-width font, one variable-width font,
  one emoji-bearing config).
- `testdata/configs/bad/*.properties` — missing-key, blank-value,
  space-as-divider (the `83155b4` regression fixture),
  grid-mismatch, oversized grid.
- `testdata/sprites/**` — 3 small sprite sheets (8×8, variable
  width, duplicate character in key).
- `testdata/chats/*.jsonl` — canned IRC messages with emoji, emotes,
  badges, zero-width joiners.
- `testdata/snapshots/**` — expected ARGB hashes + PNG renders for
  diff debugging.

---

## 5. 100 % Kotlin coverage gate

- `koverVerify { rule { bound { minValue = 100; metric = LINE };
   bound { minValue = 100; metric = BRANCH } } }` on every
  `commonMain` and `*Main` source set except UI.
- UI modules gated at ≥ 90 % line coverage (UI-testable code only;
  Swing-adapter glue excluded).
- Pitest ≥ 85 % mutation on `commonMain` + `jvmMain`.
- No coverage exceptions in Kotlin; where coverage is impossible
  (platform-native code), split into platform source set with its
  own 100 % gate or explicit `@Expect`/`@Actual` contract test.

---

## 6. Migration phases

### Phase A — scaffolding (1 PR)

- Land Gradle KMP root alongside `pom.xml`.
- Version catalog + both CI workflows.
- Gate: both lanes green on empty Kotlin module.

### Phase B — Legacy-delegate + first tests (1 PR)

- `jvmMain` adapters over every Config / Sprite class.
- Commit minimal `testdata/`.
- Pin current behaviour with Kotlin tests (no assertions on the
  critical-bug paths yet).
- Add JUnit 5 via Gradle so `surefire` isn't needed.
- Gate: all pinning tests green on Java delegate.

### Phase C — TDD fixes, one subsystem per PR

Priority order reflecting user impact:

1. `Config.baseValidation` (regression pin for `83155b4`, and red
   tests for the whitespace class of bug).
2. `ConfigFont` (critical #1 and #2 — setter persistence, height
   validation).
3. `SpriteCharacterKey` (major #4 — bitwise vs logical).
4. `SpriteFont` geometry (critical #3, majors #5 #7 #8).
5. `Sprite` (major #6 — graphics leak; plus cache eviction).

Each PR:

a. Adds **red** negative tests in `commonTest` / `jvmTest`.
b. Adds `commonMain` Kotlin replacement.
c. Turns the tests green; differential image suite must match legacy
   output except for the specific fixed bugs.
d. Raises Kover gate on the subsystem to 100 %.
e. Deletes the JVM delegate.

### Phase D — UI migration (three tracks, in parallel once Phase C
stabilises)

- **`ui-swing/`** — extract view-models from the Swing event handlers
  into Kotlin. Swing window stays; it now observes a
  `StateFlow<ChatViewState>` driven by `commonMain`.
- **`ui-compose-desktop/`** — new host built with Compose
  Multiplatform Desktop; reuses `ui-shared/` Compose components
  (`MessageRow`, `SpriteCharacter`, `BadgeIcon`).
- **`ui-compose-html/`** — Compose for Web wasmJs host serving the
  same `ui-shared/` composables.
- **UI tests**:
  - Swing: AssertJ-Swing asserts window contains the rendered text.
  - Desktop: Compose UI test asserts a rendered `MessageRow` DOM
    tree + ARGB snapshot of `ChatCanvas`.
  - Web: Compose Web test renderer asserts the DOM tree; Playwright
    Kotlin drives the deployed site against a mocked Twitch
    WebSocket.

### Phase E — E2E + load/perf

- `e2e-desktop` runs the Compose Desktop host against a fake Twitch
  IRC server emitting canned messages; asserts final canvas pixel
  hash.
- `e2e-web` runs Playwright Kotlin against the deployed
  `ui-compose-html` site.
- `benchmarks/` JMH gates for 60 Hz render throughput across
  1 / 10 / 100 concurrent messages; baseline committed; ±10 %
  regression threshold.

### Phase F — Dual CI/CD + release

- **`java-legacy`** workflow: `mvn -B verify`; publishes the classic
  Swing JAR (retained, installable). No Java sources modified since
  Phase A.
- **`kmp`** workflow: `./gradlew build`; publishes:
  - `chatgamefontificator-core` klibs (JVM / JS / wasmJs / Native).
  - `chatgamefontificator-desktop` signed bundle
    (Compose Desktop DMG / MSI / AppImage).
  - `chatgamefontificator-web` static site (GitHub Pages).
- Tag-driven release runs both lanes; legacy channel has
  `-legacy` suffix.

---

## 7. Acceptance criteria

1. Every `CODE_REVIEW.md` finding has a **named** Kotlin test whose
   Git history shows it merged **red**, then **green** after the
   Kotlin replacement (`git log --follow` on the test file).
2. `./gradlew koverVerify` reports 100 % line + branch on every
   non-UI Kotlin module.
3. `./gradlew pitestReport` ≥ 85 % on `commonMain` + `jvmMain`.
4. Differential image suite: byte-exact ARGB parity with legacy
   Swing renderer for the committed corpus (minus the specific
   fixed-bug divergences).
5. The whitespace-config bug-class from commit `83155b4` is covered
   by at least three distinct tests
   (`unknown_char = " "`, `divider = " "`, `empty_string_rejected`).
6. `java-legacy` lane green on every PR; no file under
   `src/main/java/**` modified since Phase A.
7. `kmp` lane green on {ubuntu, macos, windows} × {jvm, js, wasmJs,
   native}; Compose Web site builds and deploys to GitHub Pages.

---

## 8. Deliverables

- Gradle KMP root + version catalog + `libs.versions.toml`.
- `kmp/core/` with `commonMain/Test`, `jvmMain/Test`, `jsMain/Test`,
  `wasmJsMain/Test`, `nativeMain/Test`.
- `kmp/chat/` KMP Twitch IRC client with JVM / JS engines.
- `kmp/ui-swing/` — Kotlin view-models over the retained Swing UI.
- `kmp/ui-compose-desktop/`, `kmp/ui-compose-html/`,
  `kmp/ui-shared/`.
- `testdata/` corpus (configs, sprites, chats, snapshot hashes).
- `.github/workflows/java-legacy.yml`, `kmp.yml`.
- `TEST_PLAN.md` auto-generated mapping `CODE_REVIEW.md` finding →
  test FQN.
- `benchmarks/baselines/*.json`.
