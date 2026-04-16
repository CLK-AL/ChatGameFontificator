package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Stage S4 -- commonMain tests for the immutable `ConfigFont` port.
 *
 * Covers:
 *  - round-trip serialization (`fromProperties -> toProperties -> fromProperties`)
 *  - C1 regression (baselineOffset survives round-trip)
 *  - C2 regression (invalid grid height fails validation)
 *  - default / typical values
 *  - validation edge cases
 */
class ConfigFontTest {

    // ---- helpers -------------------------------------------------------

    /** A valid, typical configuration matching the Java defaults. */
    private fun defaultConfig(): ConfigFont = ConfigFont(
        fontFilename = "preset://fonts/dw3_font.png",
        borderFilename = "preset://borders/dw3_border.png",
        gridWidth = 8,
        gridHeight = 12,
        fontScale = 2.0f,
        borderScale = 3.0f,
        borderInsetX = 1,
        borderInsetY = 1,
        spaceWidth = 25,
        baselineOffset = 0,
        characterKey = " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F",
        unknownChar = '\u007F',
        extendedCharEnabled = true,
        lineSpacing = 2,
        charSpacing = 0,
        messageSpacing = 0,
        fontType = FontType.FIXED_WIDTH,
    )

    // ---- round-trip serialization -------------------------------------

    @Test
    fun `round-trip fromProperties-toProperties-fromProperties equals original`() {
        val original = defaultConfig()
        val props = original.toProperties()
        val restored = ConfigFont.fromProperties(props)
        assertEquals(original, restored)
    }

    @Test
    fun `round-trip preserves all fields including non-defaults`() {
        val custom = ConfigFont(
            fontFilename = "/tmp/my_font.png",
            borderFilename = "/tmp/my_border.png",
            gridWidth = 10,
            gridHeight = 10,
            fontScale = 1.5f,
            borderScale = 2.5f,
            borderInsetX = -5,
            borderInsetY = 10,
            spaceWidth = 30,
            baselineOffset = 3,
            characterKey = "ABCDEFGHIJ" +
                "KLMNOPQRST" +
                "UVWXYZabcd" +
                "efghijklmn" +
                "opqrstuvwx" +
                "yz01234567" +
                "89!@#\$%^&*" +
                "()+=-_<>./" +
                "[]{}|\\;:'\"" +
                "~` ?,\u007F\u0080\u0081\u0082\u0083",
            unknownChar = 'A',
            extendedCharEnabled = false,
            lineSpacing = 5,
            charSpacing = 2,
            messageSpacing = 10,
            fontType = FontType.VARIABLE_WIDTH,
        )
        val restored = ConfigFont.fromProperties(custom.toProperties())
        assertEquals(custom, restored)
    }

    @Test
    fun `toProperties produces expected key set`() {
        val props = defaultConfig().toProperties()
        val expected = setOf(
            "fontFile", "fontBorderFile", "fontGridWidth", "fontGridHeight",
            "fontScale", "fontBorderScale", "fontBorderInsetX", "fontBorderInsetY",
            "fontSpaceWidth", "fontBaselineOffset", "fontCharacters",
            "fontUnknownChar", "fontExtendedChar", "fontLineSpacing",
            "fontCharSpacing", "fontMessageSpacing", "fontType",
        )
        assertEquals(expected, props.keys)
    }

    // ---- C1 regression: baselineOffset survives round-trip ------------

    @Test
    fun `C1 regression -- baselineOffset survives round-trip`() {
        val cfg = defaultConfig().copy(baselineOffset = 7)
        val props = cfg.toProperties()
        // The serialised map must contain the value.
        assertEquals("7", props[ConfigFont.KEY_FONT_BASELINE_OFFSET])
        // Deserialised config must carry the same value.
        val restored = ConfigFont.fromProperties(props)
        assertEquals(7, restored.baselineOffset)
    }

    @Test
    fun `C1 regression -- negative baselineOffset round-trips`() {
        val cfg = defaultConfig().copy(baselineOffset = -15)
        val restored = ConfigFont.fromProperties(cfg.toProperties())
        assertEquals(-15, restored.baselineOffset)
    }

    // ---- C2 regression: invalid grid height fails validation ----------

