package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Stage S4 differential-parity gate for [baseValidation].
 *
 * Each fixture is executed through both the commonMain pure-Kotlin
 * [baseValidation] and the frozen Java
 * [JavaLegacyAdapter.baseValidationViaJava].  The error lists must match
 * exactly.
 */
class BaseValidationJvmParityTest {

    // The two canonical space-allowed keys matching the hardcoded Java
    // constants FontificatorProperties.KEY_FONT_UNKNOWN_CHAR and
    // FontificatorProperties.KEY_MESSAGE_CONTENT_BREAK.
    private val spaceAllowedKeys = setOf("fontUnknownChar", "messageContentBreak")

    /**
     * Run both implementations and assert exact parity.
     */
    private fun assertParity(
        props: Map<String, String>,
        keys: List<String>,
        spaceAllowed: Set<String> = spaceAllowedKeys,
    ) {
        val kotlinErrors = baseValidation(props, keys, spaceAllowed)
        val javaErrors = JavaLegacyAdapter.baseValidationViaJava(props, keys)
        assertEquals(
            javaErrors, kotlinErrors,
            "Parity failure.\n  Java:   $javaErrors\n  Kotlin: $kotlinErrors"
        )
    }

    // ---- pinned 83155b4 cases -------------------------------------------

    @Test
    fun `parity -- unknown_char single space accepted`() {
        assertParity(
            props = mapOf("fontUnknownChar" to " "),
            keys = listOf("fontUnknownChar"),
        )
    }

    @Test
    fun `parity -- divider single space accepted`() {
        assertParity(
            props = mapOf("messageContentBreak" to " "),
            keys = listOf("messageContentBreak"),
        )
    }

    @Test
    fun `parity -- empty string rejected for space-allowed key`() {
        assertParity(
            props = mapOf("fontUnknownChar" to ""),
            keys = listOf("fontUnknownChar"),
        )
    }

    // ---- missing key ----------------------------------------------------

    @Test
    fun `parity -- missing key`() {
        assertParity(
            props = emptyMap(),
            keys = listOf("someKey"),
            spaceAllowed = emptySet(),
        )
    }

    // ---- blank value for non-space-allowed key --------------------------

    @Test
    fun `parity -- empty value for normal key`() {
        assertParity(
            props = mapOf("normalKey" to ""),
            keys = listOf("normalKey"),
            spaceAllowed = emptySet(),
        )
    }

    @Test
    fun `parity -- whitespace-only value for normal key`() {
        assertParity(
            props = mapOf("normalKey" to "   "),
            keys = listOf("normalKey"),
            spaceAllowed = emptySet(),
        )
    }

    // ---- multi-char whitespace for space-allowed key --------------------

    @Test
    fun `parity -- multi-space value for space-allowed key`() {
        assertParity(
            props = mapOf("fontUnknownChar" to "  "),
            keys = listOf("fontUnknownChar"),
        )
    }

    // ---- normal non-empty values pass -----------------------------------

    @Test
    fun `parity -- normal non-empty value`() {
        assertParity(
            props = mapOf("someKey" to "hello"),
            keys = listOf("someKey"),
            spaceAllowed = emptySet(),
        )
    }

    @Test
    fun `parity -- multiple keys mixed valid and invalid`() {
        assertParity(
            props = mapOf(
                "good" to "ok",
                "bad" to "",
            ),
            keys = listOf("good", "bad", "absent"),
            spaceAllowed = emptySet(),
        )
    }

    @Test
    fun `parity -- all keys valid`() {
        assertParity(
            props = mapOf(
                "key1" to "v1",
                "key2" to "v2",
                "fontUnknownChar" to " ",
            ),
            keys = listOf("key1", "key2", "fontUnknownChar"),
        )
    }
}
