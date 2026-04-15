package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.emoji.EmojiType
import com.glitchcog.fontificator.emoji.LazyLoadEmoji
import com.glitchcog.fontificator.sprite.SpriteCharacterKey
import org.junit.jupiter.api.Test
import java.awt.Color
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Emoji-flavoured SpriteCharacterKey branches. */
class SpriteCharacterKeyEmojiTest {

    private fun emoji(type: EmojiType = EmojiType.TWITTER_EMOJI, bg: Color? = null): LazyLoadEmoji =
        LazyLoadEmoji("id", null, "http://example.invalid/x.png", 16, 16, bg, type)

    @Test
    fun emoji_ctor_sets_emoji_flags() {
        val e = emoji()
        val k = SpriteCharacterKey(e, false)
        assertFalse(k.isChar)
        assertTrue(k.isEmoji)
        assertFalse(k.isBadge)
        assertEquals(e, k.emoji)
        // toString returns "[E]" when emoji
        assertEquals("[E]", k.toString())
    }

    @Test
    fun emoji_ctor_with_badge_flag_sets_badge_true() {
        val e = emoji(EmojiType.TWITCH_BADGE)
        val k = SpriteCharacterKey(e, true)
        assertFalse(k.isChar)
        assertFalse(k.isEmoji)
        assertTrue(k.isBadge)
    }

    @Test
    fun emojiBgColor_returns_emoji_bg_when_no_override() {
        val e = emoji(bg = Color.BLUE)
        val k = SpriteCharacterKey(e, false)
        assertEquals(Color.BLUE, k.emojiBgColor)
    }

    @Test
    fun emojiBgColor_returns_null_when_emoji_bg_null_and_no_override() {
        val e = emoji(bg = null)
        val k = SpriteCharacterKey(e, false)
        // emoji.getBgColor() is null -> null
        assertEquals(null, k.emojiBgColor)
    }
}
