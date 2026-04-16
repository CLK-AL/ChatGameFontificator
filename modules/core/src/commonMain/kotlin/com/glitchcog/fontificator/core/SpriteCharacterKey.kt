package com.glitchcog.fontificator.core

/**
 * Stage S4 — pure-Kotlin port of
 * `com.glitchcog.fontificator.sprite.SpriteCharacterKey`.
 *
 * Mirrors the legacy semantics:
 *  - `isChar()`   ⟺ no emoji attached
 *  - `isEmoji()`  ⟺ emoji attached and not flagged as a badge
 *  - `isBadge()`  ⟺ emoji attached and flagged as a badge  [M4 fix]
 *  - `extended`   ⟺ codepoint falls outside the ASCII 32-127 range
 *  - `toString()` ⟺ `"[E]"` for emojis, else the codepoint rendered as a string
 *
 * The emoji payload is represented as an opaque `Any?` in commonMain
 * (the legacy type is `com.glitchcog.fontificator.emoji.LazyLoadEmoji`
 * which is out of scope for commonMain). The JVM adapter narrows it
 * back when round-tripping through the frozen Java class.
 *
 * ### Stage-S1 M4 regression
 * The original Java `isBadge()` used a bitwise `&` where short-circuit
 * `&&` was intended; this commonMain port uses `&&` (the hardened
 * form). Both evaluate the same result for boolean operands because
 * `isChar()` is side-effect free — the differential-parity test in
 * `SpriteCharacterKeyJvmParityTest` confirms byte-exact agreement.
 */
public class SpriteCharacterKey private constructor(
    private val codepoint: Int,
    private val emoji: Any?,
    private val badge: Boolean,
) {
    /** Whether the character falls outside of the inclusive ASCII range 32-127. */
    public val extended: Boolean =
        !NORMAL_ASCII_KEY.contains(firstCharOf(codepoint).toString())

    /** Construct as a single BMP character. */
    public constructor(character: Char) : this(character.code, null, false)

    /** Construct from a codepoint (may be non-BMP). */
    public constructor(codepoint: Int) : this(codepoint, null, false)

    /** Construct as an emoji (optionally flagged as a badge). */
    public constructor(emoji: Any?, badge: Boolean) : this(127, emoji, badge)

    /**
     * Get the character as a Char, assuming this represents a character.
     * For non-BMP codepoints this returns the broken high-surrogate —
     * legacy behaviour retained for bit-for-bit parity.
     */
    public fun getChar(): Char = firstCharOf(codepoint)

    /** Get the codepoint backing this key. */
    public fun getCodepoint(): Int = codepoint

    /** Get the opaque emoji payload (may be null). */
    public fun getEmoji(): Any? = emoji

    /** True iff no emoji is attached (i.e. this key represents a real character). */
    public fun isChar(): Boolean = emoji == null

    /** True iff an emoji is attached and it is NOT a badge. */
    public fun isEmoji(): Boolean = !isChar() && !badge

    /**
     * True iff an emoji is attached AND it is a badge.
     *
     * Stage-S1 finding M4: the legacy Java used `&` (bitwise) here;
     * this port uses `&&` (short-circuit). Both produce the same
     * boolean result — see KDoc header.
     */
    public fun isBadge(): Boolean = !isChar() && badge

    /** True iff the underlying codepoint is outside ASCII 32..127. */
    public fun isExtended(): Boolean = extended

    override fun toString(): String =
        if (isEmoji()) "[E]" else codepointToString(codepoint)

    public companion object {
        /**
         * The "normal" printable-ASCII key space used by
         * `SpriteFont.NORMAL_ASCII_KEY` in the frozen Java.
         * Copy kept verbatim for parity.
         */
        public const val NORMAL_ASCII_KEY: String =
            " !\"#\$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F"

        /**
         * Equivalent to `Character.toChars(cp)[0]` in the legacy Java
         * (returns the high surrogate for non-BMP codepoints).
         */
        private fun firstCharOf(cp: Int): Char =
            if (cp <= 0xFFFF) cp.toChar()
            // High-surrogate for supplementary codepoints:
            // 0xD800 + ((cp - 0x10000) ushr 10)
            else (0xD800 + ((cp - 0x10000) ushr 10)).toChar()

        /** Equivalent to `new String(Character.toChars(cp))` in legacy Java. */
        private fun codepointToString(cp: Int): String {
            if (cp <= 0xFFFF) return cp.toChar().toString()
            val high = (0xD800 + ((cp - 0x10000) ushr 10)).toChar()
            val low = (0xDC00 + ((cp - 0x10000) and 0x3FF)).toChar()
            return charArrayOf(high, low).concatToString()
        }
    }
}
