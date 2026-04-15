package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.config.EmojiLoadingDisplayStragegy
import com.glitchcog.fontificator.config.FontType
import com.glitchcog.fontificator.config.MessageCasing
import com.glitchcog.fontificator.config.UsernameCaseResolutionType
import com.glitchcog.fontificator.config.loadreport.LoadConfigErrorType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EnumsTest {

    @Test
    fun fontType_all_enum_helpers() {
        // toString -> label
        assertEquals("Fixed-width", FontType.FIXED_WIDTH.toString())
        assertEquals("Variable-width", FontType.VARIABLE_WIDTH.toString())
        assertEquals("Fixed-width", FontType.FIXED_WIDTH.getLabel())

        // contains: true + false
        assertTrue(FontType.contains("FIXED_WIDTH"))
        assertTrue(FontType.contains("VARIABLE_WIDTH"))
        assertFalse(FontType.contains("BOGUS"))

        // getByLabel: hit + miss
        assertEquals(FontType.FIXED_WIDTH, FontType.getByLabel("Fixed-width"))
        assertEquals(FontType.VARIABLE_WIDTH, FontType.getByLabel("Variable-width"))
        assertNull(FontType.getByLabel("nope"))
    }

    @Test
    fun messageCasing_all_enum_helpers() {
        for (m in MessageCasing.values()) {
            assertNotNull(m.toString())
            assertTrue(MessageCasing.contains(m.name))
        }
        assertFalse(MessageCasing.contains("BOGUS"))
    }

    @Test
    fun usernameCaseResolutionType_all_enum_helpers() {
        for (m in UsernameCaseResolutionType.values()) {
            assertNotNull(m.toString())
            assertTrue(UsernameCaseResolutionType.contains(m.name))
        }
        assertFalse(UsernameCaseResolutionType.contains("BOGUS"))
    }

    @Test
    fun emojiLoadingDisplayStragegy_all_enum_helpers() {
        for (m in EmojiLoadingDisplayStragegy.values()) {
            assertNotNull(m.toString())
            assertTrue(EmojiLoadingDisplayStragegy.contains(m.name))
        }
        assertFalse(EmojiLoadingDisplayStragegy.contains("BOGUS"))
    }

    @Test
    fun loadConfigErrorType_isProblem_exhaustive() {
        // Touch every enum constant to cover the isProblem getter on both branches.
        val problems = listOf(
            LoadConfigErrorType.FILE_NOT_FOUND,
            LoadConfigErrorType.VALUE_OUT_OF_RANGE,
            LoadConfigErrorType.PARSE_ERROR_INT,
            LoadConfigErrorType.PARSE_ERROR_FLOAT,
            LoadConfigErrorType.PARSE_ERROR_BOOL,
            LoadConfigErrorType.PARSE_ERROR_CHAR,
            LoadConfigErrorType.PARSE_ERROR_COLOR,
            LoadConfigErrorType.PARSE_ERROR_STRING,
            LoadConfigErrorType.PARSE_ERROR_ENUM,
            LoadConfigErrorType.UNKNOWN_ERROR,
        )
        for (p in problems) {
            assertTrue(p.isProblem)
        }
        assertFalse(LoadConfigErrorType.MISSING_KEY.isProblem)
        assertFalse(LoadConfigErrorType.MISSING_VALUE.isProblem)
        // valueOf roundtrip
        for (v in LoadConfigErrorType.values()) {
            assertEquals(v, LoadConfigErrorType.valueOf(v.name))
        }
    }
}
