# Code Review: ChatGameFontificator

## Overview

ChatGameFontificator is a Java Swing application that renders Twitch chat
messages using custom pixel-art fonts loaded from sprite sheets. The core
font system (`SpriteFont`) handles character rendering, emoji/badge overlay,
text wrapping, and color management. The config system (`ConfigFont`,
`Config`) loads and validates font parameters from properties files. The
rendering pipeline uses `Sprite` for image management and
`SpriteCharacterKey` to abstract characters/emoji/badges.

---

## Critical

### 1. `setBaselineOffset` never writes to the properties map

`src/main/java/com/glitchcog/fontificator/config/ConfigFont.java:369-373`

```java
public void setBaselineOffset(int baselineOffset)
{
    this.baselineOffset = baselineOffset;
    props.getProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET,
                      Integer.toString(baselineOffset));
}
```

`getProperty` is a read with a default — the computed default value is
discarded. The baseline offset is never persisted to `props`, so saving the
config loses the user's change.

**Fix:** Replace with `props.setProperty(...)`. (Check the other `set…`
methods in the file for the same copy-paste error.)

### 2. Height check is actually a duplicate width check

`src/main/java/com/glitchcog/fontificator/config/ConfigFont.java:158`

```java
if (w > 0 && w > 0)
```

Height validation is silently skipped. A grid with `h <= 0` bypasses the
`w * h == charKey.length()` check and produces undefined rendering.

**Fix:** `if (w > 0 && h > 0)`.

### 3. `getCharacterBounds` can return `null` to callers that dereference it

`src/main/java/com/glitchcog/fontificator/sprite/SpriteFont.java:322-328` and
call sites at `:157, :164, :617`.

