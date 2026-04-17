package com.glitchcog.fontificator.core

import com.glitchcog.fontificator.config.ConfigCensor as JavaConfigCensor
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stage S4 differential-parity gate for `ConfigCensor`.
 *
 * Constructs a Java `ConfigCensor` via its `load(Properties, ...)`
 * path, converts it to the commonMain port via
 * `JavaLegacyAdapter.configCensorFromJava`, and asserts field-by-field
 * parity.
 */
class ConfigCensorJvmParityTest {

    private fun buildProperties(
        enabled: Boolean = true,
        purgeOnTwitchBan: Boolean = true,
        censorUrl: Boolean = false,
        censorFirstUrl: Boolean = false,
        censorUnknownChars: Boolean = false,
        unknownCharsPercent: Int = 20,
        whitelist: String = "",
        blacklist: String = "",
        banned: String = "",
    ): Properties {
        val p = Properties()
        p.setProperty(FontificatorProperties.KEY_CENSOR_ENABLED, enabled.toString())
        p.setProperty(FontificatorProperties.KEY_CENSOR_PURGE_ON_TWITCH_BAN, purgeOnTwitchBan.toString())
        p.setProperty(FontificatorProperties.KEY_CENSOR_URL, censorUrl.toString())
        p.setProperty(FontificatorProperties.KEY_CENSOR_FIRST_URL, censorFirstUrl.toString())
        p.setProperty(FontificatorProperties.KEY_CENSOR_UNKNOWN_CHARS, censorUnknownChars.toString())
        p.setProperty(FontificatorProperties.KEY_CENSOR_UNKNOWN_CHARS_PERCENT, unknownCharsPercent.toString())
        p.setProperty(FontificatorProperties.KEY_CENSOR_WHITE, whitelist)
        p.setProperty(FontificatorProperties.KEY_CENSOR_BLACK, blacklist)
        p.setProperty(FontificatorProperties.KEY_CENSOR_BANNED, banned)
        return p
    }

    private fun loadJavaConfig(props: Properties): JavaConfigCensor {
        val javaConfig = JavaConfigCensor()
        val report = LoadConfigReport()
        javaConfig.load(props, report)
        assertTrue(report.isErrorFree, "Java load had errors: ${report.messages}")
        return javaConfig
    }

    private fun assertFieldParity(java: JavaConfigCensor, kotlin: ConfigCensor) {
        assertEquals(java.isCensorshipEnabled, kotlin.censorshipEnabled, "censorshipEnabled mismatch")
        assertEquals(java.isPurgeOnTwitchBan, kotlin.purgeOnTwitchBan, "purgeOnTwitchBan mismatch")
        assertEquals(java.isCensorAllUrls, kotlin.censorAllUrls, "censorAllUrls mismatch")
        assertEquals(java.isCensorFirstUrls, kotlin.censorFirstUrls, "censorFirstUrls mismatch")
        assertEquals(java.isCensorUnknownChars, kotlin.censorUnknownChars, "censorUnknownChars mismatch")
        assertEquals(java.unknownCharPercentage, kotlin.unknownCharsPercent, "unknownCharsPercent mismatch")
        assertEquals(java.userWhitelist?.toList() ?: emptyList(), kotlin.userWhitelist, "userWhitelist mismatch")
        assertEquals(java.userBlacklist?.toList() ?: emptyList(), kotlin.userBlacklist, "userBlacklist mismatch")
        assertEquals(java.bannedWords?.toList() ?: emptyList(), kotlin.bannedWords, "bannedWords mismatch")
    }

    // ---- parity: default config ---------------------------------------

    @Test
    fun `default config -- adapter produces field-exact parity`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configCensorFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: every field set to non-default values -----------------

    @Test
    fun `non-default fields -- adapter produces field-exact parity`() {
        val props = buildProperties(
            enabled = false,
            purgeOnTwitchBan = false,
            censorUrl = true,
            censorFirstUrl = true,
            censorUnknownChars = true,
            unknownCharsPercent = 75,
            whitelist = "admin,mod",
            blacklist = "troll",
            banned = "spam,scam",
        )
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configCensorFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: setter mutations -------------------------------------

    @Test
    fun `Java setter mutations are reflected by adapter`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)

        java.setCensorshipEnabled(false)
        java.setPurgeOnTwitchBan(false)
        java.setCensorAllUrls(true)
        java.setCensorFirstUrls(true)
        java.setCensorUnknownChars(true)
        java.setUnknownCharPercentage(50)
        java.userWhitelist = arrayOf("alice", "bob")
        java.userBlacklist = arrayOf("eve")
        java.bannedWords = arrayOf("bad", "worse")

        val kotlin = JavaLegacyAdapter.configCensorFromJava(java)
        assertFieldParity(java, kotlin)

        // Spot-check
        assertEquals(false, kotlin.censorshipEnabled)
        assertEquals(50, kotlin.unknownCharsPercent)
        assertEquals(listOf("alice", "bob"), kotlin.userWhitelist)
        assertEquals(listOf("eve"), kotlin.userBlacklist)
        assertEquals(listOf("bad", "worse"), kotlin.bannedWords)
    }

    // ---- parity: empty lists ------------------------------------------

    @Test
    fun `empty lists produce parity with Java`() {
        val props = buildProperties(whitelist = "", blacklist = "", banned = "")
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configCensorFromJava(java)

        // Java splits "" by comma -> [""] (single empty string array)
        // Our adapter mirrors this faithfully
        assertEquals(java.userWhitelist?.toList() ?: emptyList(), kotlin.userWhitelist)
        assertEquals(java.userBlacklist?.toList() ?: emptyList(), kotlin.userBlacklist)
        assertEquals(java.bannedWords?.toList() ?: emptyList(), kotlin.bannedWords)
    }
}
