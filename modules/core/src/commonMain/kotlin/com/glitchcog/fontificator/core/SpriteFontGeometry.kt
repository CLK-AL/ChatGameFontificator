package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin port of the geometry-only parts of
 * `com.glitchcog.fontificator.sprite.SpriteFont`.
 *
 * The frozen Java class interleaves two responsibilities:
 *  1. Geometric layout of character cells on the sprite sheet
 *     (`calculateFixedCharacterDimensions`,
 *     `calculateVariableCharacterDimensions`, `getCharacterBounds`).
 *  2. Rendering the sprite onto a `Graphics2D`
 *     (`drawCharacter`, `drawMessage`, `getColoredImage`).
 *
 * Only (1) is ported here. Rendering stays on the JVM/UI tier and
 * lands in a later S4 sub-stage. This split keeps `commonMain` free
 * of `java.awt.*`.
 *
 * ### Carried fixes
 * - **M5** (`use loop index, not indexOf`): the legacy variable-width
 *   loop called `key.indexOf(c)`, which returned the *first* index of
 *   a repeated character and silently collided duplicates onto a
 *   single cell. Both [calculateFixedBounds] and
 *   [calculateVariableBounds] use the running loop index instead,
 *   so every grid cell gets its own bounds entry.
 * - **M7** (bounds guard): the legacy variable-width loop walked the
 *   full `wholeWidth x wholeHeight` pixel matrix and indexed into
 *   `key.charAt(letterIndex)` without a guard. When the sheet was
 *   larger than `key.length` cells, this threw
 *   `StringIndexOutOfBoundsException`. The port stops iterating once
 *   `letterIndex >= key.length`.
 * - **C3** (fallback bounds for missing chars): the legacy
 *   `getCharacterBounds(int)` returned `null` for codepoints absent
 *   from the map; callers then NPE'd on `.width`. [lookupBounds]
 *   replicates the hardened behaviour: return a synthesised fallback
 *   cell sized to [SpriteFontMetrics.pixelsPerCell].
 */
public object SpriteFontGeometry {

    /**
     * Compute fixed-width bounds: every character gets a cell of
     * exactly `pixelsPerCell x pixelsPerCell` at its grid position.
     *
     * @param key the character key; maps grid cell index -> codepoint.
     * @param gridWidth number of grid columns.
     * @param gridHeight number of grid rows (used only as an upper
     *                   bound via `gridWidth * gridHeight`).
     * @param pixelsPerCell pixel side length of one grid cell.
     * @return map from codepoint to [CharacterBounds]; duplicate
     *         characters in the key produce last-write-wins entries
     *         (M5 fix: the writer is the *current* loop index, not
     *         the first `indexOf`).
     */
    public fun calculateFixedBounds(
        key: String,
        gridWidth: Int,
        gridHeight: Int,
        pixelsPerCell: Int,
    ): Map<Int, CharacterBounds> {
        val result = mutableMapOf<Int, CharacterBounds>()
        // M7: cap iteration at the minimum of the grid capacity and
        // the key length. A mis-sized grid must not throw.
        val limit = minOf(key.length, gridWidth * gridHeight)
        for (i in 0 until limit) {
            val c = key[i]
            // M5 fix: use the loop index `i`, not `key.indexOf(c)`,
            // so repeated characters are placed at their actual cell.
            val gridX = i % gridWidth
            val gridY = i / gridWidth
            result[c.code] = CharacterBounds(
                x = gridX * pixelsPerCell,
                y = gridY * pixelsPerCell,
                width = pixelsPerCell,
                height = pixelsPerCell,
            )
        }
        return result
    }

