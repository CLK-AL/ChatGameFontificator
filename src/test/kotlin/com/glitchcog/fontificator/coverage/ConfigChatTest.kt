package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.config.ConfigChat
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import java.awt.Rectangle
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 100% line + branch coverage driver for ConfigChat. */
class ConfigChatTest {

    private fun fullProps(): Properties = Properties().apply {
        // Pixel content-pane dims (legacy window dims also validated through load).
        setProperty(FontificatorProperties.KEY_CHAT_WIDTH, "640")
        setProperty(FontificatorProperties.KEY_CHAT_HEIGHT, "480")
        setProperty(FontificatorProperties.KEY_CHAT_SCROLL, "true")
        setProperty(FontificatorProperties.KEY_CHAT_RESIZABLE, "true")
        setProperty(FontificatorProperties.KEY_CHAT_POSITION, "true")
        setProperty(FontificatorProperties.KEY_CHAT_POSITION_X, "100")
        setProperty(FontificatorProperties.KEY_CHAT_POSITION_Y, "150")
        setProperty(FontificatorProperties.KEY_CHAT_FROM_BOTTOM, "false")
        setProperty(FontificatorProperties.KEY_CHAT_CHROMA_ENABLED, "true")
        setProperty(FontificatorProperties.KEY_CHAT_INVERT_CHROMA, "false")
        setProperty(FontificatorProperties.KEY_CHAT_REVERSE_SCROLLING, "false")
        setProperty(FontificatorProperties.KEY_CHAT_CHROMA_LEFT, "2")
        setProperty(FontificatorProperties.KEY_CHAT_CHROMA_TOP, "3")
        setProperty(FontificatorProperties.KEY_CHAT_CHROMA_RIGHT, "4")
        setProperty(FontificatorProperties.KEY_CHAT_CHROMA_BOTTOM, "5")
        setProperty(FontificatorProperties.KEY_CHAT_CHROMA_CORNER, "8")
        setProperty(FontificatorProperties.KEY_CHAT_ALWAYS_ON_TOP, "true")
        setProperty(FontificatorProperties.KEY_CHAT_ANTIALIAS, "true")
    }

    @Test
    fun load_happy_path_populates_every_field() {
        val cfg = ConfigChat()
        val report = cfg.load(fullProps(), LoadConfigReport())
        assertTrue(report.isErrorFree, report.messages.joinToString())
        assertEquals(640, cfg.width)
        assertEquals(480, cfg.height)
        assertTrue(cfg.isScrollable)
        assertTrue(cfg.isResizable)
        assertTrue(cfg.isRememberPosition)
        assertEquals(100, cfg.chatWindowPositionX)
        assertEquals(150, cfg.chatWindowPositionY)
        assertFalse(cfg.isChatFromBottom)
        assertTrue(cfg.isChromaEnabled)
        assertFalse(cfg.isChromaInvert)
        assertFalse(cfg.isReverseScrolling)
        assertEquals(8, cfg.chromaCornerRadius)
        assertTrue(cfg.isAlwaysOnTop)
        assertTrue(cfg.isAntiAlias)
        val cb = cfg.chromaBorder
        assertEquals(2, cb.x); assertEquals(3, cb.y); assertEquals(4, cb.width); assertEquals(5, cb.height)
    }

    @Test
    fun load_with_legacy_window_size_populates_windowWidth_and_Height() {
        // Providing BOTH normal dims (required by baseValidation) AND legacy dims
        // triggers the legacy branch in load(). With legacy populated, width/height
        // stay null and windowWidth/Height are set instead.
        val props = fullProps().apply {
            setProperty(FontificatorProperties.KEY_CHAT_WINDOW_WIDTH, "700")
            setProperty(FontificatorProperties.KEY_CHAT_WINDOW_HEIGHT, "500")
        }
        val cfg = ConfigChat()
        val report = cfg.load(props, LoadConfigReport())
        assertTrue(report.isErrorFree)
        assertEquals(700, cfg.windowWidth)
        assertEquals(500, cfg.windowHeight)
        // The load() branch `else if (widthStr != null...)` did not execute,
        // so width and height stay null even though KEY_CHAT_WIDTH is present.
        assertNull(cfg.width)
        assertNull(cfg.height)
    }

