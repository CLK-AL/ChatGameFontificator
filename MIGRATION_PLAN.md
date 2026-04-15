# Migration Plan: ChatGameFontificator → Kotlin / KMP / Gradle

Execution companion to [`CODE_REVIEW.md`](CODE_REVIEW.md) and
[`TEST_REVIEW.md`](TEST_REVIEW.md). Drives every review finding through
a strict **fix-then-freeze-then-port** cycle:

1. Author Kotlin TDD tests that fail against today's Java code.
2. **Fix the Java code** until every test is green and JVM coverage
   reaches 100 % line + branch (stabilise).
3. **Freeze** the now-stable Java sources and tag them `legacy-v1`.
4. **Migrate** the frozen Java logic to pure Kotlin Multiplatform
   (`commonMain`), reusing the *same* Kotlin test suite — which now
   runs against both the frozen Java and the new Kotlin
   implementations as a differential parity gate.

Two CI/CD profiles (`java`, `kmp`) run on every PR throughout; both
apply ProGuard to release artefacts. See
[`INTEGRATION.md`](INTEGRATION.md) for how this repo's
sprite-font + chat-overlay logic is vendored into the
`fonts-bitsnpicas`-hosted font studio.

ChatGameFontificator is the highest-leverage of the three repos: no
tests exist today, recent commits (`83155b4`) document a class of
whitespace-handling config bug, and `CODE_REVIEW.md` lists three
Critical + five Major bugs all reproducible without a display.

---

## 0. Guiding principles

1. **TDD, strictly.** Every finding becomes a red Kotlin test *before*
   any production code changes.
2. **Java is stabilised first, then frozen.** Fix the Java code until
   the Kotlin tests pass and JaCoCo reports 100 %. Every fix commit
   cites its `CODE_REVIEW.md` finding ID. Only then does the Java
   tree become read-only.
3. **Shared test suite across Java and Kotlin.** Tests target a
   platform-neutral `ChatFontIo` + `SpriteRendererIo` interface with
   two `actual`s — a JVM adapter over frozen Java, and pure Kotlin in
   `commonMain`. Same `@Test` methods run against both.
4. **100 % coverage at every transition.** JaCoCo at freeze; Kover +
   Pitest before a subsystem migrates to pure `commonMain`.
5. **Dual CI/CD profiles.** Gradle profiles `-Pprofile=java` and
   `-Pprofile=kmp`. Every PR runs both. Both ProGuard-shrink
   release JARs and verify post-shrink.
6. **Skiko-first rendering.** JetBrains Skiko (the Skia layer that
   powers Compose Multiplatform) becomes the primary graphics
   engine. Today's `Graphics2D` calls are wrapped behind a
   platform-neutral `Canvas2D` that has a Swing-backed `actual` for
   the legacy profile and a Skiko-backed `actual` for the KMP
   profile. Skia already contains HarfBuzz + FreeType + SVG — so
   FontBox / FreeType / HarfBuzz4J become *optional* fallbacks
   rather than core dependencies.
7. **Reproducible toolchain via SDKMAN.** Contributors and CI consume
   the exact same JDK / Kotlin / Gradle / JBang versions from
   `.sdkmanrc`, pinned to the **latest stable** at migration time.

---

## 1. Toolchain — SDKMAN + Gradle version catalog

Every number below was verified against `sdk list` or Maven Central
at commit time. The whole table is driven by two files:
`.sdkmanrc` (JDK / Kotlin / Gradle / JBang) and
`gradle/libs.versions.toml` (everything else).

