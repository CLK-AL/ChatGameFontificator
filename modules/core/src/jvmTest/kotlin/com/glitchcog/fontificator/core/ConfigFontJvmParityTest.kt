package com.glitchcog.fontificator.core

import com.glitchcog.fontificator.config.ConfigFont as JavaConfigFont
import com.glitchcog.fontificator.config.FontType as JavaFontType
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stage S4 differential-parity gate for `ConfigFont`.
 *
 * Constructs a Java `ConfigFont` via its `load(Properties, ...)` path,
 * converts it to the commonMain port via `JavaLegacyAdapter.configFontFromJava`,
 * and asserts field-by-field parity.
 */
class ConfigFontJvmParityTest {

    // 8 x 12 = 96 characters, matching the standard NORMAL_ASCII_KEY
    private val standardCharKey: String =
        " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F"

    /**
     * Build a Java `Properties` with every font key populated.
     */
    private fun buildProperties(
        fontFile: String = "preset://fonts/dw3_font.png",
        borderFile: String = "preset://borders/dw3_border.png",
        gridWidth: Int = 8,
        gridHeight: Int = 12,
        fontScale: Float = 2.0f,
        borderScale: Float = 3.0f,
        borderInsetX: Int = 1,
        borderInsetY: Int = 1,
        spaceWidth: Int = 25,
        baselineOffset: Int = 0,
        characterKey: String = standardCharKey,
        unknownChar: Char = '\u007F',
        extendedCharEnabled: Boolean = true,
        lineSpacing: Int = 2,
        charSpacing: Int = 0,
        messageSpacing: Int = 0,
        fontType: JavaFontType = JavaFontType.FIXED_WIDTH,
    ): Properties {
        val p = Properties()
        p.setProperty(FontificatorProperties.KEY_FONT_FILE_FONT, fontFile)
        p.setProperty(FontificatorProperties.KEY_FONT_FILE_BORDER, borderFile)
        p.setProperty(FontificatorProperties.KEY_FONT_GRID_WIDTH, gridWidth.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_GRID_HEIGHT, gridHeight.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_SCALE, fontScale.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_BORDER_SCALE, borderScale.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_X, borderInsetX.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_Y, borderInsetY.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_SPACE_WIDTH, spaceWidth.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET, baselineOffset.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_CHARACTERS, characterKey)
        p.setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, unknownChar.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_EXTENDED_CHAR, extendedCharEnabled.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_SPACING_LINE, lineSpacing.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_SPACING_CHAR, charSpacing.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_SPACING_MESSAGE, messageSpacing.toString())
        p.setProperty(FontificatorProperties.KEY_FONT_TYPE, fontType.name)
        return p
    }

    /**
     * Load a Java `ConfigFont` from properties and assert it loaded cleanly.
     */
    private fun loadJavaConfig(props: Properties): JavaConfigFont {
        val javaConfig = JavaConfigFont()
        val report = LoadConfigReport()
        javaConfig.load(props, report)
        assertTrue(report.isErrorFree, "Java load had errors: ${report.messages}")
        return javaConfig
    }

    /**
     * Assert every field of the Kotlin port matches the Java original.
     */
    private fun assertFieldParity(java: JavaConfigFont, kotlin: ConfigFont) {
        assertEquals(java.fontFilename, kotlin.fontFilename, "fontFilename mismatch")
        assertEquals(java.borderFilename, kotlin.borderFilename, "borderFilename mismatch")
        assertEquals(java.gridWidth, kotlin.gridWidth, "gridWidth mismatch")
        assertEquals(java.gridHeight, kotlin.gridHeight, "gridHeight mismatch")
        assertEquals(java.fontScale, kotlin.fontScale, "fontScale mismatch")
        assertEquals(java.borderScale, kotlin.borderScale, "borderScale mismatch")
        assertEquals(java.borderInsetX, kotlin.borderInsetX, "borderInsetX mismatch")
        assertEquals(java.borderInsetY, kotlin.borderInsetY, "borderInsetY mismatch")
        assertEquals(java.spaceWidth, kotlin.spaceWidth, "spaceWidth mismatch")
        assertEquals(java.baselineOffset, kotlin.baselineOffset, "baselineOffset mismatch")
        assertEquals(java.characterKey, kotlin.characterKey, "characterKey mismatch")
        assertEquals(java.unknownChar, kotlin.unknownChar, "unknownChar mismatch")
        assertEquals(java.isExtendedCharEnabled, kotlin.extendedCharEnabled, "extendedCharEnabled mismatch")
        assertEquals(java.lineSpacing, kotlin.lineSpacing, "lineSpacing mismatch")
        assertEquals(java.charSpacing, kotlin.charSpacing, "charSpacing mismatch")
        assertEquals(java.messageSpacing, kotlin.messageSpacing, "messageSpacing mismatch")
        assertEquals(java.fontType.name, kotlin.fontType.name, "fontType mismatch")
    }

    // ---- parity: default config ---------------------------------------

    @Test
    fun `default config -- adapter produces field-exact parity`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configFontFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: every field set to non-default values -----------------

    @Test
    fun `non-default fields -- adapter produces field-exact parity`() {
        // 10 x 10 = 100 chars
        val charKey = "ABCDEFGHIJ" +
            "KLMNOPQRST" +
            "UVWXYZabcd" +
            "efghijklmn" +
            "opqrstuvwx" +
            "yz01234567" +
            "89!@#\$%^&*" +
            "()+=-_<>./" +
            "[]{}|\\;:'\"" +
            "~` ?,ABCDE"
        val props = buildProperties(
            fontFile = "/tmp/custom_font.png",
            borderFile = "/tmp/custom_border.png",
            gridWidth = 10,
            gridHeight = 10,
            fontScale = 1.5f,
            borderScale = 2.5f,
            borderInsetX = -10,
            borderInsetY = 20,
            spaceWidth = 40,
            baselineOffset = 5,
            characterKey = charKey,
            unknownChar = 'A',
            extendedCharEnabled = false,
            lineSpacing = -3,
            charSpacing = 4,
            messageSpacing = 12,
            fontType = JavaFontType.VARIABLE_WIDTH,
        )
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configFontFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- C1 regression: baselineOffset survives adapter ---------------

    @Test
    fun `C1 regression -- baselineOffset survives Java-to-Kotlin adapter`() {
        val props = buildProperties(baselineOffset = 7)
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configFontFromJava(java)
        assertEquals(7, java.baselineOffset, "Java baselineOffset")
        assertEquals(7, kotlin.baselineOffset, "Kotlin baselineOffset")
    }

    @Test
    fun `C1 regression -- negative baselineOffset survives adapter`() {
        val props = buildProperties(baselineOffset = -15)
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configFontFromJava(java)
        assertEquals(-15, kotlin.baselineOffset)
    }

    // ---- parity: setter round-trip ------------------------------------

    @Test
    fun `Java setter mutations are reflected by adapter`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)

        // Mutate via Java setters
        java.fontFilename = "/tmp/mutated_font.png"
        java.borderFilename = "/tmp/mutated_border.png"
        java.baselineOffset = 12
        java.spaceWidth = 42
        java.fontType = JavaFontType.VARIABLE_WIDTH

        val kotlin = JavaLegacyAdapter.configFontFromJava(java)
        assertEquals("/tmp/mutated_font.png", kotlin.fontFilename)
        assertEquals("/tmp/mutated_border.png", kotlin.borderFilename)
        assertEquals(12, kotlin.baselineOffset)
        assertEquals(42, kotlin.spaceWidth)
        assertEquals(FontType.VARIABLE_WIDTH, kotlin.fontType)
    }

    // ---- parity: VARIABLE_WIDTH fontType ------------------------------

    @Test
    fun `VARIABLE_WIDTH fontType parity`() {
        val props = buildProperties(fontType = JavaFontType.VARIABLE_WIDTH)
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configFontFromJava(java)
        assertEquals(FontType.VARIABLE_WIDTH, kotlin.fontType)
        assertEquals(java.fontType.name, kotlin.fontType.name)
    }
}