    /**
     * Compute variable-width bounds by scanning a pre-extracted ARGB
     * pixel matrix for opaque pixels in each grid cell.
     *
     * @param key the character key; maps grid cell index -> codepoint.
     * @param gridRows number of grid rows on the sheet.
     * @param pixels row-major 2D pixel matrix; `pixels[y][x]` is the
     *               ARGB int at pixel (x, y) of the decoded sheet.
     *               The outer dimension is pixel rows (not grid rows),
     *               the inner dimension is pixel columns.
     * @return map from codepoint to [CharacterBounds].
     */
    public fun calculateVariableBounds(
        key: String,
        gridRows: Int,
        pixels: Array<IntArray>,
    ): Map<Int, CharacterBounds> {
        val result = mutableMapOf<Int, CharacterBounds>()
        if (gridRows <= 0 || pixels.isEmpty()) return result
        val wholeHeight = pixels.size
        val wholeWidth = pixels[0].size
        if (wholeWidth <= 0) return result

        // The sprite cell is assumed square; both sides derive from the
        // grid. We infer gridCols from the key length and gridRows,
        // matching the frozen Java invariant `key.length == gridW * gridH`.
        val gridCols = if (gridRows > 0) key.length / gridRows else 0
        if (gridCols <= 0) return result
        val charWidth = wholeWidth / gridCols
        val charHeight = wholeHeight / gridRows
        if (charWidth <= 0 || charHeight <= 0) return result

        var letterIndex = 0
        // For each grid row
        var y = 0
        while (y < wholeHeight) {
            // For each grid col
            var x = 0
            while (x < wholeWidth) {
                // M7: stop once we've assigned every character in the
                // key. Oversized sprite sheets must not crash.
                if (letterIndex >= key.length) return result

                val ckey = key[letterIndex]
                var leftEdgeFound = false
                var leftEdge = 0
                var rightEdge = 0

                // Scan each column in the cell for opaque pixels.
                for (localX in 0 until charWidth) {
                    val absoluteX = localX + x
                    if (absoluteX >= wholeWidth) break
                    var opaquePixelFound = false
                    var absoluteY = y
                    while (absoluteY < y + charHeight && absoluteY < wholeHeight && !opaquePixelFound) {
                        // Alpha is the top byte of the ARGB int.
                        opaquePixelFound = ((pixels[absoluteY][absoluteX] shr 24) and 0xff) > 0
                        absoluteY++
                    }
                    if (opaquePixelFound) {
                        if (!leftEdgeFound) {
                            leftEdge = localX
                            leftEdgeFound = true
                        }
                        rightEdge = localX
                    }
                }

                // Java matches: add one for inter-character spacing,
                // cap at charWidth as a safety bound.
                val rightEdgeInclusive = rightEdge + 1
                val letterWidth = minOf(charWidth, rightEdgeInclusive - leftEdge)

                val bounds = if (!leftEdgeFound) {
                    // All-transparent cell (typical for the space char):
                    // use a quarter-cell-wide placeholder centred in the cell.
                    CharacterBounds(
                        x = x + charWidth / 4,
                        y = y,
                        width = charWidth / 2,
                        height = charHeight,
                    )
                } else {
                    CharacterBounds(
                        x = x + leftEdge,
                        y = y,
                        width = letterWidth,
                        height = charHeight,
                    )
                }

                // M5 fix: the writer is keyed by `letterIndex`
                // (the current cell), not `key.indexOf(ckey)`.
                result[ckey.code] = bounds
                letterIndex++
                x += charWidth
            }
            y += charHeight
        }
        return result
    }

    /**
     * Look up a codepoint's bounds with the C3 hardened semantics.
     *
     * The frozen Java `getCharacterBounds` returned `null` for
     * codepoints absent from the map; S1 fixed that to return a
     * fallback rectangle so callers that dereference `.width`
     * directly don't NPE. This mirrors that behaviour in the
     * commonMain tier.
     *
     * @param codepoint the Unicode codepoint to look up.
     * @param bounds the map produced by [calculateFixedBounds] or
     *               [calculateVariableBounds].
     * @param pixelsPerCell used to size the fallback when [codepoint]
     *                      is missing.
     */
    public fun lookupBounds(
        codepoint: Int,
        bounds: Map<Int, CharacterBounds>,
        pixelsPerCell: Int,
    ): CharacterBounds =
        bounds[codepoint]
            ?: CharacterBounds(x = 0, y = 0, width = pixelsPerCell, height = pixelsPerCell)
}
