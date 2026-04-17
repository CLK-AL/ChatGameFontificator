package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Stage S4 -- commonMain tests for the immutable `ConfigMessage` port.
 *
 * Covers:
 *  - round-trip serialization (`fromProperties -> toProperties -> fromProperties`)
 *  - defaults check
 *  - §9 space-allowed divider (messageContentBreak accepts a single
 *    space value)
 *  - invalid casing / case-resolution-type rejection
 *  - required-fields enforcement via shared `baseValidation`
 */
class ConfigMessageTest {

    // ---- round-trip serialization -------------------------------------

    @Test
    fun `round-trip fromProperties-toProperties-fromProperties equals original`() {
        val original = ConfigMessage.defaults()
        val props = original.toProperties()
        val restored = ConfigMessage.fromProperties(props)
        assertEquals(original, restored)
    }

    @Test
    fun `round-trip preserves all fields including non-defaults`() {
        val custom = ConfigMessage(
            usernameFormat = "<%user%>",
            timeFormat = "HH:mm",
            messageContentBreak = " | ",
            queueSize = 128,
            messageSpeed = 60,
            expirationTime = 30,
            includeTimestamps = true,
            showUsernamesOnMessages = false,
            showJoinMessages = true,
            hideEmptyBorder = true,
            hideEmptyBackground = true,
            caseResolutionType = UsernameCaseResolutionType.ALL_CAPS,
            specifyCaseAllowed = true,
            messageCasing = MessageCasing.UPPERCASE,
        )
        val restored = ConfigMessage.fromProperties(custom.toProperties())
        assertEquals(custom, restored)
    }

    @Test
    fun `toProperties produces exactly the MESSAGE_KEYS set`() {
        val props = ConfigMessage.defaults().toProperties()
        assertEquals(ConfigMessage.MESSAGE_KEYS.toSet(), props.keys)
    }

    // ---- defaults validation ------------------------------------------

    @Test
    fun `default config validates without errors`() {
        val errors = ConfigMessage.defaults().validate()
        assertEquals(emptyList(), errors, "Default config should be valid, got: $errors")
    }

    @Test
    fun `defaults match expected Java defaults`() {
        val d = ConfigMessage.defaults()
        assertEquals(ConfigMessage.USERNAME_REPLACE, d.usernameFormat)
        assertEquals("[HH:mm:ss]", d.timeFormat)
        assertEquals(ConfigMessage.DEFAULT_CONTENT_BREAKER, d.messageContentBreak)
        assertEquals(64, d.queueSize)
        assertEquals(30, d.messageSpeed) // (121 * 0.25f).toInt() == 30
        assertEquals(0, d.expirationTime)
        assertFalse(d.includeTimestamps)
        assertTrue(d.showUsernamesOnMessages)
        assertFalse(d.showJoinMessages)
        assertFalse(d.hideEmptyBorder)
        assertFalse(d.hideEmptyBackground)
        assertEquals(UsernameCaseResolutionType.NONE, d.caseResolutionType)
        assertFalse(d.specifyCaseAllowed)
        assertEquals(MessageCasing.MIXED_CASE, d.messageCasing)
    }

    // ---- §9 space-allowed divider -------------------------------------

    @Test
    fun `space-allowed divider -- single space messageContentBreak accepted`() {
        val cfg = ConfigMessage.defaults().copy(messageContentBreak = " ")
        val errors = cfg.validate()
        assertEquals(emptyList(), errors, "Single-space divider should be valid, got: $errors")
    }

    @Test
    fun `space-allowed divider -- empty messageContentBreak rejected`() {
        // The baseValidation function treats empty value as missing
        // even when the key is in spaceAllowedKeys.
        val cfg = ConfigMessage.defaults().copy(messageContentBreak = "")
        val errors = cfg.validate()
        assertTrue(
            errors.any { ConfigMessage.KEY_MESSAGE_CONTENT_BREAK in it },
            "Empty divider should be rejected, got: $errors",
        )
    }

    // ---- required-fields enforcement via baseValidation ---------------

    @Test
    fun `baseValidation reports missing required field when usernameFormat is blank`() {
        val cfg = ConfigMessage.defaults().copy(usernameFormat = "")
        val errors = cfg.validate()
        assertTrue(
            errors.any { ConfigMessage.KEY_MESSAGE_USERFORMAT in it },
            "Empty usernameFormat should be rejected, got: $errors",
        )
    }

    @Test
    fun `baseValidation reports missing required field when timeFormat is blank`() {
        val cfg = ConfigMessage.defaults().copy(timeFormat = "")
        val errors = cfg.validate()
        assertTrue(
            errors.any { ConfigMessage.KEY_MESSAGE_TIMEFORMAT in it },
            "Empty timeFormat should be rejected, got: $errors",
        )
    }