    @Test
    fun load_with_missing_keys_fails_validation() {
        val cfg = ConfigChat()
        val report = cfg.load(Properties(), LoadConfigReport())
        assertFalse(report.isErrorFree)
    }

    @Test
    fun reset_nulls_all_fields() {
        val cfg = ConfigChat()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.reset()
        assertNull(cfg.width)
        assertNull(cfg.height)
        assertNull(cfg.chromaBorder)
    }

    @Test
    fun chat_window_positions_are_zero_when_null() {
        val cfg = ConfigChat()
        // no load — positions stay null, accessors should coalesce to 0
        assertEquals(0, cfg.chatWindowPositionX)
        assertEquals(0, cfg.chatWindowPositionY)
    }

    @Test
    fun every_setter_round_trips_and_persists_to_props() {
        val cfg = ConfigChat()
        cfg.load(fullProps(), LoadConfigReport()) // initializes props

        cfg.isScrollable = false; assertFalse(cfg.isScrollable)
        cfg.isResizable = false; assertFalse(cfg.isResizable)
        cfg.isRememberPosition = false; assertFalse(cfg.isRememberPosition)
        cfg.chatWindowPositionX = 9; assertEquals(9, cfg.chatWindowPositionX)
        cfg.chatWindowPositionY = 11; assertEquals(11, cfg.chatWindowPositionY)
        cfg.isChatFromBottom = true; assertTrue(cfg.isChatFromBottom)
        cfg.isReverseScrolling = true; assertTrue(cfg.isReverseScrolling)
        cfg.width = 1024; assertEquals(1024, cfg.width)
        cfg.height = 768; assertEquals(768, cfg.height)
        cfg.windowWidth = 999; assertEquals(999, cfg.windowWidth)
        cfg.windowHeight = 888; assertEquals(888, cfg.windowHeight)
        cfg.isAlwaysOnTop = false; assertFalse(cfg.isAlwaysOnTop)
        cfg.isAntiAlias = false; assertFalse(cfg.isAntiAlias)
        cfg.isChromaEnabled = false; assertFalse(cfg.isChromaEnabled)
        cfg.isChromaInvert = true; assertTrue(cfg.isChromaInvert)
        cfg.chromaCornerRadius = 32; assertEquals(32, cfg.chromaCornerRadius)
        cfg.setChromaBorder(10, 20, 30, 40)
        val cb = cfg.chromaBorder
        assertEquals(10, cb.x); assertEquals(20, cb.y); assertEquals(30, cb.width); assertEquals(40, cb.height)
        // Rectangle overload
        cfg.chromaBorder = Rectangle(1, 2, 3, 4)
        assertEquals(1, cfg.chromaBorder.x)
    }

    @Test
    fun clearLegacyWindowSize_removes_legacy_props() {
        val cfg = ConfigChat()
        val props = fullProps().apply {
            setProperty(FontificatorProperties.KEY_CHAT_WINDOW_WIDTH, "200")
            setProperty(FontificatorProperties.KEY_CHAT_WINDOW_HEIGHT, "300")
        }
        cfg.load(props, LoadConfigReport())
        cfg.windowWidth = 111 // ensure fields can be non-null
        cfg.windowHeight = 222
        cfg.clearLegacyWindowSize()
        assertNull(cfg.windowWidth)
        assertNull(cfg.windowHeight)
    }

    @Test
    fun validateDimStrings_and_validateChromaDimStrings_direct() {
        val cfg = ConfigChat()
        val r1 = LoadConfigReport()
        cfg.validateDimStrings(r1, "10", "20")
        assertTrue(r1.isErrorFree)
        val r2 = LoadConfigReport()
        cfg.validateChromaDimStrings(r2, "1", "2", "3", "4")
        assertTrue(r2.isErrorFree)
    }

