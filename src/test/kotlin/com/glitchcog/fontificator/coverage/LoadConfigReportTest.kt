package com.glitchcog.fontificator.coverage

import com.glitchcog.fontificator.config.loadreport.LoadConfigErrorType
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoadConfigReportTest {

    @Test
    fun default_ctor_is_empty_and_success() {
        val r = LoadConfigReport()
        assertTrue(r.isErrorFree)
        assertTrue(r.isSuccess)
        assertFalse(r.isProblem)
        assertTrue(r.isOnlyMissingKeys)
        assertTrue(r.messages.isEmpty())
        assertTrue(r.types.isEmpty())
        assertNull(r.mainMessage)
        assertNull(r.directory)
    }

    @Test
    fun single_type_ctor_populates_types() {
        val r = LoadConfigReport(LoadConfigErrorType.MISSING_KEY)
        assertTrue(r.isErrorFree) // messages empty
        assertFalse(r.isProblem)  // MISSING_KEY is not a problem
        assertTrue(r.isOnlyMissingKeys)
        assertTrue(r.types.contains(LoadConfigErrorType.MISSING_KEY))
    }

    @Test
    fun addError_flags_problem_correctly() {
        val r = LoadConfigReport()
        r.addError("bad int", LoadConfigErrorType.PARSE_ERROR_INT)
        assertFalse(r.isErrorFree)
        assertTrue(r.isProblem)
        assertFalse(r.isSuccess)
        assertFalse(r.isOnlyMissingKeys)
        assertEquals(1, r.messages.size)
    }

    @Test
    fun missing_key_only_is_not_a_problem_below_threshold() {
        val r = LoadConfigReport()
        // 15 or fewer non-problems should NOT be flagged
        repeat(15) { r.addError("missing $it", LoadConfigErrorType.MISSING_KEY) }
        assertFalse(r.isProblem)
        assertTrue(r.isOnlyMissingKeys)
    }

    @Test
    fun too_many_non_problem_errors_trip_problem_flag() {
        val r = LoadConfigReport()
        // > MAX_NUMBER_OF_NON_PROBLEMS (15) — triggers the count-based branch
        repeat(16) { r.addError("missing $it", LoadConfigErrorType.MISSING_KEY) }
        assertTrue(r.isProblem)
    }

    @Test
    fun isOnlyMissingKeys_returns_false_when_mixed_types() {
        val r = LoadConfigReport()
        r.addError("a", LoadConfigErrorType.MISSING_KEY)
        r.addError("b", LoadConfigErrorType.PARSE_ERROR_INT)
        assertFalse(r.isOnlyMissingKeys)
    }

    @Test
    fun addFromReport_merges_both_messages_and_types() {
        val a = LoadConfigReport()
        a.addError("a1", LoadConfigErrorType.MISSING_KEY)
        val b = LoadConfigReport()
        b.addError("b1", LoadConfigErrorType.PARSE_ERROR_FLOAT)
        b.addError("b2", LoadConfigErrorType.PARSE_ERROR_INT)

        a.addFromReport(b)
        assertEquals(3, a.messages.size)
        assertTrue(a.types.contains(LoadConfigErrorType.PARSE_ERROR_FLOAT))
        assertTrue(a.types.contains(LoadConfigErrorType.PARSE_ERROR_INT))
        assertTrue(a.isProblem)
    }

    @Test
    fun mainMessage_and_directory_round_trip() {
        val r = LoadConfigReport()
        r.mainMessage = "hello"
        r.directory = "/tmp/foo"
        assertEquals("hello", r.mainMessage)
        assertEquals("/tmp/foo", r.directory)
    }
}
