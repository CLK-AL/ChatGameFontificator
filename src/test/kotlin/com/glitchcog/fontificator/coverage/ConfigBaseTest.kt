package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.config.Config
import com.glitchcog.fontificator.config.ConfigFont
import com.glitchcog.fontificator.config.FontType
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigErrorType
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import java.awt.Color
import java.lang.reflect.Method
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exhaustively exercise `Config` — the abstract base class — via
 * `ConfigFont` + a local subclass, to reach 100% on the shared
 * validate/evaluate helpers.
 */
class ConfigBaseTest {

    /** Trivial subclass so we can call the protected helpers via reflection. */
    private class TestHarness : Config() {
        override fun load(props: Properties?, report: LoadConfigReport?): LoadConfigReport = report!!
        override fun reset() {}
        fun setPropsDirect(p: Properties) { this.props = p }
    }

    /** Use reflection to reach the protected helpers. */
    private fun methodOf(name: String, vararg types: Class<*>?): Method {
        @Suppress("UNCHECKED_CAST")
        val m = Config::class.java.getDeclaredMethod(name, *(types as Array<Class<*>?>))
        m.isAccessible = true
        return m
    }

    @Test
    fun baseValidation_missing_keys_and_missing_values_and_space_unknown_char() {
        val t = TestHarness()
        // Properties intentionally missing KEY_FONT_UNKNOWN_CHAR and KEY_MESSAGE_CONTENT_BREAK + missing key + empty key
        val props = Properties()
        // A key with empty value (not one of the space-preserving keys) -> MISSING_VALUE
        props.setProperty(FontificatorProperties.KEY_FONT_SCALE, " ")
        // A key whose value is allowed to be a single space / empty (branch: space-preserving key)
        props.setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, " ")
        props.setProperty(FontificatorProperties.KEY_MESSAGE_CONTENT_BREAK, ": ")
        val keys = arrayOf(
            FontificatorProperties.KEY_FONT_SCALE,
            FontificatorProperties.KEY_FONT_UNKNOWN_CHAR,
            FontificatorProperties.KEY_MESSAGE_CONTENT_BREAK,
            FontificatorProperties.KEY_CHAT_WIDTH // completely missing -> MISSING_KEY
        )
        val report = LoadConfigReport()
        val m = methodOf("baseValidation", Properties::class.java, Array<String>::class.java, LoadConfigReport::class.java)
        m.invoke(t, props, keys, report)

