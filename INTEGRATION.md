# INTEGRATION.md — ChatGameFontificator as a vendored subset

This repository is a **consumed subset** of the
`fonts-bitsnpicas`-hosted font studio. The authoritative integration
design lives in that repo at
[`fonts-bitsnpicas/INTEGRATION.md`](https://github.com/CLK-AL/fonts-bitsnpicas/blob/claude/code-review-k4kzN/INTEGRATION.md).

## Role

`ChatGameFontificator` contributes **two** subsystems to the
integrated font studio:

- `modules/sprite/` — sprite-sheet font loader and renderer, built
  from `com.glitchcog.fontificator.sprite.*` and
  `com.glitchcog.fontificator.config.ConfigFont`.
- `modules/chat-preview/` — chat-overlay renderer, built from the
  message-rendering subset of this app (headless; no IRC, no
  Swing windowing).

Unlike **banana-figlet** which is vendored whole, this repo's code
is **vendored as a subset**. The Twitch IRC client, Swing window
chrome, preset manager, and video overlay code stay in this repo
as the legacy standalone app.

## What is vendored, what is not

| Package / area | Status |
| --- | --- |
| `com.glitchcog.fontificator.sprite.**` | **Vendored** → `modules/sprite/` |
| `com.glitchcog.fontificator.config.ConfigFont` | **Vendored** → `modules/sprite/` (config model only) |
| `com.glitchcog.fontificator.config.ConfigMessage` | **Vendored** → `modules/chat-preview/` |
| `com.glitchcog.fontificator.config.Config.baseValidation` | **Vendored** → `modules/core/` (it's the generic whitespace-aware validator and deserves to be the canonical one) |
| Message-rendering (`MessagePanel`, `SpriteFont.drawCharacter`) | **Vendored** → `modules/chat-preview/` (Skiko-adapted; Swing drop) |
| Twitch IRC client (`IRCClient`, `ChatMessage.parse`) | **Stays** in this repo (host uses a different IRC client: Ktor-based in `modules/chat`) |
| Swing window (`ChatWindow`, menus, property dialogs) | **Stays** (the host has its own Compose UI) |
| Preset / emoji-cache / sound code | **Stays** |

## Integration timeline

| Phase | How the host consumes this repo |
| --- | --- |
| **A** | Host copies `sprite/` + `config/` packages into `main/java/BitsNPicas/src/vendor/cgf/**`, preserving upstream package names for the initial commit. Renames happen in a separate commit (auditable diff against upstream). |
| **B** | Vendored files move into `modules/sprite/src/commonMain/kotlin/**` and `modules/chat-preview/src/commonMain/kotlin/**` as pure Kotlin. The Java delegate in `jvmMain` is retired once Kover hits 100 % on the new modules. |

Full phase diagram:
[`docs/diagrams/05-integration-phases.puml`](../fonts-bitsnpicas/docs/diagrams/05-integration-phases.puml).

## What this repo owes the host

1. **Freeze a stable Java.** Complete Phase A + B of this repo's
   [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md) — the agent run on
   2026-04-15 already landed the three Critical-finding red-green
   pairs; the remainder of the plan finishes the JaCoCo 100 % gate
   on the `sprite/` + `config/` packages specifically.
2. **`Config.baseValidation` bug-class coverage.** Commit `83155b4`
   documented a whitespace-handling regression. The host's
   `modules/core` inherits this validator; at least three pinned
   tests (`unknown_char = " "`, `divider = " "`, `empty_rejected`)
   must come with the vendored code.
3. **`SpriteCharacterKey.isBadge` fix (M4).** The bitwise-`&` bug
   is trivially silently broken — must be fixed before vendoring.
4. **`Sprite.setImage` graphics leak (M6).** Any leaked `Graphics`
   instance inside the host's render loop becomes a 60 Hz leak in
   the Compose renderer; must be fixed before Phase B.

## What the host owes this repo

- Skiko replaces `java.awt.Graphics2D` in the host's render path.
  The vendored `SpriteFont.drawCharacter` needs a `Canvas2D`
  abstraction with two actuals (`SwingCanvas2D` / `SkikoCanvas2D`);
  the host provides both in `modules/renderer/` and guarantees
  ARGB-hash parity between them on the shared corpus.
- Headless test harness (CI under `{ubuntu,macos,windows}`) — the
  sprite renderer is tested without a display via Skiko's
  off-screen surface; the host's `testdata/snapshots/**` corpus
  pins the expected ARGB hashes.

## File-path map (Phase B)

```
ChatGameFontificator/                                               fonts-bitsnpicas (host)

src/main/java/com/glitchcog/fontificator/sprite/Sprite.java      ─► modules/sprite/src/commonMain/kotlin/sprite/Sprite.kt
                               …/SpriteFont.java                 ─► modules/sprite/src/commonMain/kotlin/sprite/SpriteFont.kt
                               …/SpriteCharacterKey.java         ─► modules/sprite/src/commonMain/kotlin/sprite/SpriteCharacterKey.kt

src/main/java/com/glitchcog/fontificator/config/ConfigFont.java  ─► modules/sprite/src/commonMain/kotlin/sprite/SpriteFontConfig.kt
                               …/ConfigMessage.java              ─► modules/chat-preview/src/commonMain/kotlin/chat/MessageConfig.kt
                               …/Config.java baseValidation      ─► modules/core/src/commonMain/kotlin/core/config/BaseValidation.kt

src/test/kotlin/com/glitchcog/fontificator/review/**             ─► run untouched under both profiles on the host
                                                                    via a shared test module
```

## Differential-parity contract

- **ARGB pixel hash.** For every `(spriteSheet, message, font)`
  fixture in `testdata/snapshots/**`, the frozen-Java (Swing +
  `Graphics2D`) render and the `commonMain` Kotlin (Skiko) render
  must agree on a SHA-256 of the ARGB pixel bytes.
- **Config round-trip.** Load → setter → save → load must survive
  every field in `ConfigFont` (pins C1 permanently).

## CI / release channels

This repo keeps its own dual CI/CD:

- `java` profile — classic standalone Swing JAR
  (`chatgamefontificator-legacy-*`), ProGuard-shrunk.
- `kmp` profile — library artefacts consumed by the host (not
  published directly to end users; the **user-facing** artefact
  is the host's `fonts-bitsnpicas-sprite-*` /
  `fonts-bitsnpicas-chat-preview-*`).

## See also

- This repo: [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md) —
  Phase A+B scaffolding + red-green commit schedule (Critical
  findings already landed by the 2026-04-15 agent run).
- Host: [`fonts-bitsnpicas/INTEGRATION.md`](https://github.com/CLK-AL/fonts-bitsnpicas/blob/claude/code-review-k4kzN/INTEGRATION.md) —
  overall design, unified class model, PUML diagrams, vendoring
  discipline, dual-CI/CD pipeline.
