package com.glitchcog.fontificator.core

import com.glitchcog.fontificator.config.ConfigChat as JavaConfigChat
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stage S4 differential-parity gate for `ConfigChat`.
 *
 * Constructs a Java `ConfigChat` via its `load(Properties, ...)`
 * path, converts it to the commonMain port via
 * `JavaLegacyAdapter.configChatFromJava`, and asserts field-by-field
 * parity.
 */
class ConfigChatJvmParityTest {

    private fun buildProperties(
        scrollable: Boolean = false,
        resizable: Boolean = true,
        rememberPosition: Boolean = false,
        posX: Int = 0,
        posY: Int = 0,
        chatFromBottom: Boolean = false,
        width: Int = 550,
        height: Int = 450,
        chromaEnabled: Boolean = false,
        chromaInvert: Boolean = false,
        reverseScrolling: Boolean = false,
        chromaLeft: Int = 10,
        chromaTop: Int = 10,
        chromaRight: Int = 10,
        chromaBottom: Int = 10,
        chromaCorner: Int = 10,
        alwaysOnTop: Boolean = false,
        antiAlias: Boolean = false,
    ): Properties {
        val p = Properties()
        p.setProperty(FontificatorProperties.KEY_CHAT_SCROLL, scrollable.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_RESIZABLE, resizable.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_POSITION, rememberPosition.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_POSITION_X, posX.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_POSITION_Y, posY.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_FROM_BOTTOM, chatFromBottom.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_WIDTH, width.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_HEIGHT, height.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_CHROMA_ENABLED, chromaEnabled.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_INVERT_CHROMA, chromaInvert.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_REVERSE_SCROLLING, reverseScrolling.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_CHROMA_LEFT, chromaLeft.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_CHROMA_TOP, chromaTop.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_CHROMA_RIGHT, chromaRight.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_CHROMA_BOTTOM, chromaBottom.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_CHROMA_CORNER, chromaCorner.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_ALWAYS_ON_TOP, alwaysOnTop.toString())
        p.setProperty(FontificatorProperties.KEY_CHAT_ANTIALIAS, antiAlias.toString())
        return p
    }

    private fun loadJavaConfig(props: Properties): JavaConfigChat {
        val javaConfig = JavaConfigChat()
        val report = LoadConfigReport()
        javaConfig.load(props, report)
        assertTrue(report.isErrorFree, "Java load had errors: ${report.messages}")
        return javaConfig
    }

    private fun assertFieldParity(java: JavaConfigChat, kotlin: ConfigChat) {
        assertEquals(java.isScrollable, kotlin.scrollable, "scrollable mismatch")
        assertEquals(java.isResizable, kotlin.resizable, "resizable mismatch")
        assertEquals(java.isRememberPosition, kotlin.rememberPosition, "rememberPosition mismatch")
        assertEquals(java.chatWindowPositionX, kotlin.chatWindowPositionX, "chatWindowPositionX mismatch")
        assertEquals(java.chatWindowPositionY, kotlin.chatWindowPositionY, "chatWindowPositionY mismatch")
        assertEquals(java.isChatFromBottom, kotlin.chatFromBottom, "chatFromBottom mismatch")
        assertEquals(java.width, kotlin.width, "width mismatch")
        assertEquals(java.height, kotlin.height, "height mismatch")
        assertEquals(java.isChromaEnabled, kotlin.chromaEnabled, "chromaEnabled mismatch")
        assertEquals(java.isChromaInvert, kotlin.chromaInvert, "chromaInvert mismatch")
        assertEquals(java.chromaBorder.x, kotlin.chromaLeft, "chromaLeft mismatch")
        assertEquals(java.chromaBorder.y, kotlin.chromaTop, "chromaTop mismatch")
        assertEquals(java.chromaBorder.width, kotlin.chromaRight, "chromaRight mismatch")
        assertEquals(java.chromaBorder.height, kotlin.chromaBottom, "chromaBottom mismatch")
        assertEquals(java.chromaCornerRadius, kotlin.chromaCornerRadius, "chromaCornerRadius mismatch")
        assertEquals(java.isReverseScrolling, kotlin.reverseScrolling, "reverseScrolling mismatch")
        assertEquals(java.isAlwaysOnTop, kotlin.alwaysOnTop, "alwaysOnTop mismatch")
        assertEquals(java.isAntiAlias, kotlin.antiAlias, "antiAlias mismatch")
    }

    // ---- parity: default config ---------------------------------------

    @Test
    fun `default config -- adapter produces field-exact parity`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configChatFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: every field set to non-default values -----------------

    @Test
    fun `non-default fields -- adapter produces field-exact parity`() {
        val props = buildProperties(
            scrollable = true,
            resizable = false,
            rememberPosition = true,
            posX = 100,
            posY = 200,
            chatFromBottom = true,
            width = 800,
            height = 600,
            chromaEnabled = true,
            chromaInvert = true,
            reverseScrolling = true,
            chromaLeft = 5,
            chromaTop = 15,
            chromaRight = 25,
            chromaBottom = 35,
            chromaCorner = 64,
            alwaysOnTop = true,
            antiAlias = true,
        )
        val java = loadJavaConfig(props)
        val kotlin = JavaLegacyAdapter.configChatFromJava(java)
        assertFieldParity(java, kotlin)
    }

    // ---- parity: setter mutations -------------------------------------

    @Test
    fun `Java setter mutations are reflected by adapter`() {
        val props = buildProperties()
        val java = loadJavaConfig(props)

        java.isScrollable = true
        java.isResizable = false
        java.isRememberPosition = true
        java.chatWindowPositionX = 42
        java.chatWindowPositionY = 84
        java.isChatFromBottom = true
        java.width = 1024
        java.height = 768
        java.isChromaEnabled = true
        java.isChromaInvert = true
        java.setChromaBorder(1, 2, 3, 4)
        java.chromaCornerRadius = 50
        java.isReverseScrolling = true
        java.isAlwaysOnTop = true
        java.isAntiAlias = true

        val kotlin = JavaLegacyAdapter.configChatFromJava(java)
        assertFieldParity(java, kotlin)

        // Spot-check mutated values
        assertEquals(true, kotlin.scrollable)
        assertEquals(1024, kotlin.width)
        assertEquals(50, kotlin.chromaCornerRadius)
        assertEquals(1, kotlin.chromaLeft)
        assertEquals(2, kotlin.chromaTop)
        assertEquals(3, kotlin.chromaRight)
        assertEquals(4, kotlin.chromaBottom)
    }
}