| Concern | Choice (verified latest stable) |
| --- | --- |
| JDK           | Oracle GraalVM **25.0.2-graal** — both profiles |
| Kotlin        | **2.3.20** — K2, multiplatform plugin |
| Gradle        | **9.4.1** — Kotlin DSL + version catalog |
| JBang         | **0.138.0** |
| Test          | `kotlin.test` + JUnit **5.12.2** + Kotest **5.9.1** |
| Coverage      | Kover **0.9.1** (KMP), JaCoCo (Java), 100 % line + branch |
| Mutation      | Pitest Gradle **1.15.0** / core **1.19.1**, ≥ 85 % |
| UI (Swing)    | Retained; Kotlin view-models behind legacy window |
| UI (Desktop)  | Compose Multiplatform **1.8.2** (Skiko-backed) |
| UI (Web)      | Compose for Web (wasmJs), Compose **1.8.2** |
| Graphics      | Skiko **0.9.18** (Skia layer with HarfBuzz + FreeType + SVG) |
| Chat IRC      | Ktor **3.2.0** client with JVM + JS engines |
| Static        | Detekt **1.23.8**, ktlint-gradle **12.3.0** |
| Fuzz          | Jazzer **0.24.0** |
| Bench         | `kotlinx-benchmark` runtime **0.4.14** + JMH |
| Shrink        | ProGuard Gradle **7.7.0** + `verifyProguardedJar` |
| Native        | `org.graalvm.buildtools.native` **0.10.6** |
| CI            | GitHub Actions `{ubuntu,macos,windows}-latest` via `sdk env` |


### 1.1 `.sdkmanrc`

```
# Pinned to the latest 2026 stable; bumped by a single renovate/dependabot PR.
java=25.0.2-graal            # Oracle GraalVM for JDK 25 LTS (SDKMAN `graal` distro; native-image + PGO for KMP native targets)
kotlin=2.3.20            # latest stable from `sdk list kotlin`
gradle=9.4.1             # latest stable from `sdk list gradle`
jbang=0.138.0            # latest stable from `sdk list jbang`
```

### 1.2 `gradle/libs.versions.toml`

Latest 2026 stable at adoption time; Renovate/Dependabot keep the
file fresh.

```toml
[versions]
# All versions below verified against Maven Central on the day of
# this commit. Renovate/Dependabot keep them fresh.
kotlin         = "2.3.20"               # matches `.sdkmanrc`
coroutines     = "1.10.2"
serialization  = "1.9.0"
ktor           = "3.2.0"
kover          = "0.9.1"
pitestGradle   = "1.15.0"
pitestCore     = "1.19.1"
kotest         = "5.9.1"                # 6.x is milestones
junit          = "5.12.2"               # 5.13.x is milestones
jazzer         = "0.24.0"
detekt         = "1.23.8"
ktlintGradle   = "12.3.0"
ktlintCore     = "1.6.0"
compose        = "1.8.2"                # pulls Skiko as a transitive; 1.9.x is alpha
skiko          = "0.9.18"               # also usable standalone on the java profile
benchmarks     = "0.4.14"
proguard       = "7.7.0"
graalvmPlugin  = "0.10.6"               # org.graalvm.buildtools.native plugin

[libraries]
kotlin-test        = { module = "org.jetbrains.kotlin:kotlin-test",          version.ref = "kotlin" }
skiko-jvm          = { module = "org.jetbrains.skiko:skiko-awt",             version.ref = "skiko" }
skiko-jvm-linux    = { module = "org.jetbrains.skiko:skiko-awt-runtime-linux-x64",   version.ref = "skiko" }
skiko-jvm-mac      = { module = "org.jetbrains.skiko:skiko-awt-runtime-macos-arm64", version.ref = "skiko" }
skiko-jvm-win      = { module = "org.jetbrains.skiko:skiko-awt-runtime-windows-x64", version.ref = "skiko" }
kotest-property    = { module = "io.kotest:kotest-property",                 version.ref = "kotest" }
junit-jupiter      = { module = "org.junit.jupiter:junit-jupiter",           version.ref = "junit" }
jazzer             = { module = "com.code-intelligence:jazzer-junit",        version.ref = "jazzer" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kover                = { id = "org.jetbrains.kotlinx.kover",        version.ref = "kover" }
pitest               = { id = "info.solidsoft.pitest",              version.ref = "pitest" }
detekt               = { id = "io.gitlab.arturbosch.detekt",        version.ref = "detekt" }
ktlint               = { id = "org.jlleitschuh.gradle.ktlint",      version.ref = "ktlint" }
compose              = { id = "org.jetbrains.compose",              version.ref = "compose" }
benchmarks           = { id = "org.jetbrains.kotlinx.benchmark",    version.ref = "benchmarks" }
proguard             = { id = "com.guardsquare.proguard",           version.ref = "proguard" }
graalvm-native       = { id = "org.graalvm.buildtools.native",      version.ref = "graalvm" }
```

### 1.3 JBang

