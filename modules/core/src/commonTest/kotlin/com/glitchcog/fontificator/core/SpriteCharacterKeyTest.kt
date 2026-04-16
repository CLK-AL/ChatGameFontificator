package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Stage S4 — proof-of-concept positive tests for the commonMain
 * `SpriteCharacterKey` port.
 *
 * The JVM-side `SpriteCharacterKeyJvmParityTest` re-runs the same
 * fixtures through the frozen Java class via `JavaLegacyAdapter` and
 * asserts byte-exact agreement. That differential gate is what
 * promotes these tests from "a rewrite" to "a certified byte-compatible
 * replacement".
 */
class SpriteCharacterKeyTest {

    // --- constructors ------------------------------------------------

    @Test
    fun `char ctor stores codepoint and marks as char`() {
        val k = SpriteCharacterKey('A')
        assertEquals('A'.code, k.getCodepoint())
        assertEquals('A', k.getChar())
        assertTrue(k.isChar())
        assertFalse(k.isEmoji())
        assertFalse(k.isBadge())
        assertNull(k.getEmoji())
    }

    @Test
    fun `int ctor stores codepoint and marks as char`() {
        val k = SpriteCharacterKey(0x2603) // SNOWMAN
        assertEquals(0x2603, k.getCodepoint())
        assertTrue(k.isChar())
        assertFalse(k.isEmoji())
        assertFalse(k.isBadge())
    }

    @Test
    fun `emoji ctor with badge=false marks as emoji, not badge`() {
        val token = Any()
        val k = SpriteCharacterKey(token, false)
        assertFalse(k.isChar())
        assertTrue(k.isEmoji())
        assertFalse(k.isBadge())
        assertSame(token, k.getEmoji())
        // The private ctor hard-codes 127 as the codepoint for emoji keys.
        assertEquals(127, k.getCodepoint())
    }

    @Test
    fun `emoji ctor with badge=true marks as badge, not emoji`() {
        val token = Any()
        val k = SpriteCharacterKey(token, true)
        assertFalse(k.isChar())
        assertFalse(k.isEmoji())
        assertTrue(k.isBadge())
        assertSame(token, k.getEmoji())
    }

    // --- predicate semantics ----------------------------------------

    @Test
    fun `isChar is true only when emoji is null`() {
        assertTrue(SpriteCharacterKey('x').isChar())
        assertTrue(SpriteCharacterKey(42).isChar())
        assertFalse(SpriteCharacterKey(Any(), false).isChar())
        assertFalse(SpriteCharacterKey(Any(), true).isChar())
    }

    @Test
    fun `isEmoji is true only for non-badge emoji`() {
        assertFalse(SpriteCharacterKey('x').isEmoji())
        assertTrue(SpriteCharacterKey(Any(), false).isEmoji())
        assertFalse(SpriteCharacterKey(Any(), true).isEmoji())
    }

    @Test
    fun `isBadge is true only for badge-flagged emoji — M4 regression`() {
        // M4: the original Java used `!isChar() & badge` where
        // `!isChar() && badge` was intended. Both evaluate to the same
        // boolean; the commonMain port uses the corrected `&&` form.
        assertFalse(SpriteCharacterKey('x').isBadge())             // plain char
        assertFalse(SpriteCharacterKey(Any(), false).isBadge())    // emoji w/o badge
        assertTrue(SpriteCharacterKey(Any(), true).isBadge())      // emoji w/ badge
    }

    @Test
    fun `char keys never report as badge even when codepoint is high`() {
        // Char-keyed instance → emoji is null → !isChar() is false → isBadge() false.
        val k = SpriteCharacterKey(0x1F600)
        assertFalse(k.isBadge())
    }

    // --- extended-ASCII flag ----------------------------------------

    @Test
    fun `ASCII printable chars are not extended`() {
        for (c in 32..126) {
            val k = SpriteCharacterKey(c)
            assertFalse(k.isExtended(), "codepoint $c should not be extended")
        }
        // DEL (127) is in NORMAL_ASCII_KEY, so not extended either.
        assertFalse(SpriteCharacterKey(127).isExtended())
    }

    @Test
    fun `non-ASCII chars are extended`() {
        assertTrue(SpriteCharacterKey(0x2603).isExtended())  // SNOWMAN
        assertTrue(SpriteCharacterKey(31).isExtended())      // Unit separator
        assertTrue(SpriteCharacterKey(128).isExtended())     // ¬
    }

    @Test
    fun `supplementary codepoint uses high-surrogate for extended check`() {
        // Character.toChars(0x1F600)[0] is the high-surrogate 0xD83D — not
        // present in NORMAL_ASCII_KEY → extended.
        assertTrue(SpriteCharacterKey(0x1F600).isExtended())
    }

    // --- toString ---------------------------------------------------

    @Test
    fun `toString renders char as its codepoint string`() {
        assertEquals("A", SpriteCharacterKey('A').toString())
        assertEquals(" ", SpriteCharacterKey(' ').toString())
    }

    @Test
    fun `toString renders emoji as bracketed E`() {
        assertEquals("[E]", SpriteCharacterKey(Any(), false).toString())
    }

    @Test
    fun `toString renders badge as its codepoint, not bracketed E`() {
        // isEmoji() is false for a badge — the legacy toString takes the
        // non-emoji branch, which re-renders codepoint 127 as "\u007F".
        val s = SpriteCharacterKey(Any(), true).toString()
        assertEquals("\u007F", s)
    }

    @Test
    fun `toString for supplementary codepoint returns the surrogate pair`() {
        val k = SpriteCharacterKey(0x1F600)
        val s = k.toString()
        assertEquals(2, s.length)
        assertEquals(0xD83D.toChar(), s[0])
        assertEquals(0xDE00.toChar(), s[1])
    }

    // --- getChar compromise (legacy parity) -------------------------

    @Test
    fun `getChar returns BMP char for BMP codepoint`() {
        assertEquals('A', SpriteCharacterKey('A').getChar())
    }

    @Test
    fun `getChar returns high-surrogate for supplementary codepoint`() {
        // Legacy: Character.toChars(0x1F600)[0] == 0xD83D.
        assertEquals(0xD83D.toChar(), SpriteCharacterKey(0x1F600).getChar())
    }

    @Test
    fun `getEmoji round-trips opaque payload`() {
        val payload = "fake-emoji-token"
        val k = SpriteCharacterKey(payload, false)
        assertNotNull(k.getEmoji())
        assertEquals(payload, k.getEmoji())
    }
}
