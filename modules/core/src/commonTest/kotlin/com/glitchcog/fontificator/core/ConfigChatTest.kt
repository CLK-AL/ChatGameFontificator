package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Stage S4 -- commonMain tests for the immutable `ConfigChat` port.
 *
 * Covers:
 *  - round-trip serialization (`fromProperties -> toProperties -> fromProperties`)
 *  - defaults check
 *  - validation: dimension, chroma border, chroma corner radius
 *  - legacy window width/height handling
 *  - required-fields enforcement via shared `baseValidation`
 */
class ConfigChatTest {

    // ---- round-trip serialization -------------------------------------

    @Test
    fun `round-trip fromProperties-toProperties-fromProperties equals original`() {
        val original = ConfigChat.defaults()
        val props = original.toProperties()
        val restored = ConfigChat.fromProperties(props)
        assertEquals(original, restored)
    }

    @Test
    fun `round-trip preserves all fields including non-defaults`() {
        val custom = ConfigChat(
            scrollable = true,
            resizable = false,
            rememberPosition = true,
            chatWindowPositionX = 100,
            chatWindowPositionY = 200,
            chatFromBottom = true,
            width = 800,
            height = 600,
            windowWidth = null,
            windowHeight = null,
            chromaEnabled = true,
            chromaInvert = true,
            chromaLeft = 5,
            chromaTop = 15,
            chromaRight = 25,
            chromaBottom = 35,
            chromaCornerRadius = 64,
            reverseScrolling = true,
            alwaysOnTop = true,
            antiAlias = true,
        )
        val restored = ConfigChat.fromProperties(custom.toProperties())
        assertEquals(custom, restored)
    }

    @Test
    fun `toProperties produces exactly the CHAT_KEYS set for defaults`() {
        val props = ConfigChat.defaults().toProperties()
        // defaults have null window sizes, so they should not appear
        assertEquals(ConfigChat.CHAT_KEYS.toSet(), props.keys)
    }

    @Test
    fun `toProperties includes legacy keys when windowWidth and windowHeight are set`() {
        val cfg = ConfigChat.defaults().copy(windowWidth = 640, windowHeight = 480)
        val props = cfg.toProperties()
        assertEquals("640", props[ConfigChat.KEY_CHAT_WINDOW_WIDTH])
        assertEquals("480", props[ConfigChat.KEY_CHAT_WINDOW_HEIGHT])
    }

    // ---- defaults validation ------------------------------------------

    @Test
    fun `default config validates without errors`() {
        val errors = ConfigChat.defaults().validate()
        assertEquals(emptyList(), errors, "Default config should be valid, got: $errors")
    }

    @Test
    fun `defaults match expected Java defaults`() {
        val d = ConfigChat.defaults()
        assertFalse(d.scrollable)
        assertTrue(d.resizable)
        assertFalse(d.rememberPosition)
        assertEquals(0, d.chatWindowPositionX)
        assertEquals(0, d.chatWindowPositionY)
        assertFalse(d.chatFromBottom)
        assertEquals(550, d.width)
        assertEquals(450, d.height)
        assertNull(d.windowWidth)
        assertNull(d.windowHeight)
        assertFalse(d.chromaEnabled)
        assertFalse(d.chromaInvert)
        assertEquals(10, d.chromaLeft)
        assertEquals(10, d.chromaTop)
        assertEquals(10, d.chromaRight)
        assertEquals(10, d.chromaBottom)
        assertEquals(10, d.chromaCornerRadius)
        assertFalse(d.reverseScrolling)
        assertFalse(d.alwaysOnTop)
        assertFalse(d.antiAlias)
    }

    // ---- dimension validation -----------------------------------------

    @Test
    fun `width below minimum fails validation`() {
        val cfg = ConfigChat.defaults().copy(width = 0)
        assertTrue(cfg.validate().any { ConfigChat.KEY_CHAT_WIDTH in it })
    }