    @Test
    fun validateStrings_with_null_dims_falls_through_to_chroma_validation() {
        val cfg = ConfigChat()
        val r = LoadConfigReport()
        // Both normal dims null and window dims null -> only chroma+corners validated
        cfg.validateStrings(r, null, null, null, null, "1", "2", "3", "4", "5",
            "true", "false", "true", "true", "false", "true", "false", "true")
        assertTrue(r.isErrorFree)
    }

    @Test
    fun validateStrings_with_only_window_dims_uses_legacy_branch() {
        val cfg = ConfigChat()
        val r = LoadConfigReport()
        // width null -> falls to else-if with window dims
        cfg.validateStrings(r, null, null, "100", "200", "1", "2", "3", "4", "5",
            "true", "false", "true", "true", "false", "true", "false", "true")
        assertTrue(r.isErrorFree)
    }

    @Test
    fun validateStrings_with_null_width_but_non_null_height_takes_else_branch() {
        val cfg = ConfigChat()
        val r = LoadConfigReport()
        // widthStr null but heightStr set: short-circuit `widthStr != null` is false
        cfg.validateStrings(r, null, "10", null, null, "1", "2", "3", "4", "5",
            "true", "false", "true", "true", "false", "true", "false", "true")
        assertTrue(r.isErrorFree)
    }

    @Test
    fun validateStrings_width_non_null_height_null_short_circuits_first_condition() {
        val cfg = ConfigChat()
        val r = LoadConfigReport()
        // widthStr non-null, heightStr null -> first && short-circuits on right half
        cfg.validateStrings(r, "10", null, null, null, "1", "2", "3", "4", "5",
            "true", "false", "true", "true", "false", "true", "false", "true")
        assertTrue(r.isErrorFree)
    }

    @Test
    fun validateStrings_window_width_non_null_window_height_null_short_circuits() {
        val cfg = ConfigChat()
        val r = LoadConfigReport()
        // widthStr null -> else-if path; windowWidth non-null, windowHeight null
        cfg.validateStrings(r, null, null, "10", null, "1", "2", "3", "4", "5",
            "true", "false", "true", "true", "false", "true", "false", "true")
        assertTrue(r.isErrorFree)
    }

    @Test
    fun load_with_width_only_no_legacy_populates_width_height_branch() {
        // This is the else-if `widthStr != null && heightStr != null` branch in load
        val cfg = ConfigChat()
        val r = cfg.load(fullProps(), LoadConfigReport())
        assertTrue(r.isErrorFree)
        assertEquals(640, cfg.width)
        assertEquals(480, cfg.height)
    }

    @Test
    fun load_baseValidation_ok_but_validateStrings_fails_takes_skip_branch() {
        // baseValidation passes (all keys present, non-empty), but
        // validateBooleanStrings flags the KEY_CHAT_SCROLL as bad -> the
        // `if (report.isErrorFree())` after validateStrings is false.
        val p = fullProps().apply { setProperty(FontificatorProperties.KEY_CHAT_SCROLL, "maybe") }
        val cfg = ConfigChat()
        val r = cfg.load(p, LoadConfigReport())
        assertFalse(r.isErrorFree)
        assertNull(cfg.width) // not populated due to skip
    }

    @Test
    fun load_with_window_width_only_does_not_take_legacy_branch() {
        // windowWidthStr non-null, windowHeightStr null -> first && short-circuit
        // evaluates right half as false; the load() drops to the else-if and uses
        // the normal width/height. Exercises the inner short-circuit branch (line 199).
        val p = fullProps().apply { setProperty(FontificatorProperties.KEY_CHAT_WINDOW_WIDTH, "777") }
        val cfg = ConfigChat()
        val r = cfg.load(p, LoadConfigReport())
        assertTrue(r.isErrorFree)
        // Legacy window sizes not populated because windowHeight missing
        assertNull(cfg.windowWidth)
        assertEquals(640, cfg.width)
    }
}
