package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Stage S4 -- proof-of-concept positive tests for the commonMain
 * `SpriteFontGeometry` port.
 *
 * Parity against the frozen Java `SpriteFont` geometry methods is
 * covered by `SpriteFontGeometryJvmParityTest` on the JVM side.
 */
class SpriteFontGeometryTest {

    // --- fixed-grid happy path --------------------------------------

    @Test
    fun `fixed bounds -- each cell at its grid position with cell-sized rect`() {
        // 2x2 grid, 8-pixel cells.
        val key = "ABCD"
        val bounds = SpriteFontGeometry.calculateFixedBounds(key, 2, 2, 8)

        assertEquals(4, bounds.size, "every cell should produce a bound")
        assertEquals(CharacterBounds(0, 0, 8, 8), bounds['A'.code])
        assertEquals(CharacterBounds(8, 0, 8, 8), bounds['B'.code])
        assertEquals(CharacterBounds(0, 8, 8, 8), bounds['C'.code])
        assertEquals(CharacterBounds(8, 8, 8, 8), bounds['D'.code])
    }

    @Test
    fun `fixed bounds -- 4x3 grid with non-square sheet placement`() {
        // 4 cols, 3 rows, 16 px cells -> 64x48 sheet.
        val key = "0123456789AB"
        val bounds = SpriteFontGeometry.calculateFixedBounds(key, 4, 3, 16)
        assertEquals(12, bounds.size)
        // "5" is cell index 5 -> col 1, row 1.
        assertEquals(CharacterBounds(16, 16, 16, 16), bounds['5'.code])
        // "B" is cell index 11 -> col 3, row 2.
        assertEquals(CharacterBounds(48, 32, 16, 16), bounds['B'.code])
    }

    // --- variable-width happy path ----------------------------------

    @Test
    fun `variable bounds -- synthetic 2x1 grid of 8x8 cells`() {
        // 2 cells wide (cols), 1 row = 16 px wide x 8 px tall.
        val key = "AB"
        val pixels = Array(8) { IntArray(16) }
        // Fill cell 0 (x=0..7): opaque pixel at col 2, col 3, col 4.
        for (px in 2..4) {
            pixels[3][px] = 0xFFFFFFFF.toInt()
        }
        // Fill cell 1 (x=8..15): opaque pixel at col 0..6 (local x 0..6 -> absolute 8..14).
        for (px in 8..14) {
            pixels[4][px] = 0xFFFFFFFF.toInt()
        }

        val bounds = SpriteFontGeometry.calculateVariableBounds(key, 1, pixels)
        assertEquals(2, bounds.size)

        // "A": leftEdge=2, rightEdge=4, letterWidth=min(8, 4+1 - 2)=3, absolute x = 0 + 2 = 2.
        assertEquals(CharacterBounds(2, 0, 3, 8), bounds['A'.code])
        // "B": leftEdge=0, rightEdge=6 (local), letterWidth=min(8, 6+1 - 0)=7, absolute x = 8 + 0 = 8.
        assertEquals(CharacterBounds(8, 0, 7, 8), bounds['B'.code])
    }

    @Test
    fun `variable bounds -- fully transparent cell gets quarter-cell placeholder`() {
        val key = "A"
        val pixels = Array(8) { IntArray(8) } // all zero = transparent
        val bounds = SpriteFontGeometry.calculateVariableBounds(key, 1, pixels)
        assertEquals(1, bounds.size)
        // Quarter-cell: x = 0 + 8/4 = 2, width = 8/2 = 4.
        assertEquals(CharacterBounds(2, 0, 4, 8), bounds['A'.code])
    }

    // --- M5 regression: duplicate characters ------------------------

    @Test
    fun `M5 regression -- duplicate chars in fixed key get distinct cells`() {
        // Key with duplicates: 'A' appears at cell 0 and cell 2.
        // The legacy code called `key.indexOf('A')` -> 0 both times,
        // collapsing cell 2's bounds onto cell 0. The fix uses the
        // loop index, so cell 2 overwrites cell 0 in the map.
        val key = "ABAB"
        val bounds = SpriteFontGeometry.calculateFixedBounds(key, 2, 2, 8)
        // With M5: 'A' is written at i=0 (cell (0,0)) then again at i=2 (cell (0,1)).
        // Last-write-wins -> the final 'A' entry is cell (0,1).
        assertEquals(CharacterBounds(0, 8, 8, 8), bounds['A'.code])
        // Similarly 'B' is written at i=1 (cell (1,0)) then i=3 (cell (1,1)).
        assertEquals(CharacterBounds(8, 8, 8, 8), bounds['B'.code])
    }

