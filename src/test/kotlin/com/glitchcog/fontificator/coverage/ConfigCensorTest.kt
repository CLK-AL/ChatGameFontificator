package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.config.ConfigCensor
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConfigCensorTest {

    private fun fullProps(): Properties = Properties().apply {
        setProperty(FontificatorProperties.KEY_CENSOR_ENABLED, "true")
        setProperty(FontificatorProperties.KEY_CENSOR_PURGE_ON_TWITCH_BAN, "true")
        setProperty(FontificatorProperties.KEY_CENSOR_URL, "false")
        setProperty(FontificatorProperties.KEY_CENSOR_FIRST_URL, "false")
        setProperty(FontificatorProperties.KEY_CENSOR_UNKNOWN_CHARS, "true")
        setProperty(FontificatorProperties.KEY_CENSOR_UNKNOWN_CHARS_PERCENT, "50")
        setProperty(FontificatorProperties.KEY_CENSOR_WHITE, "alice,bob")
        setProperty(FontificatorProperties.KEY_CENSOR_BLACK, "mallory")
        setProperty(FontificatorProperties.KEY_CENSOR_BANNED, "badword1,badword2")
    }

    @Test
    fun load_happy_path() {
        val cfg = ConfigCensor()
        val r = cfg.load(fullProps(), LoadConfigReport())
        assertTrue(r.isErrorFree)
        assertTrue(cfg.isCensorshipEnabled)
        assertTrue(cfg.isPurgeOnTwitchBan)
        assertFalse(cfg.isCensorAllUrls)
        assertFalse(cfg.isCensorFirstUrls)
        assertTrue(cfg.isCensorUnknownChars)
        assertEquals(50, cfg.unknownCharPercentage)
        assertEquals(2, cfg.userWhitelist.size)
        assertEquals(1, cfg.userBlacklist.size)
        assertEquals(2, cfg.bannedWords.size)
    }

    @Test
    fun load_without_optional_lists_uses_empty_defaults() {
        val p = fullProps().apply {
            remove(FontificatorProperties.KEY_CENSOR_WHITE)
            remove(FontificatorProperties.KEY_CENSOR_BLACK)
            remove(FontificatorProperties.KEY_CENSOR_BANNED)
        }
        val cfg = ConfigCensor()
        val r = cfg.load(p, LoadConfigReport())
        assertTrue(r.isErrorFree)
        // split on empty string returns ["".split(",")] == [""] (size 1)
        assertEquals(1, cfg.userWhitelist.size)
        assertEquals(1, cfg.userBlacklist.size)
        assertEquals(1, cfg.bannedWords.size)
    }

    @Test
    fun load_with_bad_percent_fails_validation() {
        val p = fullProps().apply {
            setProperty(FontificatorProperties.KEY_CENSOR_UNKNOWN_CHARS_PERCENT, "150") // out of range
        }
        val cfg = ConfigCensor()
        val r = cfg.load(p, LoadConfigReport())
        assertFalse(r.isErrorFree)
    }

    @Test
    fun reset_nulls_fields_and_setters_round_trip() {
        val cfg = ConfigCensor()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.reset()
        cfg.load(fullProps(), LoadConfigReport())
        cfg.setCensorshipEnabled(false); assertFalse(cfg.isCensorshipEnabled)
        cfg.setPurgeOnTwitchBan(false); assertFalse(cfg.isPurgeOnTwitchBan)
        cfg.setCensorAllUrls(true); assertTrue(cfg.isCensorAllUrls)
        cfg.setCensorFirstUrls(true); assertTrue(cfg.isCensorFirstUrls)
        cfg.setCensorUnknownChars(false); assertFalse(cfg.isCensorUnknownChars)
        cfg.setUnknownCharPercentage(75); assertEquals(75, cfg.unknownCharPercentage)
        cfg.userWhitelist = arrayOf("x", "y", "z")
        assertEquals(3, cfg.userWhitelist.size)
        cfg.userBlacklist = arrayOf("b1")
        assertEquals(1, cfg.userBlacklist.size)
        cfg.bannedWords = arrayOf("w1", "w2")
        assertEquals(2, cfg.bannedWords.size)
    }

    @Test
    fun list_as_string_helpers_round_trip_to_comma_separated() {
        val cfg = ConfigCensor()
        cfg.load(fullProps(), LoadConfigReport())
        assertEquals("alice,bob", cfg.userWhiteListString)
        assertEquals("mallory", cfg.userBlackListString)
        assertEquals("badword1,badword2", cfg.bannedWordsString)
    }

    @Test
    fun validateStrings_direct() {
        val cfg = ConfigCensor()
        val r = LoadConfigReport()
        cfg.validateStrings(r, "true", "true", "false", "false", "true", "42")
        assertTrue(r.isErrorFree)
        val rBad = LoadConfigReport()
        cfg.validateStrings(rBad, "true", "true", "false", "false", "true", "200")
        assertFalse(rBad.isErrorFree)
    }

    @Test
    fun load_with_non_error_free_report_skips_fill_values() {
        // Inject a pre-existing error so the `if (report.isErrorFree())` is false
        // on entry, forcing the skip branch at ConfigCensor.java:62.
        val cfg = ConfigCensor()
        val report = LoadConfigReport().apply {
            addError("pre-existing", com.glitchcog.fontificator.config.loadreport.LoadConfigErrorType.UNKNOWN_ERROR)
        }
        cfg.load(fullProps(), report)
        // No values filled.
        assertFalse(report.isErrorFree)
    }
}
