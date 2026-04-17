package com.glitchcog.fontificator.core

import com.glitchcog.fontificator.config.ConfigColor as JavaConfigColor
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import java.awt.Color
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stage S4 differential-parity gate for `ConfigColor`.
 *
 * Constructs a Java `ConfigColor` via its `load(Properties, ...)`
 * path, converts it to the commonMain port via
 * `JavaLegacyAdapter.configColorFromJava`, and asserts field-by-field
 * parity.
 */
class ConfigColorJvmParityTest {

    private fun buildProperties(
        bg: String = "000000",
        fg: String = "FFFFFF",
        border: String = "FFFFFF",
        highlight: String = "6699FF",
        chromaKey: String = "00FF00",
        palette: String = "F7977A,FDC68A,FFF79A,A2D39C,6ECFF6,A187BE,F6989D",
        colorUsername: Boolean = true,
        colorTimestamp: Boolean = false,
        colorMessage: Boolean = false,
        colorJoin: Boolean = false,
        useTwitchColors: Boolean = false,
    ): Properties {
        val p = Properties()
        p.setProperty(FontificatorProperties.KEY_COLOR_BG, bg)
        p.setProperty(FontificatorProperties.KEY_COLOR_FG, fg)
        p.setProperty(FontificatorProperties.KEY_COLOR_BORDER, border)
        p.setProperty(FontificatorProperties.KEY_COLOR_HIGHLIGHT, highlight)
        p.setProperty(FontificatorProperties.KEY_COLOR_CHROMA_KEY, chromaKey)
        p.setProperty(FontificatorProperties.KEY_COLOR_PALETTE, palette)
        p.setProperty(FontificatorProperties.KEY_COLOR_USERNAME, colorUsername.toString())
        p.setProperty(FontificatorProperties.KEY_COLOR_TIMESTAMP, colorTimestamp.toString())
        p.setProperty(FontificatorProperties.KEY_COLOR_MESSAGE, colorMessage.toString())
        p.setProperty(FontificatorProperties.KEY_COLOR_JOIN, colorJoin.toString())
        p.setProperty(FontificatorProperties.KEY_COLOR_TWITCH, useTwitchColors.toString())
        return p
    }

    private fun loadJavaConfig(props: Properties): JavaConfigColor {
        val javaConfig = JavaConfigColor()
        val report = LoadConfigReport()
        javaConfig.load(props, report)
        assertTrue(report.isErrorFree, "Java load had errors: ${report.messages}")
        return javaConfig
    }

    private fun assertColorParity(javaColor: Color, kotlinColor: ColorRGBA, label: String) {
        assertEquals(javaColor.red, kotlinColor.r, "$label red mismatch")
        assertEquals(javaColor.green, kotlinColor.g, "$label green mismatch")
        assertEquals(javaColor.blue, kotlinColor.b, "$label blue mismatch")
    }

    private fun assertFieldParity(java: JavaConfigColor, kotlin: ConfigColor) {
        assertColorParity(java.bgColor, kotlin.bgColor, "bgColor")
        assertColorParity(java.fgColor, kotlin.fgColor, "fgColor")
        assertColorParity(java.borderColor, kotlin.borderColor, "borderColor")
        assertColorParity(java.highlight, kotlin.highlight, "highlight")
        assertColorParity(java.chromaColor, kotlin.chromaColor, "chromaColor")
        assertEquals(java.palette.size, kotlin.palette.size, "palette size mismatch")
        for (i in java.palette.indices) {
            assertColorParity(java.palette[i], kotlin.palette[i], "palette[$i]")
        }
        assertEquals(java.isColorUsername, kotlin.colorUsername, "colorUsername mismatch")
        assertEquals(java.isColorTimestamp, kotlin.colorTimestamp, "colorTimestamp mismatch")
        assertEquals(java.isColorMessage, kotlin.colorMessage, "colorMessage mismatch")
        assertEquals(java.isColorJoin, kotlin.colorJoin, "colorJoin mismatch")
        assertEquals(java.isUseTwitchColors, kotlin.useTwitchColors, "useTwitchColors mismatch")
    }

    // ---- parity: default config ---------------------------------------

    @Test
    fun `default config -- adapter produces field-exact parity`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configColorFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: every field set to non-default values -----------------

    @Test
    fun `non-default fields -- adapter produces field-exact parity`() {
        val props = buildProperties(
            bg = "112233",
            fg = "445566",
            border = "778899",
            highlight = "AABBCC",
            chromaKey = "DDEEFF",
            palette = "FF0000,00FF00",
            colorUsername = false,
            colorTimestamp = true,
            colorMessage = true,
            colorJoin = true,
            useTwitchColors = true,
        )
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configColorFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: empty palette ----------------------------------------

    @Test
    fun `empty palette -- adapter produces field-exact parity`() {
        val props = buildProperties(palette = "")
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configColorFromJava(java)
        assertFieldParity(java, kotlin)
        assertEquals(0, kotlin.palette.size)
    }

    // ---- parity: setter mutations -------------------------------------

    @Test
    fun `Java setter mutations are reflected by adapter`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)

        java.bgColor = Color(0x11, 0x22, 0x33)
        java.fgColor = Color(0x44, 0x55, 0x66)
        java.borderColor = Color(0x77, 0x88, 0x99)
        java.highlight = Color(0xAA, 0xBB, 0xCC)
        java.chromaColor = Color(0xDD, 0xEE, 0xFF)
        java.setColorUsername(false)
        java.setColorTimestamp(true)
        java.setColorMessage(true)
        java.setColorJoin(true)
        java.setUseTwitchColors(true)

        val kotlin = JavaLegacyAdapter.configColorFromJava(java)
        assertFieldParity(java, kotlin)

        // Spot-check
        assertEquals(0x11, kotlin.bgColor.r)
        assertEquals(false, kotlin.colorUsername)
        assertEquals(true, kotlin.colorTimestamp)
    }

    // ---- parity: hex format matches Java getColorHex ------------------

    @Test
    fun `ColorRGBA toHex matches Java getColorHex`() {
        val colors = listOf(
            Color(0, 0, 0),
            Color(255, 255, 255),
            Color(0xFF, 0x00, 0x80),
            Color(0x12, 0x34, 0x56),
        )
        for (javaColor in colors) {
            val kotlinColor = JavaLegacyAdapter.colorFromJava(javaColor)
            val javaHex = JavaConfigColor.getColorHex(javaColor)
            assertEquals(javaHex, kotlinColor.toHex(), "Hex mismatch for $javaColor")
        }
    }
}
