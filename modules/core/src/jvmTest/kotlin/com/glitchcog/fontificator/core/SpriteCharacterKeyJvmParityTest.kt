package com.glitchcog.fontificator.core

import com.glitchcog.fontificator.emoji.EmojiType
import com.glitchcog.fontificator.emoji.LazyLoadEmoji
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stage S4 differential-parity gate for `SpriteCharacterKey`.
 *
 * Every fixture from the commonMain `SpriteCharacterKeyTest` is
 * re-run here: once through the commonMain port and once through
 * `JavaLegacyAdapter.toJava(...)` — which instantiates the frozen
 * `com.glitchcog.fontificator.sprite.SpriteCharacterKey` via its
 * public constructors. The getter / predicate / toString surface
 * must agree byte-for-byte across both implementations.
 *
 * This test is what turns the KMP port from "a rewrite" into
 * "a certified byte-compatible replacement".
 */
class SpriteCharacterKeyJvmParityTest {

    // --- char / codepoint fixtures ----------------------------------

    @Test
    fun `char ctor — parity`() {
        assertParity(SpriteCharacterKey('A'))
        assertParity(SpriteCharacterKey(' '))
        assertParity(SpriteCharacterKey('~'))
    }

    @Test
    fun `codepoint ctor inside BMP — parity`() {
        assertParity(SpriteCharacterKey(0x0041))  // 'A'
        assertParity(SpriteCharacterKey(0x2603))  // SNOWMAN
        assertParity(SpriteCharacterKey(0x007F))  // DEL
        assertParity(SpriteCharacterKey(0x001F))  // Unit separator
        assertParity(SpriteCharacterKey(0x00A2))  // ¢
    }

    @Test
    fun `codepoint ctor supplementary plane — parity`() {
        // Non-BMP codepoints: both implementations take the high-surrogate
        // for getChar(), and both render the surrogate pair via toString().
        assertParity(SpriteCharacterKey(0x1F600))  // grinning face
        assertParity(SpriteCharacterKey(0x1F4A9))  // pile of poo
    }

    // --- emoji / badge fixtures -------------------------------------

    @Test
    fun `emoji ctor with badge=false — parity`() {
        val emoji = mkEmoji()
        assertParity(SpriteCharacterKey(emoji, false))
    }

    @Test
    fun `emoji ctor with badge=true — parity (M4 regression)`() {
        // Exercises the `!isChar() (& or &&) badge` branch on both
        // implementations. The legacy Java uses `&`; the commonMain
        // port uses `&&`; the result must be identical.
        val emoji = mkEmoji()
        assertParity(SpriteCharacterKey(emoji, true))
    }

    @Test
    fun `null emoji with badge=false — parity`() {
        // LazyLoadEmoji.null + badge=false → legacy treats it as emoji.
        assertParity(SpriteCharacterKey(null, false))
    }

    @Test
    fun `null emoji with badge=true — parity`() {
        assertParity(SpriteCharacterKey(null, true))
    }

    // --- helpers ----------------------------------------------------

    /** Minimal LazyLoadEmoji — no network, no image decode. */
    private fun mkEmoji(): LazyLoadEmoji =
        LazyLoadEmoji("parity", "http://example.invalid/x.png", EmojiType.TWITCH_V1)

    /**
     * Drive both paths and assert byte-exact equality of every
     * observable on `SpriteCharacterKey`'s public surface.
     */
    private fun assertParity(k: SpriteCharacterKey) {
        val j = JavaLegacyAdapter.toJava(k)

        assertEquals(j.codepoint, k.getCodepoint(), "codepoint mismatch")
        assertEquals(j.char, k.getChar(), "char mismatch")
        assertEquals(j.isChar, k.isChar(), "isChar mismatch")
        assertEquals(j.isEmoji, k.isEmoji(), "isEmoji mismatch")
        assertEquals(j.isBadge, k.isBadge(), "isBadge mismatch")
        assertEquals(j.isExtended, k.isExtended(), "isExtended mismatch")
        assertEquals(j.toString(), k.toString(), "toString mismatch")

        // Round-trip: fromJava(toJava(k)) observably matches k.
        val roundTripped = JavaLegacyAdapter.fromJava(j)
        assertEquals(k.getCodepoint(), roundTripped.getCodepoint(), "round-trip codepoint")
        assertEquals(k.isChar(), roundTripped.isChar(), "round-trip isChar")
        assertEquals(k.isBadge(), roundTripped.isBadge(), "round-trip isBadge")
        assertEquals(k.isEmoji(), roundTripped.isEmoji(), "round-trip isEmoji")
        assertEquals(k.toString(), roundTripped.toString(), "round-trip toString")
    }
}