For a `VARIABLE_WIDTH` font, `spaceBounds` can be `null` (e.g. when the space
character isn't in `characterBounds`) and is then modified without a null
guard. Callers also read `.width` on the returned value without null checks.

**Fix:** Either guarantee the space character is always in the map (populate
it explicitly during `calculate…Dimensions`) or null-check everywhere the
result is consumed.

---

## Major

### 4. Bitwise `&` instead of logical `&&` in `isBadge`

`src/main/java/com/glitchcog/fontificator/sprite/SpriteCharacterKey.java:142`

```java
return !isChar() & badge;
```

This currently evaluates to the right answer because both operands are
`boolean`, but it forces evaluation of the right operand and signals a code
smell that is easy to copy into a more dangerous spot.

**Fix:** `return !isChar() && badge;`.

### 5. `calculateFixedCharacterDimensions` uses `indexOf(c)` for grid position

`src/main/java/com/glitchcog/fontificator/sprite/SpriteFont.java:208`

Looking up the first occurrence of `c` in `key` means duplicate characters in
the key always resolve to the first position, silently misrendering the
later occurrence.

**Fix:** Use the loop index `i` directly (`int index = i;`) rather than
`indexOf(c)`.

### 6. Graphics object leaked in `Sprite.setImage`

`src/main/java/com/glitchcog/fontificator/sprite/Sprite.java:146-147`

```java
img.getGraphics().drawImage(testImg, 0, 0, null);
img.getGraphics().dispose();
```

Each `getGraphics()` call returns a new `Graphics` instance; the first one
used for `drawImage` is never disposed, and the second is disposed without
being used.

**Fix:**

```java
Graphics g = img.getGraphics();
try { g.drawImage(testImg, 0, 0, null); }
finally { g.dispose(); }
```

### 7. `letterIndex` can overrun the key length

`src/main/java/com/glitchcog/fontificator/sprite/SpriteFont.java:244, :304`

`letterIndex` is incremented for every grid cell, and `key.charAt(letterIndex)`
is called without a bounds check. If `gridWidth * gridHeight > key.length()`
(e.g. a mis-edited config), `charAt` throws `StringIndexOutOfBoundsException`.

**Fix:** Guard `if (letterIndex >= key.length()) break;` and surface a clear
log message — the condition indicates a bad config that's easy to hit.

### 8. Variable-width glyphs can end up with width 0

`src/main/java/com/glitchcog/fontificator/sprite/SpriteFont.java:286`

```java
final int letterWidth = Math.min(charWidth, rightEdge - leftEdge);
```

Nothing forces `rightEdge > leftEdge`, so completely empty glyphs store a
width of `0`. Downstream rendering then advances by zero pixels, collapsing
consecutive invisible characters into the same column.

**Fix:** `Math.max(1, Math.min(charWidth, rightEdge - leftEdge))`, or
explicitly model "zero-width glyph" and consume a known advance width.

---

## Minor

### 9. `baseValidation` rejects configs where space is a valid value

`src/main/java/com/glitchcog/fontificator/config/Config.java:46-49`

`props.getProperty(keys[i]).trim().isEmpty()` rejects a single-space value
because of the `trim()`. The code then tries to allow-list a couple of keys
that legitimately accept space, but the logic is awkward and easy to break
when adding similar settings.

**Fix:** Extract a helper such as
`isBlankIgnoringAllowedSpaces(key, value)`, and only trim in the validation
path — not the stored value. (This is the same class of bug as the recent
"trim() was incorrectly invalidating configs when they had spaces as
unknown characters or user message dividers" fix.)

### 10. Uncached character-bounds lookups in the render hot path

`src/main/java/com/glitchcog/fontificator/sprite/SpriteFont.java:138-166`

`getCharacterBounds` is called once per character on every rendered message.
For variable-width fonts with many lookups this is measurable.

**Fix:** Cache a computed `CharacterBounds[]` for ASCII (0–127), falling back
to the HashMap for higher codepoints.

### 11. Integer-division precision loss in sprite sizing

`src/main/java/com/glitchcog/fontificator/sprite/Sprite.java:192, :209`

`pixelWidth = img.getWidth(null) / gridWidth` silently loses fractional
pixels when the grid doesn't divide the image evenly.

**Fix:** Either require exact divisibility and fail loudly, or document the
truncation.

### 12. `coloredImgs` cache has no eviction

`src/main/java/com/glitchcog/fontificator/sprite/Sprite.java:259-272`

Every unique color requested is cached indefinitely. Long-running chat
sessions with many unique name/message colours leak memory over time.

**Fix:** Bound the cache (e.g. `LinkedHashMap` with
`removeEldestEntry` returning true over N entries).

### 13. Missing null-check in `drawCharacter`

`src/main/java/com/glitchcog/fontificator/sprite/SpriteFont.java:617`

`characterBounds.get(sck.getCodepoint())` can return `null`, but the caller
uses `.width` directly. See finding #3.

---

## Nit

### 14. Garbled Javadoc

`src/main/java/com/glitchcog/fontificator/config/ConfigMessage.java:71`

"The formatter for timestampstimeFormatIntimeFormatInputput" → "The formatter
for timestamps".

### 15. Typo in log message

`src/main/java/com/glitchcog/fontificator/sprite/Sprite.java:66`

`construstor` → `constructor`.

### 16. Shadowed loop variable in `SpriteFont` drawing loop

`src/main/java/com/glitchcog/fontificator/sprite/SpriteFont.java:464`

Rename the outer loop variable from `c` to `i` to avoid visual confusion
with inner `text[c]` access.

---

## Summary

| Severity | Count |
| --- | --- |
| Critical | 3 |
| Major    | 5 |
| Minor    | 5 |
| Nit      | 3 |

**Top priorities:**

1. Fix the three critical bugs — `setBaselineOffset`, the duplicated `w > 0`
   check, and the unchecked `getCharacterBounds` returns — all are concrete
   user-facing defects and trivial to patch.
2. Address the `indexOf`-for-duplicates issue (#5) and the `letterIndex`
   overrun (#7) — both cause hard-to-diagnose rendering regressions for bad
   configs.
3. Close the `Graphics` leak in `Sprite.setImage` (#6); it's a tiny change
   with outsized reliability impact.