        assertTrue(report.types.contains(LoadConfigErrorType.MISSING_KEY))
        assertTrue(report.types.contains(LoadConfigErrorType.MISSING_VALUE))
    }

    @Test
    fun baseValidation_with_empty_unknown_char_value_flags_missing_value() {
        val t = TestHarness()
        val props = Properties()
        // Truly empty string (not space) for the space-preserving key — MUST flag MISSING_VALUE
        props.setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, "")
        val keys = arrayOf(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR)
        val report = LoadConfigReport()
        val m = methodOf("baseValidation", Properties::class.java, Array<String>::class.java, LoadConfigReport::class.java)
        m.invoke(t, props, keys, report)
        assertTrue(report.types.contains(LoadConfigErrorType.MISSING_VALUE))
    }

    @Test
    fun validateIntegerString_null_flags_missing_and_bad_int_flags_parse() {
        val t = TestHarness()
        val m = methodOf("validateIntegerString", String::class.java, String::class.java, LoadConfigReport::class.java)
        val r1 = LoadConfigReport()
        m.invoke(t, "k", null, r1)
        assertTrue(r1.types.contains(LoadConfigErrorType.MISSING_VALUE))
        val r2 = LoadConfigReport()
        m.invoke(t, "k", "xx", r2)
        assertTrue(r2.types.contains(LoadConfigErrorType.PARSE_ERROR_INT))
    }

    @Test
    fun validateIntegerWithLimitString_below_minimum_and_above_maximum() {
        val t = TestHarness()
        val m = methodOf("validateIntegerWithLimitString", String::class.java, String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, LoadConfigReport::class.java)
        val r1 = LoadConfigReport()
        m.invoke(t, "k", "-5", 0, 10, r1)
        assertTrue(r1.types.contains(LoadConfigErrorType.VALUE_OUT_OF_RANGE))
        val r2 = LoadConfigReport()
        m.invoke(t, "k", "50", 0, 10, r2)
        assertTrue(r2.types.contains(LoadConfigErrorType.VALUE_OUT_OF_RANGE))
        val r3 = LoadConfigReport()
        m.invoke(t, "k", "5", 0, 10, r3)
        assertTrue(r3.isErrorFree)
    }

    @Test
    fun validateIntegerWithLimitString_single_arg_overload_delegates_to_MAX_VALUE() {
        val t = TestHarness()
        val m = methodOf("validateIntegerWithLimitString", String::class.java, String::class.java, Int::class.javaPrimitiveType, LoadConfigReport::class.java)
        val r = LoadConfigReport()
        m.invoke(t, "k", "5", 0, r)
        assertTrue(r.isErrorFree)
    }

    @Test
    fun validateFloatString_null_missing_and_bad_format_flags_parse() {
        val t = TestHarness()
        val m = methodOf("validateFloatString", String::class.java, String::class.java, LoadConfigReport::class.java)
        val r1 = LoadConfigReport()
        m.invoke(t, "k", null, r1)
        assertTrue(r1.types.contains(LoadConfigErrorType.MISSING_VALUE))
        val r2 = LoadConfigReport()
        m.invoke(t, "k", "nope", r2)
        assertTrue(r2.types.contains(LoadConfigErrorType.PARSE_ERROR_FLOAT))
    }

    @Test
    fun validateFloatWithLimitString_below_above_and_in_range() {
        val t = TestHarness()
        val m = methodOf("validateFloatWithLimitString", String::class.java, String::class.java, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, LoadConfigReport::class.java)
        val below = LoadConfigReport()
        m.invoke(t, "k", "-1.0", 0.0f, 10.0f, below)
        assertTrue(below.types.contains(LoadConfigErrorType.VALUE_OUT_OF_RANGE))
        val above = LoadConfigReport()
        m.invoke(t, "k", "100.0", 0.0f, 10.0f, above)
        assertTrue(above.types.contains(LoadConfigErrorType.VALUE_OUT_OF_RANGE))
        val okay = LoadConfigReport()
        m.invoke(t, "k", "5.0", 0.0f, 10.0f, okay)
        assertTrue(okay.isErrorFree)
    }

    @Test
    fun evaluateBooleanString_value_variant_hits_true_false_and_bad() {
        val t = TestHarness()
        val m = methodOf("evaluateBooleanString", String::class.java, LoadConfigReport::class.java)
        val r1 = LoadConfigReport()
        assertEquals(true, m.invoke(t, "true", r1))
        assertEquals(true, m.invoke(t, "yes", r1))
        assertEquals(true, m.invoke(t, "+", r1))
        assertEquals(true, m.invoke(t, "t", r1))
        assertEquals(true, m.invoke(t, "1", r1))
        assertEquals(false, m.invoke(t, "false", r1))
        assertEquals(false, m.invoke(t, "no", r1))
        assertEquals(false, m.invoke(t, "-", r1))
        assertEquals(false, m.invoke(t, "f", r1))
        assertEquals(false, m.invoke(t, "0", r1))
        assertTrue(r1.isErrorFree)
        val bad = LoadConfigReport()
        assertNull(m.invoke(t, "maybe", bad))
        assertTrue(bad.types.contains(LoadConfigErrorType.PARSE_ERROR_BOOL))
    }

    @Test
    fun evaluateBooleanString_props_variant_key_missing_and_bad_and_good() {
        val t = TestHarness()
        val m = methodOf("evaluateBooleanString", Properties::class.java, String::class.java, LoadConfigReport::class.java)
        val props = Properties()
        val rMissing = LoadConfigReport()
        assertNull(m.invoke(t, props, "K", rMissing))
        assertTrue(rMissing.types.contains(LoadConfigErrorType.MISSING_KEY))

        props.setProperty("K", "maybe")
        val rBad = LoadConfigReport()
        assertNull(m.invoke(t, props, "K", rBad))
        assertTrue(rBad.types.contains(LoadConfigErrorType.PARSE_ERROR_BOOL))

        props.setProperty("K", "true")
        val rTrue = LoadConfigReport()
        assertEquals(true, m.invoke(t, props, "K", rTrue))

        props.setProperty("K", "false")
        val rFalse = LoadConfigReport()
        assertEquals(false, m.invoke(t, props, "K", rFalse))
    }

    @Test
    fun evaluateIntegerString_missing_and_bad_and_good() {
        val t = TestHarness()
        val m = methodOf("evaluateIntegerString", Properties::class.java, String::class.java, LoadConfigReport::class.java)
        val props = Properties()
        val rMissing = LoadConfigReport()
        assertNull(m.invoke(t, props, "K", rMissing))
        // missing key: returns null silently; evaluateIntegerString does NOT add MISSING_KEY

        props.setProperty("K", "zz")
        val rBad = LoadConfigReport()
        assertNull(m.invoke(t, props, "K", rBad))
        assertTrue(rBad.types.contains(LoadConfigErrorType.PARSE_ERROR_INT))

        props.setProperty("K", "42")
        val rOk = LoadConfigReport()
        assertEquals(42, m.invoke(t, props, "K", rOk))
    }

    @Test
    fun evaluateColorString_props_variant_and_string_variant_happy_and_bad() {
        val t = TestHarness()
        val mProps = methodOf("evaluateColorString", Properties::class.java, String::class.java, LoadConfigReport::class.java)
        val mStr = methodOf("evaluateColorString", String::class.java, LoadConfigReport::class.java)
        val props = Properties()
        props.setProperty("K", "FF00FF")
        val r1 = LoadConfigReport()
        val c = mProps.invoke(t, props, "K", r1) as Color
        assertEquals(0xFF00FF, c.rgb and 0xFFFFFF)

        val r2 = LoadConfigReport()
        assertNull(mStr.invoke(t, "NOT_HEX", r2))
        assertTrue(r2.types.contains(LoadConfigErrorType.PARSE_ERROR_COLOR))
    }

    @Test
    fun config_load_rejects_non_boolean_extended_char() {
        val props = Properties()
        // Full valid set except KEY_FONT_EXTENDED_CHAR is bogus
        props.setProperty(FontificatorProperties.KEY_FONT_FILE_BORDER, "preset://borders/dw3_border.png")
        props.setProperty(FontificatorProperties.KEY_FONT_FILE_FONT, "preset://fonts/dw3_font.png")
        props.setProperty(FontificatorProperties.KEY_FONT_TYPE, FontType.FIXED_WIDTH.name)
        props.setProperty(FontificatorProperties.KEY_FONT_GRID_WIDTH, "1")
        props.setProperty(FontificatorProperties.KEY_FONT_GRID_HEIGHT, "1")
        props.setProperty(FontificatorProperties.KEY_FONT_SCALE, "1.0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_SCALE, "1.0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_X, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_Y, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACE_WIDTH, "25")
        props.setProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, "A")
        props.setProperty(FontificatorProperties.KEY_FONT_EXTENDED_CHAR, "maybe")
        props.setProperty(FontificatorProperties.KEY_FONT_CHARACTERS, "A")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_LINE, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_CHAR, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_MESSAGE, "0")
        val cfg = ConfigFont()
        val report = cfg.load(props, LoadConfigReport())
        assertFalse(report.isErrorFree)
        assertTrue(report.types.contains(LoadConfigErrorType.PARSE_ERROR_BOOL))
    }
}
