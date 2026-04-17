package com.glitchcog.fontificator.core

import com.glitchcog.fontificator.config.ConfigMessage as JavaConfigMessage
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.MessageCasing as JavaMessageCasing
import com.glitchcog.fontificator.config.UsernameCaseResolutionType as JavaUsernameCaseResolutionType
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stage S4 differential-parity gate for `ConfigMessage`.
 *
 * Constructs a Java `ConfigMessage` via its `load(Properties, ...)`
 * path (and also via direct setter mutations), converts it to the
 * commonMain port via `JavaLegacyAdapter.configMessageFromJava`, and
 * asserts field-by-field parity.
 */
class ConfigMessageJvmParityTest {

    /**
     * Build a Java `Properties` with every message key populated.
     * Defaults mirror `FontificatorProperties.applyDefaults()` for
     * the message section.
     */
    private fun buildProperties(
        usernameFormat: String = JavaConfigMessage.USERNAME_REPLACE,
        timeFormat: String = "[HH:mm:ss]",
        contentBreak: String = JavaConfigMessage.DEFAULT_CONTENT_BREAKER,
        queueSize: Int = 64,
        messageSpeed: Int = 30,
        expirationTime: Int = 0,
        includeTimestamps: Boolean = false,
        showUsernames: Boolean = true,
        showJoinMessages: Boolean = false,
        hideEmptyBorder: Boolean = false,
        hideEmptyBackground: Boolean = false,
        caseResolutionType: JavaUsernameCaseResolutionType = JavaUsernameCaseResolutionType.NONE,
        specifyCaseAllowed: Boolean = false,
        messageCasing: JavaMessageCasing = JavaMessageCasing.MIXED_CASE,
    ): Properties {
        val p = Properties()
        p.setProperty(FontificatorProperties.KEY_MESSAGE_JOIN, showJoinMessages.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_USERNAME, showUsernames.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_TIMESTAMP, includeTimestamps.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_USERFORMAT, usernameFormat)
        p.setProperty(FontificatorProperties.KEY_MESSAGE_TIMEFORMAT, timeFormat)
        p.setProperty(FontificatorProperties.KEY_MESSAGE_CONTENT_BREAK, contentBreak)
        p.setProperty(FontificatorProperties.KEY_MESSAGE_QUEUE_SIZE, queueSize.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_SPEED, messageSpeed.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_EXPIRATION_TIME, expirationTime.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_HIDE_EMPTY_BORDER, hideEmptyBorder.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_HIDE_EMPTY_BACKGROUND, hideEmptyBackground.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_CASE_TYPE, caseResolutionType.name)
        p.setProperty(FontificatorProperties.KEY_MESSAGE_CASE_SPECIFY, specifyCaseAllowed.toString())
        p.setProperty(FontificatorProperties.KEY_MESSAGE_CASING, messageCasing.name)
        return p
    }

    private fun loadJavaConfig(props: Properties): JavaConfigMessage {
        val javaConfig = JavaConfigMessage()
        val report = LoadConfigReport()
        javaConfig.load(props, report)
        assertTrue(report.isErrorFree, "Java load had errors: ${report.messages}")
        return javaConfig
    }

    /**
     * Assert every field of the Kotlin port matches the Java
     * original.  Covers every getter exposed by the frozen Java
     * `ConfigMessage`.
     */
    private fun assertFieldParity(java: JavaConfigMessage, kotlin: ConfigMessage) {
        assertEquals(java.usernameFormat, kotlin.usernameFormat, "usernameFormat mismatch")
        assertEquals(java.timeFormat, kotlin.timeFormat, "timeFormat mismatch")
        assertEquals(java.contentBreaker, kotlin.messageContentBreak, "messageContentBreak mismatch")
        assertEquals(java.queueSize, kotlin.queueSize, "queueSize mismatch")
        assertEquals(java.messageSpeed, kotlin.messageSpeed, "messageSpeed mismatch")
        assertEquals(java.expirationTime, kotlin.expirationTime, "expirationTime mismatch")
        assertEquals(java.showTimestamps(), kotlin.includeTimestamps, "includeTimestamps mismatch")
        assertEquals(java.showUsernames(), kotlin.showUsernamesOnMessages, "showUsernamesOnMessages mismatch")
        assertEquals(java.showJoinMessages(), kotlin.showJoinMessages, "showJoinMessages mismatch")
        assertEquals(java.isHideEmptyBorder, kotlin.hideEmptyBorder, "hideEmptyBorder mismatch")
        assertEquals(java.isHideEmptyBackground, kotlin.hideEmptyBackground, "hideEmptyBackground mismatch")
        assertEquals(java.caseResolutionType.name, kotlin.caseResolutionType.name, "caseResolutionType mismatch")
        assertEquals(java.isSpecifyCaseAllowed, kotlin.specifyCaseAllowed, "specifyCaseAllowed mismatch")
        assertEquals(java.messageCasing.name, kotlin.messageCasing.name, "messageCasing mismatch")
    }

