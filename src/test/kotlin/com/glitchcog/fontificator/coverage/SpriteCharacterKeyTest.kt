package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.sprite.SpriteCharacterKey
import org.junit.jupiter.api.Test
import java.awt.Color
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpriteCharacterKeyTest {

    @Test
    fun char_ctor_populates_basic_fields_and_flags() {
        val k = SpriteCharacterKey('A')
        assertEquals('A', k.char)
        assertEquals('A'.code, k.codepoint)
        assertTrue(k.isChar)
        assertFalse(k.isEmoji)
        assertFalse(k.isBadge)
        assertNull(k.emoji)
        assertFalse(k.isExtended)
        assertEquals("A", k.toString())
    }

    @Test
    fun codepoint_ctor_is_equivalent_for_ascii() {
        val k = SpriteCharacterKey('Z'.code)
        assertEquals('Z', k.char)
        assertTrue(k.isChar)
        assertFalse(k.isExtended)
    }

    @Test
    fun extended_codepoint_sets_extended_flag() {
        // 0xA9 (copyright) falls outside NORMAL_ASCII_KEY
        val k = SpriteCharacterKey(0xA9)
        assertTrue(k.isExtended)
        assertTrue(k.isChar)
    }

    @Test
    fun astral_codepoint_handles_surrogate_pair_without_npe() {
        // U+1F600 GRINNING FACE uses a surrogate pair
        val k = SpriteCharacterKey(0x1F600)
        assertTrue(k.isExtended)
        // getChar returns the high surrogate half, per the class's documented compromise.
        val c = k.char
        assertTrue(Character.isSurrogate(c))
        assertEquals("\uD83D\uDE00", k.toString())
    }

    @Test
    fun emojiBgColor_returns_null_when_no_emoji_and_no_override() {
        val k = SpriteCharacterKey('A')
        assertNull(k.emojiBgColor)
    }

    @Test
    fun emojiBgColor_override_takes_precedence() {
        val k = SpriteCharacterKey('A')
        k.setEmojiBgColorOverride(Color.RED)
        assertEquals(Color.RED, k.emojiBgColor)
    }
}
