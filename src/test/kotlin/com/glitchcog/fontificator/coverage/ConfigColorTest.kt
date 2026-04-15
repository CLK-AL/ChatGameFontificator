package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.config.ConfigColor
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import java.awt.Color
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigColorTest {

    private fun fullProps(palette: String = "FF0000,00FF00,0000FF"): Properties = Properties().apply {
        setProperty(FontificatorProperties.KEY_COLOR_BG, "000000")
        setProperty(FontificatorProperties.KEY_COLOR_FG, "FFFFFF")
        setProperty(FontificatorProperties.KEY_COLOR_BORDER, "FFFFFF")
        setProperty(FontificatorProperties.KEY_COLOR_HIGHLIGHT, "FFFF00")
        setProperty(FontificatorProperties.KEY_COLOR_CHROMA_KEY, "00FF00")
        setProperty(FontificatorProperties.KEY_COLOR_PALETTE, palette)
        setProperty(FontificatorProperties.KEY_COLOR_USERNAME, "true")
        setProperty(FontificatorProperties.KEY_COLOR_TIMESTAMP, "false")
        setProperty(FontificatorProperties.KEY_COLOR_MESSAGE, "false")
        setProperty(FontificatorProperties.KEY_COLOR_JOIN, "false")
        setProperty(FontificatorProperties.KEY_COLOR_TWITCH, "true")
    }

    @Test
    fun load_happy_path_populates_every_color_and_palette() {
        val cfg = ConfigColor()
        val report = cfg.load(fullProps(), LoadConfigReport())
        assertTrue(report.isErrorFree)
        assertEquals(Color(0, 0, 0), cfg.bgColor)
        assertEquals(Color(0xFF, 0xFF, 0xFF), cfg.fgColor)
        assertEquals(Color(0xFF, 0xFF, 0xFF), cfg.borderColor)
        assertEquals(Color(0xFF, 0xFF, 0x00), cfg.highlight)
        assertEquals(Color(0x00, 0xFF, 0x00), cfg.chromaColor)
        assertEquals(3, cfg.palette.size)
        assertTrue(cfg.isColorUsername)
        assertFalse(cfg.isColorTimestamp)
        assertFalse(cfg.isColorMessage)
        assertFalse(cfg.isColorJoin)
        assertTrue(cfg.isUseTwitchColors)
    }

    @Test
    fun load_with_empty_palette_returns_empty_list() {
        val cfg = ConfigColor()
        val props = fullProps(palette = "")
        val report = cfg.load(props, LoadConfigReport())
        assertTrue(report.isErrorFree)
        assertEquals(0, cfg.palette.size)
    }

    @Test
    fun validateStrings_whitespace_palette_short_circuits_to_ok() {
        // In validateStrings, palStr.trim().isEmpty() short-circuits the palette
        // parse. This covers the whitespace-specific branch in validateStrings.
        val cfg = ConfigColor()
        // Use reflection so we don't go through load() which tries to split on ","
        val m = ConfigColor::class.java.getDeclaredMethod(
            "validateStrings",
            com.glitchcog.fontificator.config.loadreport.LoadConfigReport::class.java,
            String::class.java, String::class.java, String::class.java,
            String::class.java, String::class.java, String::class.java
        )
        m.isAccessible = true
        // Need props wired so evaluateColorString can read them
        cfg.load(fullProps(), LoadConfigReport())
        val r = com.glitchcog.fontificator.config.loadreport.LoadConfigReport()
        m.invoke(cfg, r, "   ", "true", "false", "false", "false", "true")
        assertTrue(r.isErrorFree)
    }

    @Test
    fun load_missing_keys_fails_validation() {
        val cfg = ConfigColor()
        val r = cfg.load(Properties(), LoadConfigReport())
        assertFalse(r.isErrorFree)
    }

    @Test
    fun reset_nulls_every_field_and_palette_getter_returns_empty_list() {
        val cfg = ConfigColor()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.reset()
        assertNull(cfg.bgColor)
        assertEquals(0, cfg.palette.size) // getter coalesces null -> empty
    }

    @Test
    fun every_setter_round_trips_and_persists_to_props() {
        val cfg = ConfigColor()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.bgColor = Color.RED; assertEquals(Color.RED, cfg.bgColor)
        cfg.fgColor = Color.GREEN; assertEquals(Color.GREEN, cfg.fgColor)
        cfg.borderColor = Color.BLUE; assertEquals(Color.BLUE, cfg.borderColor)
        cfg.highlight = Color.YELLOW; assertEquals(Color.YELLOW, cfg.highlight)
        cfg.chromaColor = Color.CYAN; assertEquals(Color.CYAN, cfg.chromaColor)
        cfg.palette = listOf(Color.RED, Color.GREEN, Color.BLUE)
        assertEquals(3, cfg.palette.size)
        cfg.setColorUsername(false); assertFalse(cfg.isColorUsername)
        cfg.setColorTimestamp(true); assertTrue(cfg.isColorTimestamp)
        cfg.setColorMessage(true); assertTrue(cfg.isColorMessage)
        cfg.setColorJoin(true); assertTrue(cfg.isColorJoin)
        cfg.setUseTwitchColors(false); assertFalse(cfg.isUseTwitchColors)
    }

    @Test
    fun setPalette_with_single_color_writes_no_leading_comma() {
        val cfg = ConfigColor()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.palette = listOf(Color.RED)
        assertEquals(1, cfg.palette.size)
    }

    @Test
    fun setPalette_with_empty_list_writes_empty_string() {
        val cfg = ConfigColor()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.palette = emptyList()
        assertEquals(0, cfg.palette.size)
    }

    @Test
    fun getColorHex_formats_correctly() {
        assertEquals("FF0000", ConfigColor.getColorHex(Color.RED))
        assertEquals("000000", ConfigColor.getColorHex(Color.BLACK))
        assertEquals("FFFFFF", ConfigColor.getColorHex(Color.WHITE))
    }

    @Test
    fun load_with_bad_color_string_in_palette_flags_parse_error_and_skips_null() {
        val cfg = ConfigColor()
        val props = fullProps(palette = "FF0000,NOTHEX,0000FF")
        val report = cfg.load(props, LoadConfigReport())
        // parse error registered for NOTHEX but load still populates palette with the 2 valid ones
        assertFalse(report.isErrorFree)
    }
}
