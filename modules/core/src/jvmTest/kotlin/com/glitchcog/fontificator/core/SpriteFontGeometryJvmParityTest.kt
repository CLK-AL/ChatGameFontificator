package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stage S4 differential-parity gate for [SpriteFontGeometry].
 *
 * Runs the same fixtures through both the commonMain Kotlin port and
 * the frozen Java `SpriteFont` (via [JavaLegacyAdapter]) and asserts
 * byte-exact agreement on every produced [CharacterBounds].
 */
class SpriteFontGeometryJvmParityTest {

    // --- fixed-grid parity -------------------------------------------

    @Test
    fun `fixed grid -- 2x2 cells parity`() {
        val key = "ABCD"
        val kotlin = SpriteFontGeometry.calculateFixedBounds(key, 2, 2, 8)
        val java = JavaLegacyAdapter.fixedBoundsViaJava(key, 2, 2, 8)
        assertBoundsParity(java, kotlin)
    }

    @Test
    fun `fixed grid -- 4x3 cells parity`() {
        val key = "0123456789AB"
        val kotlin = SpriteFontGeometry.calculateFixedBounds(key, 4, 3, 16)
        val java = JavaLegacyAdapter.fixedBoundsViaJava(key, 4, 3, 16)
        assertBoundsParity(java, kotlin)
    }

    @Test
    fun `fixed grid -- NORMAL_ASCII_KEY 8x12 parity`() {
        // 8 cols x 12 rows = 96 chars, matching NORMAL_ASCII_KEY.
        val key = SpriteCharacterKey.NORMAL_ASCII_KEY
        val kotlin = SpriteFontGeometry.calculateFixedBounds(key, 8, 12, 16)
        val java = JavaLegacyAdapter.fixedBoundsViaJava(key, 8, 12, 16)
        assertBoundsParity(java, kotlin)
    }

    // --- variable-width parity ---------------------------------------

    @Test
    fun `variable grid -- 2x1 cells with opaque pixels parity`() {
        val key = "AB"
        val pixels = Array(8) { IntArray(16) }
        // Cell 0 (x=0..7): opaque at localX 2..4 on row 3.
        for (px in 2..4) pixels[3][px] = 0xFFFFFFFF.toInt()
        // Cell 1 (x=8..15): opaque at localX 0..6 (abs 8..14) on row 4.
        for (px in 8..14) pixels[4][px] = 0xFFFFFFFF.toInt()

        val kotlin = SpriteFontGeometry.calculateVariableBounds(key, 1, pixels)
        val java = JavaLegacyAdapter.variableBoundsViaJava(key, 1, pixels)
        assertBoundsParity(java, kotlin)
    }

    @Test
    fun `variable grid -- fully transparent cell parity (space-like fallback)`() {
        val key = "A"
        val pixels = Array(8) { IntArray(8) } // zero alpha throughout
        val kotlin = SpriteFontGeometry.calculateVariableBounds(key, 1, pixels)
        val java = JavaLegacyAdapter.variableBoundsViaJava(key, 1, pixels)
        assertBoundsParity(java, kotlin)
    }

    @Test
    fun `variable grid -- 2x2 cells with mixed opaque patterns parity`() {
        val key = "ABCD"
        // 2 cols x 2 rows of 8x8 cells -> 16x16 sheet.
        val pixels = Array(16) { IntArray(16) }
        // A (cell 0,0): left edge 0, right edge 5.
        for (px in 0..5) pixels[2][px] = 0xFF_00FF00.toInt()
        // B (cell 1,0): left edge 8 (abs), right edge 14.
        for (px in 8..14) pixels[3][px] = 0xFF_FF0000.toInt()
        // C (cell 0,1): single pixel at localX 3.
        pixels[10][3] = 0xFF_0000FF.toInt()
        // D (cell 1,1): fully transparent -> fallback quarter-cell.

        val kotlin = SpriteFontGeometry.calculateVariableBounds(key, 2, pixels)
        val java = JavaLegacyAdapter.variableBoundsViaJava(key, 2, pixels)
        assertBoundsParity(java, kotlin)
    }

    @Test
    fun `variable grid -- alpha threshold honoured identically`() {
        // Any non-zero alpha counts as "opaque" in the frozen algorithm.
        val key = "A"
        val pixels = Array(8) { IntArray(8) }
        // A single pixel with alpha=1 (minimum non-zero) at (3, 4).
        pixels[4][3] = 0x01_FFFFFF

        val kotlin = SpriteFontGeometry.calculateVariableBounds(key, 1, pixels)
        val java = JavaLegacyAdapter.variableBoundsViaJava(key, 1, pixels)
        assertBoundsParity(java, kotlin)
    }

    // --- helpers -----------------------------------------------------

    private fun assertBoundsParity(
        java: Map<Int, CharacterBounds>,
        kotlin: Map<Int, CharacterBounds>,
    ) {
        assertEquals(java.keys, kotlin.keys, "bounds key sets diverge")
        for ((codepoint, javaBounds) in java) {
            val kotlinBounds = kotlin[codepoint]
            assertEquals(javaBounds, kotlinBounds, "bounds for codepoint $codepoint diverge")
        }
    }
}
