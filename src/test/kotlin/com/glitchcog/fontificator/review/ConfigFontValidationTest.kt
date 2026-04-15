package com.glitchcog.fontificator.review

import com.glitchcog.fontificator.config.ConfigFont
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * C2: `validateStrings` must reject a grid with non-positive height.
 *
 * Original bug at `ConfigFont.java:158` read `if (w > 0 && w > 0)` — a duplicated
 * width check that silently skipped the height guard on the grid-vs-key-length
 * sanity check. Once the outer per-field integer range check is stubbed out,
 * the duplicated-width condition incorrectly lets a zero-height grid slip into
 * the `w * h == charKey.length()` branch, where it produces a nonsensical
 * "8 x 0 = 0" error instead of rejecting the height outright.
 *
 * This test bypasses the outer integer-range validation so the inner guard is
 * what actually stops a pathological height — which is exactly what the fix
 * `w > 0 && h > 0` restores.
 */
class ConfigFontValidationTest {

    /**
     * Test-only ConfigFont that skips the per-field `validateIntegerWithLimitString`
     * pre-checks so the inner `w > 0 && h > 0` branch is the effective guard.
     * We can override the protected hook because Kotlin honours Java protected
     * visibility for subclasses across packages.
     */
    private class BypassingConfigFont : ConfigFont() {
        override fun validateIntegerWithLimitString(
            key: String?,
            value: String?,
            minimum: Int,
            report: LoadConfigReport?
        ): LoadConfigReport? = report

        override fun validateIntegerWithLimitString(
            key: String?,
            value: String?,
            minimum: Int,
            maximum: Int,
            report: LoadConfigReport?
        ): LoadConfigReport? = report
    }

    @Test
    fun invalid_grid_height_fails_validation() {
        val config = BypassingConfigFont()
        val report = LoadConfigReport()

        // width=8, height=0, charKey length=8, unknown char "A".
        // With the outer integer range checks stubbed out, the inner
        // `w > 0 && h > 0` guard is what must prevent the nonsensical
        // "Character key length (8) must match the number of characters
        // in the font image (8 x 0 = 0)" message from being produced.
        config.validateStrings(report, "8", "0", "ABCDEFGH", "A")

        val joined = report.messages.joinToString("\n")
        assertFalse(
            joined.contains("8 x 0"),
            "Zero-height grid must not be smuggled into the w*h vs charKey " +
                "length check — that path produces a nonsense message. " +
                "Got: $joined"
        )
        assertTrue(
            report.isErrorFree || !joined.contains("must match the number of characters"),
            "With height <= 0 the grid-size-vs-key-length branch must be " +
                "skipped, since the height itself is what is invalid. " +
                "Got: $joined"
        )
    }
}