    // ---- invalid casing rejected by fromProperties --------------------

    @Test
    fun `fromProperties rejects invalid messageCasing string`() {
        val props = ConfigMessage.defaults().toProperties().toMutableMap()
        props[ConfigMessage.KEY_MESSAGE_CASING] = "RANDOM_CASE"
        val ex = assertFailsWith<IllegalArgumentException> {
            ConfigMessage.fromProperties(props)
        }
        assertTrue(
            ConfigMessage.KEY_MESSAGE_CASING in (ex.message ?: ""),
            "Exception message should mention the offending key, got: ${ex.message}",
        )
    }

    @Test
    fun `fromProperties rejects invalid caseResolutionType string`() {
        val props = ConfigMessage.defaults().toProperties().toMutableMap()
        props[ConfigMessage.KEY_MESSAGE_CASE_TYPE] = "BOGUS"
        val ex = assertFailsWith<IllegalArgumentException> {
            ConfigMessage.fromProperties(props)
        }
        assertTrue(
            ConfigMessage.KEY_MESSAGE_CASE_TYPE in (ex.message ?: ""),
            "Exception message should mention the offending key, got: ${ex.message}",
        )
    }

    // ---- integer range checks -----------------------------------------

    @Test
    fun `queueSize below minimum fails validation`() {
        val cfg = ConfigMessage.defaults().copy(queueSize = 0)
        assertTrue(cfg.validate().any { ConfigMessage.KEY_MESSAGE_QUEUE_SIZE in it })
    }

    @Test
    fun `queueSize above maximum fails validation`() {
        val cfg = ConfigMessage.defaults().copy(queueSize = 6000)
        assertTrue(cfg.validate().any { ConfigMessage.KEY_MESSAGE_QUEUE_SIZE in it })
    }

    @Test
    fun `messageSpeed above maximum fails validation`() {
        val cfg = ConfigMessage.defaults().copy(messageSpeed = 999)
        assertTrue(cfg.validate().any { ConfigMessage.KEY_MESSAGE_SPEED in it })
    }

    @Test
    fun `expirationTime above maximum fails validation`() {
        val cfg = ConfigMessage.defaults().copy(expirationTime = 10_000)
        assertTrue(cfg.validate().any { ConfigMessage.KEY_MESSAGE_EXPIRATION_TIME in it })
    }

    // ---- fromProperties error handling --------------------------------

    @Test
    fun `fromProperties throws on missing key`() {
        val props = ConfigMessage.defaults().toProperties().toMutableMap()
        props.remove(ConfigMessage.KEY_MESSAGE_QUEUE_SIZE)
        assertFailsWith<IllegalArgumentException> {
            ConfigMessage.fromProperties(props)
        }
    }

    @Test
    fun `fromProperties throws on unparseable integer`() {
        val props = ConfigMessage.defaults().toProperties().toMutableMap()
        props[ConfigMessage.KEY_MESSAGE_QUEUE_SIZE] = "not-a-number"
        assertFailsWith<NumberFormatException> {
            ConfigMessage.fromProperties(props)
        }
    }

    // ---- MessageCasing companion --------------------------------------

    @Test
    fun `MessageCasing contains recognises valid names`() {
        assertTrue(MessageCasing.contains("MIXED_CASE"))
        assertTrue(MessageCasing.contains("UPPERCASE"))
        assertTrue(MessageCasing.contains("LOWERCASE"))
    }

    @Test
    fun `MessageCasing contains rejects invalid names and null`() {
        assertFalse(MessageCasing.contains("RANDOM_CASE"))
        assertFalse(MessageCasing.contains(""))
        assertFalse(MessageCasing.contains(null))
    }

    // ---- UsernameCaseResolutionType companion -------------------------

    @Test
    fun `UsernameCaseResolutionType contains recognises valid names`() {
        assertTrue(UsernameCaseResolutionType.contains("NONE"))
        assertTrue(UsernameCaseResolutionType.contains("FIRST"))
        assertTrue(UsernameCaseResolutionType.contains("ALL_LOWERCASE"))
        assertTrue(UsernameCaseResolutionType.contains("ALL_CAPS"))
        assertTrue(UsernameCaseResolutionType.contains("LOOKUP"))
    }

    @Test
    fun `UsernameCaseResolutionType contains rejects invalid names`() {
        assertFalse(UsernameCaseResolutionType.contains("BOGUS"))
        assertFalse(UsernameCaseResolutionType.contains(""))
    }
}