JBang scripts under `testdata/gen/*.kt` generate synthetic sprite
sheets, canned chat logs, and malformed `.properties` fixtures.
`jbang testdata/gen/BadConfig.kt` — one command, no project setup.

---

## 2. Repository layout after migration

```
ChatGameFontificator/
├── .sdkmanrc
├── gradle/libs.versions.toml
├── settings.gradle.kts
├── build.gradle.kts
├── pom.xml                                      ← retained (legacy profile)
├── src/main/java/com/glitchcog/fontificator/**  ← Java (stabilised in B; frozen at C)
├── src/test/kotlin/com/glitchcog/fontificator/**← NEW: Kotlin tests reused by both profiles
├── kmp/
│   ├── core/        (Config, Sprite, SpriteFont, SpriteCharacterKey → commonMain)
│   ├── chat/        (Twitch IRC via Ktor; expect/actual engines)
│   ├── renderer/    (Canvas2D abstraction + Skiko + Swing actuals)
│   ├── ui-swing/    (legacy Swing host, Kotlin view-models)
│   ├── ui-compose-desktop/
│   ├── ui-compose-html/
│   └── ui-shared/
├── proguard/
│   ├── proguard-rules-common.pro
│   ├── proguard-rules-java.pro
│   └── proguard-rules-kmp.pro
├── testdata/
└── .github/workflows/
    ├── java.yml
    └── kmp.yml
```

---

## 3. Test strategy across every level

| Level | Source set | Runner | Profile(s) |
| --- | --- | --- | --- |
| Unit | `commonTest` / `jvmTest` | `kotlin.test` | java, kmp |
| Integration | `jvmTest` | JUnit 5 | java, kmp |
| UI (Swing legacy) | `ui-swing:jvmTest` | AssertJ-Swing | java, kmp |
| UI (Desktop) | `ui-compose-desktop:jvmTest` | Compose UI test | kmp |
| UI (Web) | `ui-compose-html:wasmJsTest` | Compose Web test | kmp |
| API / contract | `jvmTest` / `jsTest` | JUnit 5 / kotlin.test | kmp |
| E2E (Web) | `e2e-web` | Playwright Kotlin | kmp |
| E2E (Desktop) | `e2e-desktop` | Compose UI test + Robot | kmp |
| Load / perf | `benchmarks` | `kotlinx-benchmark` | java, kmp |
| Fuzz | `jvmTest` | Jazzer | java, kmp |

Under `profile=java`, tests drive the Swing/Graphics2D-backed
`Canvas2D` actual. Under `profile=kmp`, tests drive **both** —
Swing-backed (legacy parity) and Skiko-backed (new renderer) — with
ARGB-hash differential assertions.

---

## 4. TDD test plan — failing tests first, fix Java, then port

### 4.1 One failing Kotlin test per `CODE_REVIEW.md` finding

| Finding | Failing Kotlin test |
| --- | --- |
| C1. `setBaselineOffset` uses `getProperty` | `ConfigFontRoundTripTest.set_baseline_offset_persists_to_properties()` |
| C2. `w > 0 && w > 0` | `ConfigFontValidationTest.invalid_grid_height_fails_validation()` |
| C3. `getCharacterBounds` → null deref | `SpriteFontBoundsTest.unknown_codepoint_returns_fallback_not_NPE()` |
| M4. Bitwise `&` in `isBadge` | `SpriteCharacterKeyTest.isBadge_uses_short_circuit_evaluation()` |
| M5. `indexOf(c)` loses duplicates | `SpriteFontGeometryTest.duplicate_character_in_key_resolves_to_correct_position()` |
| M6. `Graphics` leak in `setImage` | `SpriteResourceTest.setImage_does_not_leak_graphics_instances()` *(jvmTest)* |
| M7. `letterIndex` overrun | `SpriteFontGeometryTest.mismatched_grid_dimensions_report_error_not_SIOOBE()` |
| M8. Zero-width variable glyph | `SpriteFontGeometryTest.empty_variable_width_glyph_keeps_min_advance_1()` |
| m9. Whitespace-in-config bug-class | `ConfigBaseValidationTest.single_space_value_accepted_when_key_allows_it()` — **pins commit `83155b4`** + two sibling cases (`unknown_char = " "`, `divider = " "`, `empty_rejected`). |
| m12. Color cache growth | `SpriteColorCacheTest.colored_cache_bounded_to_N_entries()` |