    @Test
    fun `height below minimum fails validation`() {
        val cfg = ConfigChat.defaults().copy(height = -1)
        assertTrue(cfg.validate().any { ConfigChat.KEY_CHAT_HEIGHT in it })
    }

    // ---- chroma border validation -------------------------------------

    @Test
    fun `negative chromaLeft fails validation`() {
        val cfg = ConfigChat.defaults().copy(chromaLeft = -1)
        assertTrue(cfg.validate().any { ConfigChat.KEY_CHAT_CHROMA_LEFT in it })
    }

    @Test
    fun `negative chromaTop fails validation`() {
        val cfg = ConfigChat.defaults().copy(chromaTop = -1)
        assertTrue(cfg.validate().any { ConfigChat.KEY_CHAT_CHROMA_TOP in it })
    }

    @Test
    fun `negative chromaRight fails validation`() {
        val cfg = ConfigChat.defaults().copy(chromaRight = -1)
        assertTrue(cfg.validate().any { ConfigChat.KEY_CHAT_CHROMA_RIGHT in it })
    }

    @Test
    fun `negative chromaBottom fails validation`() {
        val cfg = ConfigChat.defaults().copy(chromaBottom = -1)
        assertTrue(cfg.validate().any { ConfigChat.KEY_CHAT_CHROMA_BOTTOM in it })
    }

    // ---- chroma corner radius validation ------------------------------

    @Test
    fun `chromaCornerRadius below minimum fails validation`() {
        val cfg = ConfigChat.defaults().copy(chromaCornerRadius = -1)
        assertTrue(cfg.validate().any { ConfigChat.KEY_CHAT_CHROMA_CORNER in it })
    }

    @Test
    fun `chromaCornerRadius above maximum fails validation`() {
        val cfg = ConfigChat.defaults().copy(chromaCornerRadius = 200)
        assertTrue(cfg.validate().any { ConfigChat.KEY_CHAT_CHROMA_CORNER in it })
    }

    @Test
    fun `chromaCornerRadius at maximum passes validation`() {
        val cfg = ConfigChat.defaults().copy(chromaCornerRadius = 128)
        val errors = cfg.validate()
        assertTrue(errors.none { ConfigChat.KEY_CHAT_CHROMA_CORNER in it })
    }

    // ---- fromProperties error handling --------------------------------

    @Test
    fun `fromProperties throws on missing key`() {
        val props = ConfigChat.defaults().toProperties().toMutableMap()
        props.remove(ConfigChat.KEY_CHAT_WIDTH)
        assertFailsWith<IllegalArgumentException> {
            ConfigChat.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on unparseable integer`() {
        val props = ConfigChat.defaults().toProperties().toMutableMap()
        props[ConfigChat.KEY_CHAT_WIDTH] = "not-a-number"
        assertFailsWith<NumberFormatException> {
            ConfigChat.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on unparseable boolean`() {
        val props = ConfigChat.defaults().toProperties().toMutableMap()
        props[ConfigChat.KEY_CHAT_SCROLL] = "maybe"
        assertFailsWith<IllegalArgumentException> {
            ConfigChat.fromProperties(props)
        }
    }

    // ---- legacy window size handling ----------------------------------

    @Test
    fun `fromProperties reads legacy window sizes when present`() {
        val props = ConfigChat.defaults().toProperties().toMutableMap()
        props[ConfigChat.KEY_CHAT_WINDOW_WIDTH] = "640"
        props[ConfigChat.KEY_CHAT_WINDOW_HEIGHT] = "480"
        val cfg = ConfigChat.fromProperties(props)
        assertEquals(640, cfg.windowWidth)
        assertEquals(480, cfg.windowHeight)
    }

    @Test
    fun `fromProperties sets windowWidth and windowHeight to null when absent`() {
        val props = ConfigChat.defaults().toProperties()
        val cfg = ConfigChat.fromProperties(props)
        assertNull(cfg.windowWidth)
        assertNull(cfg.windowHeight)
    }
}
