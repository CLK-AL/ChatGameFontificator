# legacy-v1 — Stage S3 freeze marker

```
legacy-v1 = 0f878402a9c0e6986f384fe1336be9cce0ee214f
branch    = claude/code-review-k4kzN
date      = 2026-04-15
```

(Annotated git tag created locally on the same commit; upstream
endpoint rejects tag pushes with HTTP 403, so this file is the
authoritative freeze marker used by the freeze-guard CI check.)

## What's frozen

```
src/main/java/**
```

CODEOWNERS assigns that path to `@legacy-frozen` and
[`.github/workflows/freeze-guard.yml`](.github/workflows/freeze-guard.yml)
fails any PR that modifies it after this commit.

## Coverage gates at freeze (Option-C scope)

| Scope | Line | Branch |
| --- | ---: | ---: |
| `com.glitchcog.fontificator.sprite.**`    | 100.0 % | 97.2 % |
| `com.glitchcog.fontificator.config.**`    | 100.0 % | 97.2 % |

`./gradlew check` enforces strict `LINE = 1.0` via
`jacocoTestCoverageVerification`, with class-level + branch-level
excludes documented inline in `build.gradle.kts` (each justified
against reaches into out-of-scope packages: Swing chrome, Twitch
IRC, emoji-jobs, jasypt, disk I/O).

## Out-of-scope packages (retained but not gated)

The following packages are compiled to host the Swing `UiDriver`
oracle (see [`../fonts-bitsnpicas/INTEGRATION.md`](../fonts-bitsnpicas/INTEGRATION.md) §4.6)
but excluded from the 100 % line gate. They are pinned by ARGB
snapshot via the `UiDriver` tier once the Swing actual lands:

- `com.glitchcog.fontificator.gui.**` (Swing window chrome)
- `com.glitchcog.fontificator.bot.**` (Twitch IRC client)
- `com.glitchcog.fontificator.emoji.**` (emoji-job package)
- `com.glitchcog.fontificator.preset.**`
- `com.glitchcog.fontificator.sound.**`

## Path forward (Stage S4+)

- All new work lands in the fonts-bitsnpicas host at:
  - `modules/sprite/` — Kotlin port of `sprite.**`
  - `modules/chat-preview/` — Kotlin port of render + canned-chat logic
  - `modules/core/` — the generic whitespace-aware `Config.baseValidation`
    validator (canonicalised across the studio)
- Legacy Java is retained only as the test oracle + differential
  parity reference. See
  [`INTEGRATION.md`](INTEGRATION.md).

## FREEZE changelog

<!-- one line per intentional modification of the frozen tree; none yet -->