    @Test
    fun `M5 regression -- duplicate chars in variable key get distinct cells`() {
        // Same duplicate key, but via the variable-width path.
        val key = "AA"
        val pixels = Array(8) { IntArray(16) }
        // cell 0 opaque at localX=1..2, cell 1 opaque at localX=3..5.
        pixels[2][1] = 0xFFFFFFFF.toInt()
        pixels[2][2] = 0xFFFFFFFF.toInt()
        pixels[2][8 + 3] = 0xFFFFFFFF.toInt()
        pixels[2][8 + 5] = 0xFFFFFFFF.toInt()

        val bounds = SpriteFontGeometry.calculateVariableBounds(key, 1, pixels)
        // With M5: cell 0's bounds written for 'A', then cell 1's bounds
        // overwrite. Final 'A' entry must match cell 1, not cell 0.
        val a = assertNotNull(bounds['A'.code])
        assertEquals(8 + 3, a.x, "M5: 'A' must reflect cell 1 (last write), not cell 0")
        // width = min(8, (5+1) - 3) = 3
        assertEquals(3, a.width)
    }

    // --- M7 regression: oversized grid ------------------------------

    @Test
    fun `M7 regression -- fixed bounds with oversized grid does not throw`() {
        // Grid claims 4x3 = 12 cells but key is only 3 chars long.
        val key = "ABC"
        val bounds = SpriteFontGeometry.calculateFixedBounds(key, 4, 3, 8)
        // Only the 3 real key characters should be bound; no IOOBE.
        assertEquals(3, bounds.size)
        assertEquals(CharacterBounds(0, 0, 8, 8), bounds['A'.code])
        assertEquals(CharacterBounds(8, 0, 8, 8), bounds['B'.code])
        assertEquals(CharacterBounds(16, 0, 8, 8), bounds['C'.code])
    }

    @Test
    fun `M7 regression -- variable bounds with oversized sheet does not throw`() {
        // Key has 2 chars but the pixel sheet has 3 cells worth of pixels.
        val key = "AB"
        // 3 cols x 1 row of 8x8 cells -> 24 px wide.
        val pixels = Array(8) { IntArray(24) }
        val bounds = SpriteFontGeometry.calculateVariableBounds(key, 1, pixels)
        // Must stop at the key length, not iterate the phantom third cell.
        assertEquals(2, bounds.size)
        assertTrue('A'.code in bounds)
        assertTrue('B'.code in bounds)
    }

    // --- C3 regression: missing-codepoint fallback ------------------

    @Test
    fun `C3 regression -- lookupBounds returns fallback for missing codepoint`() {
        val bounds = SpriteFontGeometry.calculateFixedBounds("AB", 2, 1, 8)
        // 'Z' is not in the key.
        val fallback = SpriteFontGeometry.lookupBounds('Z'.code, bounds, 8)
        // Fallback must not be null and must carry the cell size.
        assertEquals(CharacterBounds(0, 0, 8, 8), fallback)
    }

    @Test
    fun `C3 regression -- lookupBounds returns map entry when present`() {
        val bounds = SpriteFontGeometry.calculateFixedBounds("AB", 2, 1, 8)
        val a = SpriteFontGeometry.lookupBounds('A'.code, bounds, 8)
        assertEquals(CharacterBounds(0, 0, 8, 8), a)
        val b = SpriteFontGeometry.lookupBounds('B'.code, bounds, 8)
        assertEquals(CharacterBounds(8, 0, 8, 8), b)
    }

    // --- SpriteFontMetrics projection -------------------------------

    @Test
    fun `SpriteFontMetrics fromConfig copies geometry fields`() {
        val config = ConfigFont(
            fontFilename = "preset://f.png",
            borderFilename = "preset://b.png",
            gridWidth = 8,
            gridHeight = 12,
            fontScale = 1.0f,
            borderScale = 1.0f,
            borderInsetX = 0,
            borderInsetY = 0,
            spaceWidth = 25,
            baselineOffset = 0,
            characterKey = " ".repeat(96),
            unknownChar = ' ',
            extendedCharEnabled = false,
            lineSpacing = 0,
            charSpacing = 0,
            messageSpacing = 0,
            fontType = FontType.FIXED_WIDTH,
        )
        val metrics = SpriteFontMetrics.fromConfig(config, pixelsPerCell = 16)
        assertEquals(8, metrics.gridWidth)
        assertEquals(12, metrics.gridHeight)
        assertEquals(16, metrics.pixelsPerCell)
        assertEquals(96, metrics.characterKey.length)
        assertEquals(FontType.FIXED_WIDTH, metrics.fontType)
    }
}
