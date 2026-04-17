package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stage S4 -- commonMain tests for the immutable `ConfigColor` port.
 *
 * Covers:
 *  - round-trip serialization (`fromProperties -> toProperties -> fromProperties`)
 *  - defaults check
 *  - ColorRGBA hex round-trip and ARGB packing
 *  - empty palette handling
 *  - validation
 *  - fromProperties error handling
 */
class ConfigColorTest {

    // ---- ColorRGBA unit tests -----------------------------------------

    @Test
    fun `ColorRGBA fromHex and toHex round-trip`() {
        val hex = "FF8800"
        val color = ColorRGBA.fromHex(hex)
        assertEquals(0xFF, color.r)
        assertEquals(0x88, color.g)
        assertEquals(0x00, color.b)
        assertEquals(255, color.a)
        assertEquals(hex, color.toHex())
    }

    @Test
    fun `ColorRGBA fromHex pads short values`() {
        val color = ColorRGBA.fromHex("FF")
        assertEquals(0, color.r)
        assertEquals(0, color.g)
        assertEquals(0xFF, color.b)
        assertEquals("0000FF", color.toHex())
    }

    @Test
    fun `ColorRGBA toArgbInt matches java awt Color packing`() {
        val color = ColorRGBA(r = 0xFF, g = 0x00, b = 0x80, a = 0xFF)
        // Expected: 0xFF_FF_00_80
        val expected = (0xFF shl 24) or (0xFF shl 16) or (0x00 shl 8) or 0x80
        assertEquals(expected, color.toArgbInt())
    }

    @Test
    fun `ColorRGBA fromArgbInt inverts toArgbInt`() {
        val original = ColorRGBA(r = 0x12, g = 0x34, b = 0x56, a = 0x78)
        val packed = original.toArgbInt()
        val restored = ColorRGBA.fromArgbInt(packed)
        assertEquals(original, restored)
    }

    @Test
    fun `ColorRGBA toHex is uppercase and 6 chars`() {
        val color = ColorRGBA(r = 0x0A, g = 0x0B, b = 0x0C)
        assertEquals("0A0B0C", color.toHex())
        assertEquals(6, color.toHex().length)
    }

    @Test
    fun `ColorRGBA black produces 000000`() {
        assertEquals("000000", ColorRGBA(0, 0, 0).toHex())
    }

    @Test
    fun `ColorRGBA white produces FFFFFF`() {
        assertEquals("FFFFFF", ColorRGBA(255, 255, 255).toHex())
    }

    // ---- round-trip serialization -------------------------------------

    @Test
    fun `round-trip fromProperties-toProperties-fromProperties equals original`() {
        val original = ConfigColor.defaults()
        val props = original.toProperties()
        val restored = ConfigColor.fromProperties(props)
        assertEquals(original, restored)
    }

    @Test
    fun `round-trip preserves all fields including non-defaults`() {
        val custom = ConfigColor(
            bgColor = ColorRGBA.fromHex("112233"),
            fgColor = ColorRGBA.fromHex("445566"),
            borderColor = ColorRGBA.fromHex("778899"),
            highlight = ColorRGBA.fromHex("AABBCC"),
            chromaColor = ColorRGBA.fromHex("DDEEFF"),
            palette = listOf(ColorRGBA.fromHex("FF0000"), ColorRGBA.fromHex("00FF00")),
            colorUsername = false,
            colorTimestamp = true,
            colorMessage = true,
            colorJoin = true,
            useTwitchColors = true,
        )
        val restored = ConfigColor.fromProperties(custom.toProperties())
        assertEquals(custom, restored)
    }

    @Test
    fun `toProperties produces exactly the COLOR_KEYS set`() {
        val props = ConfigColor.defaults().toProperties()
        assertEquals(ConfigColor.COLOR_KEYS.toSet(), props.keys)
    }

    // ---- defaults validation ------------------------------------------

    @Test
    fun `default config validates without errors`() {
        val errors = ConfigColor.defaults().validate()
        assertEquals(emptyList(), errors, "Default config should be valid, got: $errors")
    }

    @Test
    fun `defaults match expected Java defaults`() {
        val d = ConfigColor.defaults()
        assertEquals(ColorRGBA(0, 0, 0), d.bgColor)
        assertEquals(ColorRGBA(255, 255, 255), d.fgColor)
        assertEquals(ColorRGBA(255, 255, 255), d.borderColor)
        assertEquals(ColorRGBA.fromHex("6699FF"), d.highlight)
        assertEquals(ColorRGBA.fromHex("00FF00"), d.chromaColor)
        assertEquals(7, d.palette.size)
        assertTrue(d.colorUsername)
        assertFalse(d.colorTimestamp)
        assertFalse(d.colorMessage)
        assertFalse(d.colorJoin)
        assertFalse(d.useTwitchColors)
    }

    // ---- empty palette handling ---------------------------------------

    @Test
    fun `empty palette round-trips correctly`() {
        val cfg = ConfigColor.defaults().copy(palette = emptyList())
        val restored = ConfigColor.fromProperties(cfg.toProperties())
        assertEquals(emptyList(), restored.palette)
    }

    @Test
    fun `empty palette string in properties produces empty list`() {
        val props = ConfigColor.defaults().toProperties().toMutableMap()
        props[ConfigColor.KEY_COLOR_PALETTE] = ""
        val cfg = ConfigColor.fromProperties(props)
        assertEquals(emptyList(), cfg.palette)
    }

    // ---- fromProperties error handling --------------------------------

    @Test
    fun `fromProperties throws on missing color key`() {
        val props = ConfigColor.defaults().toProperties().toMutableMap()
        props.remove(ConfigColor.KEY_COLOR_BG)
        assertFailsWith<IllegalArgumentException> {
            ConfigColor.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on invalid color hex`() {
        val props = ConfigColor.defaults().toProperties().toMutableMap()
        props[ConfigColor.KEY_COLOR_BG] = "ZZZZZZ"
        assertFailsWith<IllegalArgumentException> {
            ConfigColor.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on invalid palette entry`() {
        val props = ConfigColor.defaults().toProperties().toMutableMap()
        props[ConfigColor.KEY_COLOR_PALETTE] = "FF0000,INVALID"
        assertFailsWith<IllegalArgumentException> {
            ConfigColor.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on missing boolean key`() {
        val props = ConfigColor.defaults().toProperties().toMutableMap()
        props.remove(ConfigColor.KEY_COLOR_USERNAME)
        assertFailsWith<IllegalArgumentException> {
            ConfigColor.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on unparseable boolean`() {
        val props = ConfigColor.defaults().toProperties().toMutableMap()
        props[ConfigColor.KEY_COLOR_USERNAME] = "maybe"
        assertFailsWith<IllegalArgumentException> {
            ConfigColor.fromProperties(props)
        }
    }

    // ---- single-color palette -----------------------------------------

    @Test
    fun `single-color palette round-trips correctly`() {
        val cfg = ConfigColor.defaults().copy(palette = listOf(ColorRGBA.fromHex("ABCDEF")))
        val restored = ConfigColor.fromProperties(cfg.toProperties())
        assertEquals(1, restored.palette.size)
        assertEquals(ColorRGBA.fromHex("ABCDEF"), restored.palette[0])
    }
}
