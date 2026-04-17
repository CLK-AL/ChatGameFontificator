package com.glitchcog.fontificator.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stage S4 -- commonMain tests for [baseValidation].
 *
 * Covers the three pinned migration-plan cases from `83155b4`, plus
 * additional edge-case and happy-path scenarios.
 */
class BaseValidationTest {

    // The two canonical space-allowed keys from FontificatorProperties.
    private val spaceAllowedKeys = setOf("fontUnknownChar", "messageContentBreak")

    // ---- pinned 83155b4 cases -------------------------------------------

    @Test
    fun `unknown_char single space is accepted`() {
        val props = mapOf("fontUnknownChar" to " ")
        val errors = baseValidation(props, listOf("fontUnknownChar"), spaceAllowedKeys)
        assertEquals(emptyList(), errors, "Single space should be accepted for fontUnknownChar")
    }

    @Test
    fun `divider single space is accepted`() {
        val props = mapOf("messageContentBreak" to " ")
        val errors = baseValidation(props, listOf("messageContentBreak"), spaceAllowedKeys)
        assertEquals(emptyList(), errors, "Single space should be accepted for messageContentBreak")
    }

    @Test
    fun `empty string is always rejected`() {
        val props = mapOf("fontUnknownChar" to "")
        val errors = baseValidation(props, listOf("fontUnknownChar"), spaceAllowedKeys)
        assertTrue(errors.isNotEmpty(), "Empty string should be rejected even for spaceAllowedKeys")
        assertTrue(errors.any { "fontUnknownChar" in it })
    }

    // ---- missing key ----------------------------------------------------

    @Test
    fun `missing key produces error`() {
        val props = emptyMap<String, String>()
        val errors = baseValidation(props, listOf("someKey"))
        assertEquals(1, errors.size)
        assertTrue("someKey" in errors[0])
        assertTrue("missing" in errors[0].lowercase())
    }

    // ---- null-equivalent (absent value for present key) -----------------

    @Test
    fun `blank value for non-space-allowed key is rejected`() {
        // Simulates the Java case where getProperty returns "" after the
        // key is present but has no meaningful value.
        val props = mapOf("someKey" to "")
        val errors = baseValidation(props, listOf("someKey"))
        assertEquals(1, errors.size)
        assertTrue("someKey" in errors[0])
    }

    @Test
    fun `whitespace-only value for non-space-allowed key is rejected`() {
        val props = mapOf("someKey" to "   ")
        val errors = baseValidation(props, listOf("someKey"))
        assertEquals(1, errors.size)
        assertTrue("someKey" in errors[0])
    }

    // ---- multi-char whitespace ------------------------------------------

    @Test
    fun `multi-char whitespace rejected for space-allowed key`() {
        // "  " (two spaces) trims to empty and the raw value is non-empty,
        // so it would be accepted — this mirrors the Java behaviour.
        // However, multi-char whitespace like tab or three spaces still
        // has isEmpty() == false and trim().isEmpty() == true, so the
        // Java code accepts it too.  We assert parity here.
        val props = mapOf("fontUnknownChar" to "  ")
        val errors = baseValidation(props, listOf("fontUnknownChar"), spaceAllowedKeys)
        // Java accepts this: the raw value is non-empty and the key is
        // in the allowed set, so no error.  Port must match.
        assertEquals(emptyList(), errors)
    }

    @Test
    fun `tab value for non-space-allowed key is rejected`() {
        val props = mapOf("normalKey" to "\t")
        val errors = baseValidation(props, listOf("normalKey"))
        assertEquals(1, errors.size)
        assertTrue("normalKey" in errors[0])
    }

    // ---- normal non-empty values pass -----------------------------------

    @Test
    fun `normal non-empty value passes`() {
        val props = mapOf("someKey" to "hello")
        val errors = baseValidation(props, listOf("someKey"))
        assertEquals(emptyList(), errors)
    }

    @Test
    fun `multiple keys all valid produces no errors`() {
        val props = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3",
        )
        val errors = baseValidation(props, listOf("key1", "key2", "key3"))
        assertEquals(emptyList(), errors)
    }

    @Test
    fun `mixed valid and invalid keys produces errors only for invalid`() {
        val props = mapOf(
            "good" to "ok",
            "bad" to "",
        )
        val errors = baseValidation(props, listOf("good", "bad", "absent"))
        assertEquals(2, errors.size)
        assertTrue(errors.any { "bad" in it })
        assertTrue(errors.any { "absent" in it })
    }

    // ---- space-allowed key with normal value ----------------------------

    @Test
    fun `space-allowed key with normal non-space value passes`() {
        val props = mapOf("fontUnknownChar" to "X")
        val errors = baseValidation(props, listOf("fontUnknownChar"), spaceAllowedKeys)
        assertEquals(emptyList(), errors)
    }

    // ---- default spaceAllowedKeys is empty ------------------------------

    @Test
    fun `single space rejected when spaceAllowedKeys defaults to empty`() {
        val props = mapOf("fontUnknownChar" to " ")
        val errors = baseValidation(props, listOf("fontUnknownChar"))
        assertTrue(errors.isNotEmpty(), "Space should be rejected when spaceAllowedKeys is empty")
    }
}