### 4.2 Strict authoring order

For each subsystem `S` (Config.baseValidation, ConfigFont,
SpriteCharacterKey, SpriteFont, Sprite):

1. **Red (Phase A).** Commit Kotlin tests against `…JavaAdapter`
   over the legacy class. CI shows red under `-Pprofile=java`.
2. **Fix Java to green (Phase B).** Edit
   `src/main/java/com/glitchcog/fontificator/**` one finding at a
   time; each commit cites the finding ID.
3. **Freeze (Phase C).** Tag `legacy-v1`, CODEOWNERS + CI guard.
4. **Port (Phase D).** Author `commonMain` Kotlin. Same Kotlin
   tests now run against both implementations.
5. **Differential ARGB parity gate.** For every `(sprite sheet,
   canned message)` fixture, render with both the legacy
   Swing/Graphics2D pipeline and the Skiko pipeline, compare
   SHA-256 of ARGB pixel bytes.
6. Retire the JavaAdapter from `jvmMain`; frozen Java stays.

### 4.3 Test corpus

- `testdata/configs/good/*.properties` — presets covering fixed &
  variable-width, emoji-bearing, space-as-divider.
- `testdata/configs/bad/*.properties` — the `83155b4` regression
  fixture plus missing-key, blank-value, grid-mismatch,
  oversized-grid.
- `testdata/sprites/**` — 8×8 fixed, variable-width, duplicate-char-
  in-key, empty-glyph.
- `testdata/chats/*.jsonl` — canned IRC messages with emoji,
  emotes, badges, ZWJ sequences.
- `testdata/snapshots/**` — expected ARGB SHA-256 hashes per
  (config, message) pair, plus the PNG itself for diff debugging.
- `testdata/gen/*.kt` — JBang generators.

---

## 5. 100 % coverage gates (both profiles)

- **Profile `java`**: JaCoCo `minimum = 1.0` on line + branch across
  `src/main/java/**`. Required to enter Phase C.
- **Profile `kmp`**: Kover `minBound = 100`; Pitest ≥ 85 %.
- UI source sets gated ≥ 90 % (adapter glue excluded).

---

## 6. Migration phases

### Phase A — Toolchain + red tests

- Commit `.sdkmanrc`, `gradle/libs.versions.toml`,
  `build.gradle.kts` with profiles `-Pprofile=java` / `-Pprofile=kmp`.
- `.github/workflows/{java,kmp}.yml`.
- Introduce `ChatFontIo`, `SpriteRendererIo`, `Canvas2D` interfaces
  + `JavaAdapter` + `SwingCanvas2D` actuals.
- Commit Kotlin TDD tests from §4.1 — **they fail on CI**.
- Gate: `profile=java` red on purpose; `profile=kmp` green on empty
  module.

### Phase B — Fix Java to green + 100 % JaCoCo

- Fix one finding per commit; commit subjects reference finding IDs.
- Expand corpus until JaCoCo ≥ 100 %.
- Gate: `profile=java` green, JaCoCo 100 %, all `CODE_REVIEW.md`
  findings closed.

### Phase C — Freeze Java

- Tag `legacy-v1`. CODEOWNERS + CI diff-check enforce read-only on
  `src/main/java/**`.

### Phase D — Port to `commonMain`

Priority:

1. `Config.baseValidation` (pin `83155b4` + whitespace-class bug).
2. `ConfigFont` (C1, C2).
3. `SpriteCharacterKey` (M4).
4. `SpriteFont` geometry (C3, M5, M7, M8).
5. `Sprite` (M6 + cache eviction).

Per subsystem: add `commonMain` Kotlin, wire `KotlinAdapter`, run
shared test suite against both. Differential ARGB parity must hold
on corpus.

### Phase E — UI migration (Skiko + three hosts)

- **`renderer/`** — `Canvas2D` common interface with two actuals:
  - `SwingCanvas2D` (legacy) — wraps `java.awt.Graphics2D`.
  - `SkikoCanvas2D` (new) — wraps `org.jetbrains.skiko.Surface`
    and `Canvas`. Same API; Skiko gives OpenGL / Metal / Direct3D /
    wasm backends out of the box.
- **`ui-swing/`** — Kotlin view-models behind the legacy `ChatWindow`;
  Swing host observes a `StateFlow<ChatViewState>`.
