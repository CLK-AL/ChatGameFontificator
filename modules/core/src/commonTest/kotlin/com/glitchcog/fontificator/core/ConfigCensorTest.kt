package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stage S4 -- commonMain tests for the immutable `ConfigCensor` port.
 *
 * Covers:
 *  - round-trip serialization (`fromProperties -> toProperties -> fromProperties`)
 *  - defaults check
 *  - validation: unknownCharsPercent range
 *  - whitelist / blacklist / banned-words list handling
 *  - fromProperties error handling
 */
class ConfigCensorTest {

    // ---- round-trip serialization -------------------------------------

    @Test
    fun `round-trip fromProperties-toProperties-fromProperties equals original`() {
        val original = ConfigCensor.defaults()
        val props = original.toProperties()
        val restored = ConfigCensor.fromProperties(props)
        assertEquals(original, restored)
    }

    @Test
    fun `round-trip preserves all fields including non-defaults`() {
        val custom = ConfigCensor(
            censorshipEnabled = false,
            purgeOnTwitchBan = false,
            censorAllUrls = true,
            censorFirstUrls = true,
            censorUnknownChars = true,
            unknownCharsPercent = 50,
            userWhitelist = listOf("admin", "mod"),
            userBlacklist = listOf("troll"),
            bannedWords = listOf("spam", "scam"),
        )
        val restored = ConfigCensor.fromProperties(custom.toProperties())
        assertEquals(custom, restored)
    }

    @Test
    fun `toProperties produces exactly the CENSOR_KEYS set`() {
        val props = ConfigCensor.defaults().toProperties()
        assertEquals(ConfigCensor.CENSOR_KEYS.toSet(), props.keys)
    }

    // ---- defaults validation ------------------------------------------

    @Test
    fun `default config validates without errors`() {
        val errors = ConfigCensor.defaults().validate()
        assertEquals(emptyList(), errors, "Default config should be valid, got: $errors")
    }

    @Test
    fun `defaults match expected Java defaults`() {
        val d = ConfigCensor.defaults()
        assertTrue(d.censorshipEnabled)
        assertTrue(d.purgeOnTwitchBan)
        assertFalse(d.censorAllUrls)
        assertFalse(d.censorFirstUrls)
        assertFalse(d.censorUnknownChars)
        assertEquals(20, d.unknownCharsPercent)
        assertEquals(emptyList(), d.userWhitelist)
        assertEquals(emptyList(), d.userBlacklist)
        assertEquals(emptyList(), d.bannedWords)
    }

    // ---- unknownCharsPercent validation -------------------------------

    @Test
    fun `unknownCharsPercent below minimum fails validation`() {
        val cfg = ConfigCensor.defaults().copy(unknownCharsPercent = -1)
        assertTrue(cfg.validate().any { ConfigCensor.KEY_CENSOR_UNKNOWN_CHARS_PERCENT in it })
    }

    @Test
    fun `unknownCharsPercent above maximum fails validation`() {
        val cfg = ConfigCensor.defaults().copy(unknownCharsPercent = 101)
        assertTrue(cfg.validate().any { ConfigCensor.KEY_CENSOR_UNKNOWN_CHARS_PERCENT in it })
    }

    @Test
    fun `unknownCharsPercent at boundaries passes validation`() {
        val cfg0 = ConfigCensor.defaults().copy(unknownCharsPercent = 0)
        assertEquals(emptyList(), cfg0.validate())

        val cfg100 = ConfigCensor.defaults().copy(unknownCharsPercent = 100)
        assertEquals(emptyList(), cfg100.validate())
    }

    // ---- list handling ------------------------------------------------

    @Test
    fun `empty lists round-trip correctly`() {
        val cfg = ConfigCensor.defaults()
        val props = cfg.toProperties()
        assertEquals("", props[ConfigCensor.KEY_CENSOR_WHITE])
        assertEquals("", props[ConfigCensor.KEY_CENSOR_BLACK])
        assertEquals("", props[ConfigCensor.KEY_CENSOR_BANNED])
        val restored = ConfigCensor.fromProperties(props)
        assertEquals(emptyList(), restored.userWhitelist)
        assertEquals(emptyList(), restored.userBlacklist)
        assertEquals(emptyList(), restored.bannedWords)
    }

    @Test
    fun `multi-item lists round-trip correctly`() {
        val cfg = ConfigCensor.defaults().copy(
            userWhitelist = listOf("alice", "bob"),
            userBlacklist = listOf("eve"),
            bannedWords = listOf("bad", "worse", "worst"),
        )
        val restored = ConfigCensor.fromProperties(cfg.toProperties())
        assertEquals(listOf("alice", "bob"), restored.userWhitelist)
        assertEquals(listOf("eve"), restored.userBlacklist)
        assertEquals(listOf("bad", "worse", "worst"), restored.bannedWords)
    }

    @Test
    fun `fromProperties treats missing list keys as empty lists`() {
        val props = ConfigCensor.defaults().toProperties().toMutableMap()
        props.remove(ConfigCensor.KEY_CENSOR_WHITE)
        props.remove(ConfigCensor.KEY_CENSOR_BLACK)
        props.remove(ConfigCensor.KEY_CENSOR_BANNED)
        // fromProperties should not throw -- lists default to empty
        val cfg = ConfigCensor.fromProperties(props)
        assertEquals(emptyList(), cfg.userWhitelist)
        assertEquals(emptyList(), cfg.userBlacklist)
        assertEquals(emptyList(), cfg.bannedWords)
    }

    // ---- fromProperties error handling --------------------------------

    @Test
    fun `fromProperties throws on missing required key`() {
        val props = ConfigCensor.defaults().toProperties().toMutableMap()
        props.remove(ConfigCensor.KEY_CENSOR_ENABLED)
        assertFailsWith<IllegalArgumentException> {
            ConfigCensor.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on unparseable boolean`() {
        val props = ConfigCensor.defaults().toProperties().toMutableMap()
        props[ConfigCensor.KEY_CENSOR_ENABLED] = "maybe"
        assertFailsWith<IllegalArgumentException> {
            ConfigCensor.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on unparseable integer`() {
        val props = ConfigCensor.defaults().toProperties().toMutableMap()
        props[ConfigCensor.KEY_CENSOR_UNKNOWN_CHARS_PERCENT] = "abc"
        assertFailsWith<NumberFormatException> {
            ConfigCensor.fromProperties(props)
        }
    }
}