    @Test
    fun `C2 regression -- zero gridHeight produces validation error`() {
        val cfg = defaultConfig().copy(gridHeight = 0)
        val errors = cfg.validate()
        assertTrue(errors.any { "fontGridHeight" in it }, "Expected gridHeight error, got: $errors")
    }

    @Test
    fun `C2 regression -- negative gridHeight produces validation error`() {
        val cfg = defaultConfig().copy(gridHeight = -1)
        val errors = cfg.validate()
        assertTrue(errors.any { "fontGridHeight" in it }, "Expected gridHeight error, got: $errors")
    }

    @Test
    fun `C2 regression -- zero gridWidth produces validation error`() {
        val cfg = defaultConfig().copy(gridWidth = 0)
        val errors = cfg.validate()
        assertTrue(errors.any { "fontGridWidth" in it }, "Expected gridWidth error, got: $errors")
    }

    @Test
    fun `C2 regression -- valid grid but wrong key length reports mismatch`() {
        val cfg = defaultConfig().copy(characterKey = "AB")
        val errors = cfg.validate()
        assertTrue(errors.any { "Character key length" in it }, "Expected key length error, got: $errors")
    }

    // ---- default values validation ------------------------------------

    @Test
    fun `default config validates without errors`() {
        val errors = defaultConfig().validate()
        assertEquals(emptyList(), errors, "Default config should be valid, got: $errors")
    }

    // ---- validation edge cases ----------------------------------------

    @Test
    fun `empty fontFilename fails validation`() {
        val cfg = defaultConfig().copy(fontFilename = "")
        assertTrue(cfg.validate().any { "font filename" in it })
    }

    @Test
    fun `empty borderFilename fails validation`() {
        val cfg = defaultConfig().copy(borderFilename = "")
        assertTrue(cfg.validate().any { "border filename" in it })
    }

    @Test
    fun `unknownChar not in characterKey fails validation`() {
        // Use a character that is definitely not in the key.
        val cfg = defaultConfig().copy(unknownChar = '\u0001')
        assertTrue(cfg.validate().any { KEY_FONT_UNKNOWN_CHAR in it })
    }

    @Test
    fun `fontScale below minimum fails validation`() {
        val cfg = defaultConfig().copy(fontScale = 0.1f)
        assertTrue(cfg.validate().any { KEY_FONT_SCALE in it })
    }

    @Test
    fun `fontScale above maximum fails validation`() {
        val cfg = defaultConfig().copy(fontScale = 100f)
        assertTrue(cfg.validate().any { KEY_FONT_SCALE in it })
    }

    @Test
    fun `borderInsetX out of range fails validation`() {
        val cfg = defaultConfig().copy(borderInsetX = 999)
        assertTrue(cfg.validate().any { "fontBorderInsetX" in it })
    }

    @Test
    fun `messageSpacing out of range fails validation`() {
        val cfg = defaultConfig().copy(messageSpacing = -1)
        assertTrue(cfg.validate().any { "fontMessageSpacing" in it })
    }

    // ---- fromProperties error handling --------------------------------

    @Test
    fun `fromProperties throws on missing key`() {
        val props = defaultConfig().toProperties().toMutableMap()
        props.remove(ConfigFont.KEY_FONT_BASELINE_OFFSET)
        assertFailsWith<IllegalArgumentException> {
            ConfigFont.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on invalid fontType`() {
        val props = defaultConfig().toProperties().toMutableMap()
        props[ConfigFont.KEY_FONT_TYPE] = "MONOSPACED"
        assertFailsWith<IllegalArgumentException> {
            ConfigFont.fromProperties(props)
        }
    }

    // ---- FontType companion -------------------------------------------

    @Test
    fun `FontType contains recognises valid names`() {
        assertTrue(FontType.contains("FIXED_WIDTH"))
        assertTrue(FontType.contains("VARIABLE_WIDTH"))
    }

    @Test
    fun `FontType contains rejects invalid names`() {
        assertTrue(!FontType.contains("MONOSPACED"))
        assertTrue(!FontType.contains(""))
    }

    companion object {
        private const val KEY_FONT_UNKNOWN_CHAR = "fontUnknownChar"
        private const val KEY_FONT_SCALE = "fontScale"
    }
}