    // ---- parity: default config ---------------------------------------

    @Test
    fun `default config -- adapter produces field-exact parity`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configMessageFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: every field set to non-default values -----------------

    @Test
    fun `non-default fields -- adapter produces field-exact parity`() {
        val props = buildProperties(
            usernameFormat = "<%user%>",
            timeFormat = "yyyy-MM-dd HH:mm:ss",
            contentBreak = " >> ",
            queueSize = 256,
            messageSpeed = 60,
            expirationTime = 120,
            includeTimestamps = true,
            showUsernames = false,
            showJoinMessages = true,
            hideEmptyBorder = true,
            hideEmptyBackground = true,
            caseResolutionType = JavaUsernameCaseResolutionType.ALL_CAPS,
            specifyCaseAllowed = true,
            messageCasing = JavaMessageCasing.UPPERCASE,
        )
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configMessageFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: setter mutations across every setter -----------------

    @Test
    fun `Java setter mutations across every setter are reflected by adapter`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)

        // Mutate via every Java setter (messageSpeed setter requires a
        // MessageProgressor; passing null is legal per Java impl).
        java.setJoinMessages(true)
        java.setShowUsernames(false)
        java.setShowTimestamps(true)
        java.usernameFormat = "[%user%]"
        java.timeFormat = "HH:mm"
        java.contentBreaker = " | "
        java.queueSize = 500
        java.setMessageSpeed(90, null)
        java.setExpirationTime(250, null)
        java.setHideEmptyBorder(true)
        java.setHideEmptyBackground(true)
        java.caseResolutionType = JavaUsernameCaseResolutionType.LOOKUP
        java.setSpecifyCaseAllowed(true)
        java.messageCasing = JavaMessageCasing.LOWERCASE

        val kotlin = JavaLegacyAdapter.configMessageFromJava(java)
        assertFieldParity(java, kotlin)

        // Spot-check each mutated value landed in the Kotlin port.
        assertEquals(true, kotlin.showJoinMessages)
        assertEquals(false, kotlin.showUsernamesOnMessages)
        assertEquals(true, kotlin.includeTimestamps)
        assertEquals("[%user%]", kotlin.usernameFormat)
        assertEquals("HH:mm", kotlin.timeFormat)
        assertEquals(" | ", kotlin.messageContentBreak)
        assertEquals(500, kotlin.queueSize)
        assertEquals(90, kotlin.messageSpeed)
        assertEquals(250, kotlin.expirationTime)
        assertEquals(true, kotlin.hideEmptyBorder)
        assertEquals(true, kotlin.hideEmptyBackground)
        assertEquals(UsernameCaseResolutionType.LOOKUP, kotlin.caseResolutionType)
        assertEquals(true, kotlin.specifyCaseAllowed)
        assertEquals(MessageCasing.LOWERCASE, kotlin.messageCasing)
    }

    // ---- parity: every enum constant round-trips ----------------------

    @Test
    fun `every MessageCasing constant round-trips`() {
        for (jc in JavaMessageCasing.values()) {
            val props = buildProperties(messageCasing = jc)
            val java = loadJavaConfig(props)
            val kotlin = JavaLegacyAdapter.configMessageFromJava(java)
            assertEquals(jc.name, kotlin.messageCasing.name, "MessageCasing.${jc.name} mismatch")
        }
    }

    @Test
    fun `every UsernameCaseResolutionType constant round-trips`() {
        for (jc in JavaUsernameCaseResolutionType.values()) {
            val props = buildProperties(caseResolutionType = jc)
            val java = loadJavaConfig(props)
            val kotlin = JavaLegacyAdapter.configMessageFromJava(java)
            assertEquals(jc.name, kotlin.caseResolutionType.name, "UsernameCaseResolutionType.${jc.name} mismatch")
        }
    }

    // ---- parity: §9 space-allowed divider via the Java loader ---------

    @Test
    fun `space-allowed divider -- single space contentBreak survives Java load`() {
        val props = buildProperties(contentBreak = " ")
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configMessageFromJava(java)
        assertEquals(" ", java.contentBreaker)
        assertEquals(" ", kotlin.messageContentBreak)
    }
}
