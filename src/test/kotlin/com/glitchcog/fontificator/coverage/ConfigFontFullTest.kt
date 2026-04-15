package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.config.ConfigFont
import com.glitchcog.fontificator.config.FontType
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigErrorType
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Exercise every getter/setter/validator/load path in ConfigFont. */
class ConfigFontFullTest {

    private fun validProps(fontFile: String = "preset://fonts/dw3_font.png",
                           borderFile: String = "preset://borders/dw3_border.png"): Properties =
        Properties().apply {
            setProperty(FontificatorProperties.KEY_FONT_FILE_BORDER, borderFile)
            setProperty(FontificatorProperties.KEY_FONT_FILE_FONT, fontFile)
            setProperty(FontificatorProperties.KEY_FONT_TYPE, FontType.FIXED_WIDTH.name)
            setProperty(FontificatorProperties.KEY_FONT_GRID_WIDTH, "1")
            setProperty(FontificatorProperties.KEY_FONT_GRID_HEIGHT, "1")
            setProperty(FontificatorProperties.KEY_FONT_SCALE, "1.0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_SCALE, "1.0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_X, "0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_Y, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACE_WIDTH, "25")
            setProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET, "0")
            setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, "A")
            setProperty(FontificatorProperties.KEY_FONT_EXTENDED_CHAR, "true")
            setProperty(FontificatorProperties.KEY_FONT_CHARACTERS, "A")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_LINE, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_CHAR, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_MESSAGE, "0")
        }

    @Test
    fun load_success_populates_every_field_and_round_trips_setters() {
        val cfg = ConfigFont()
        val report = cfg.load(validProps(), LoadConfigReport())
        assertTrue(report.isErrorFree, report.messages.joinToString())

        // Every getter/setter — covers each one-line body once.
        cfg.fontFilename = "preset://fonts/dw3_font.png"
        assertEquals("preset://fonts/dw3_font.png", cfg.fontFilename)
        cfg.borderFilename = "preset://borders/dw3_border.png"
        assertEquals("preset://borders/dw3_border.png", cfg.borderFilename)
        cfg.gridWidth = 8; assertEquals(8, cfg.gridWidth)
        cfg.gridHeight = 12; assertEquals(12, cfg.gridHeight)
        cfg.fontScale = 2.0f; assertEquals(2.0f, cfg.fontScale)
        cfg.borderScale = 3.0f; assertEquals(3.0f, cfg.borderScale)
        cfg.borderInsetX = 4; assertEquals(4, cfg.borderInsetX)
        cfg.borderInsetY = 5; assertEquals(5, cfg.borderInsetY)
        cfg.spaceWidth = 50; assertEquals(50, cfg.spaceWidth)
        cfg.baselineOffset = 7; assertEquals(7, cfg.baselineOffset)
        cfg.characterKey = "AB"; assertEquals("AB", cfg.characterKey)
        cfg.unknownChar = 'X'; assertEquals('X', cfg.unknownChar)
        cfg.isExtendedCharEnabled = true; assertTrue(cfg.isExtendedCharEnabled)
        cfg.lineSpacing = 1; assertEquals(1, cfg.lineSpacing)
        cfg.charSpacing = 2; assertEquals(2, cfg.charSpacing)
        cfg.messageSpacing = 3; assertEquals(3, cfg.messageSpacing)
        cfg.fontType = FontType.VARIABLE_WIDTH; assertEquals(FontType.VARIABLE_WIDTH, cfg.fontType)
    }

    @Test
    fun reset_nulls_all_fields() {
        val cfg = ConfigFont()
        cfg.load(validProps(), LoadConfigReport())
        cfg.reset()
        assertNull(cfg.fontFilename)
        assertNull(cfg.borderFilename)
        assertNull(cfg.fontType)
        assertNull(cfg.characterKey)
    }

    @Test
    fun validateFontFile_accepts_existing_preset_and_real_disk_file() {
        val cfg = ConfigFont()
        val ok = LoadConfigReport()
        cfg.validateFontFile(ok, "preset://fonts/dw3_font.png")
        assertTrue(ok.isErrorFree)

        val tmp = Files.createTempFile("cfg-font-", ".png").toFile().apply { deleteOnExit() }
        tmp.writeBytes(byteArrayOf())
        val ok2 = LoadConfigReport()
        cfg.validateFontFile(ok2, tmp.absolutePath)
        assertTrue(ok2.isErrorFree)
    }

    @Test
    fun validateFontFile_flags_missing_preset_and_missing_disk_file() {
        val cfg = ConfigFont()
        val r1 = LoadConfigReport()
        cfg.validateFontFile(r1, "preset://fonts/DOES_NOT_EXIST.png")
        assertTrue(r1.types.contains(LoadConfigErrorType.FILE_NOT_FOUND))

        val r2 = LoadConfigReport()
        cfg.validateFontFile(r2, "/definitely/not/here/${'$'}{System.nanoTime()}.png")
        assertTrue(r2.types.contains(LoadConfigErrorType.FILE_NOT_FOUND))
    }

    @Test
    fun validateBorderFile_accepts_existing_preset_and_real_disk_file() {
        val cfg = ConfigFont()
        val ok = LoadConfigReport()
        cfg.validateBorderFile(ok, "preset://borders/dw3_border.png")
        assertTrue(ok.isErrorFree)

        val tmp = Files.createTempFile("cfg-border-", ".png").toFile().apply { deleteOnExit() }
        tmp.writeBytes(byteArrayOf())
        val ok2 = LoadConfigReport()
        cfg.validateBorderFile(ok2, tmp.absolutePath)
        assertTrue(ok2.isErrorFree)
    }

    @Test
    fun validateBorderFile_flags_missing_preset_and_missing_disk_file() {
        val cfg = ConfigFont()
        val r1 = LoadConfigReport()
        cfg.validateBorderFile(r1, "preset://borders/DOES_NOT_EXIST.png")
        assertTrue(r1.types.contains(LoadConfigErrorType.FILE_NOT_FOUND))

        val r2 = LoadConfigReport()
        cfg.validateBorderFile(r2, "/definitely/not/here/${'$'}{System.nanoTime()}.png")
        assertTrue(r2.types.contains(LoadConfigErrorType.FILE_NOT_FOUND))
    }

    @Test
    fun validateStrings_flags_bad_unknown_char_length() {
        val cfg = ConfigFont()
        val report = LoadConfigReport()
        cfg.validateStrings(report, "2", "3", "AB CDEF", "QQ")
        assertFalse(report.isErrorFree)
        assertTrue(report.types.contains(LoadConfigErrorType.PARSE_ERROR_CHAR))
    }

    @Test
    fun validateStrings_flags_mismatched_key_length_when_unknownChar_is_ok() {
        val cfg = ConfigFont()
        val report = LoadConfigReport()
        // w=2, h=3, w*h=6 but charKey "ABC" length=3 -> hits the length-mismatch branch (line 162).
        cfg.validateStrings(report, "2", "3", "ABC", "A")
        assertFalse(report.isErrorFree)
        assertTrue(report.types.contains(LoadConfigErrorType.VALUE_OUT_OF_RANGE))
    }

    @Test
    fun validateStrings_flags_unknown_char_not_in_key_length_correct() {
        val cfg = ConfigFont()
        val report = LoadConfigReport()
        // width=2, height=2, key length matches (4), but unknownChar "Z" is not in "ABCD"
        cfg.validateStrings(report, "2", "2", "ABCD", "Z")
        assertTrue(report.types.contains(LoadConfigErrorType.VALUE_OUT_OF_RANGE))
    }

    @Test
    fun validateStrings_short_overload_accepts_valid_inputs() {
        val cfg = ConfigFont()
        val report = LoadConfigReport()
        cfg.validateStrings(report, "2", "2", "ABCD", "A")
        assertTrue(report.isErrorFree)
    }

    @Test
    fun validateStrings_long_overload_flags_empty_filenames_and_bad_type() {
        val cfg = ConfigFont()
        val report = LoadConfigReport()
        cfg.validateStrings(report, "", "", "1", "1", "A", "A", "true",
            "1.0", "1.0", "0", "0", "25", "0", "0", "0", "0", "NOT_A_FONT_TYPE")
        assertTrue(report.types.contains(LoadConfigErrorType.MISSING_VALUE))
        assertTrue(report.types.contains(LoadConfigErrorType.PARSE_ERROR_ENUM))
    }

    @Test
    fun load_missing_properties_produces_missing_key_errors() {
        val cfg = ConfigFont()
        val empty = Properties()
        val report = cfg.load(empty, LoadConfigReport())
        assertFalse(report.isErrorFree)
        assertTrue(report.types.contains(LoadConfigErrorType.MISSING_KEY))
    }

    @Test
    fun load_with_validation_failures_does_not_populate_fields() {
        val props = validProps()
        props.setProperty(FontificatorProperties.KEY_FONT_GRID_WIDTH, "not_an_int")
        val cfg = ConfigFont()
        val report = cfg.load(props, LoadConfigReport())
        assertFalse(report.isErrorFree)
        assertNull(cfg.fontFilename)
    }
}