- **`ui-compose-desktop/`** — Compose Multiplatform host using
  `Canvas` composable. Skiko renders the sprite font natively.
- **`ui-compose-html/`** — Compose for Web wasmJs host. Same
  composable. Same Skiko renderer (browser-side).
- **UI tests**:
  - Swing: AssertJ-Swing asserts rendered text.
  - Desktop: Compose UI test + ARGB snapshot of `ChatCanvas`.
  - Web: Compose Web test renderer asserts DOM; Playwright Kotlin
    E2E drives the deployed site against a mocked Twitch WebSocket.

### Phase F — Dual CI/CD + release (with ProGuard)

- **`profile=java`** workflow: `sdk env`, then
  `./gradlew -Pprofile=java check jacocoTestCoverageVerification
  proguardRelease verifyProguardedJar`. Publishes a ProGuard-shrunk
  classic Swing JAR (`chatgamefontificator-legacy-*`).
- **`profile=kmp`** workflow: `sdk env`, then
  `./gradlew -Pprofile=kmp build koverVerify pitest proguardRelease
  verifyProguardedJar packageReleaseDistribution`. Publishes:
  - `chatgamefontificator-core-*` klibs (JVM / JS / wasmJs / Native)
  - `chatgamefontificator-desktop` Compose signed bundle
    (DMG / MSI / AppImage) via Compose's ProGuard integration
  - `chatgamefontificator-web` static site (GitHub Pages)
- **ProGuard rules** (`proguard/*.pro`):
  - `common` — `kotlinx-serialization`, Skiko JNI entry points,
    service-loader, Compose `@Composable` metadata.
  - `java` — Swing reflection (`UIManager`, custom look-and-feel),
    Twitch IRC-client reflective field lookups.
  - `kmp` — Coroutines, Compose runtime, `@JvmStatic`.
- **`verifyProguardedJar`** runs the full JUnit 5 + UI test suite
  against the shrunk artefact on a separate classpath.
- `mapping.txt` uploaded as CI artefact; attached to GitHub release.

Both workflows run on every PR and every tag.

---

## 7. Acceptance criteria

1. Every `CODE_REVIEW.md` finding has a named Kotlin test whose Git
   history shows **red → green** across Phase A → B, and **still
   green** at Phase D running against both Swing- and Skiko-backed
   `Canvas2D`.
2. The `83155b4` whitespace-config bug-class is covered by **three**
   dedicated tests (`unknown_char = " "`, `divider = " "`,
   `empty_rejected`).
3. Phase B closes with JaCoCo 100 % line + branch.
4. Phase C tags `legacy-v1`; no post-tag commit touches
   `src/main/java/**` (CI-enforced).
5. Phase D closes with Kover 100 % + Pitest ≥ 85 %.
6. Differential ARGB parity holds for the committed sprite/chat
   corpus between Swing and Skiko renderers (minus documented
   divergences).
7. `profile=java` + `profile=kmp` green on every PR across
   {ubuntu, macos, windows}; `kmp` also green across {jvm, js,
   wasmJs, native}.
8. ProGuard-shrunk JAR passes the full test suite in
   `verifyProguardedJar` on every release.
9. `.sdkmanrc` + `libs.versions.toml` track the latest stable
   toolchain.

---

## 8. Deliverables

- `.sdkmanrc`, `gradle/libs.versions.toml`, `build.gradle.kts` with
  both profiles.
- `ChatFontIo`, `SpriteRendererIo`, `Canvas2D` interfaces +
  `JavaAdapter` / `KotlinAdapter` actuals + `SwingCanvas2D` /
  `SkikoCanvas2D` actuals.
- `kmp/core/`, `chat/`, `renderer/`, `ui-swing/`,
  `ui-compose-desktop/`, `ui-compose-html/`, `ui-shared/`.
- `proguard/*.pro`.
- `testdata/` corpus (configs, sprites, chats, snapshots) + JBang
  generators.
- `.github/workflows/{java,kmp}.yml` both running ProGuard +
  post-shrink verification.
- `TEST_PLAN.md` auto-generated mapping findings → test FQN.
- `benchmarks/baselines/*.json`.
- [`INTEGRATION.md`](INTEGRATION.md) — how sprite / chat logic is
  vendored into the `fonts-bitsnpicas`-hosted font studio.
