package com.glitchcog.fontificator.review

import com.glitchcog.fontificator.config.ConfigFont
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals

/**
 * C1: `setBaselineOffset` must persist to the backing Properties map.
 *
 * Original bug at `ConfigFont.java:369-373` called `props.getProperty(KEY, default)` —
 * a read with a default — instead of `props.setProperty(KEY, value)`, so the user's
 * change was silently discarded on save.
 */
class ConfigFontRoundTripTest {

    @Test
    fun set_baseline_offset_persists_to_properties() {
        val config = ConfigFont()
        val props = Properties()

        // Seed the backing props map via load() so ConfigFont.props is assigned.
        // An easier path: bypass load() by calling a setter that writes to props,
        // but ConfigFont.props is protected and only assigned in load(). We fake
        // a minimal properties map and invoke load() to wire it up.
        seedMinimalFontProps(props)
        val report = LoadConfigReport()
        config.load(props, report)

        // Act
        config.setBaselineOffset(5)

        // Assert: the props map must reflect the new value.
        assertEquals(
            "5",
            props.getProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET),
            "setBaselineOffset must persist the value to the Properties map"
        )
    }

    private fun seedMinimalFontProps(props: Properties) {
        // Provide every FONT_KEYS entry so load() proceeds far enough to assign `this.props = props`.
        // Values chosen so validation passes (referencing internal preset paths is avoided by using
        // only the ConfigFont.props wiring — validation errors are tolerated if props is still set).
        props.setProperty(FontificatorProperties.KEY_FONT_FILE_BORDER, "preset://borders/none.png")
        props.setProperty(FontificatorProperties.KEY_FONT_FILE_FONT, "preset://fonts/none.png")
        props.setProperty(FontificatorProperties.KEY_FONT_TYPE, "FIXED_WIDTH")
        props.setProperty(FontificatorProperties.KEY_FONT_GRID_WIDTH, "1")
        props.setProperty(FontificatorProperties.KEY_FONT_GRID_HEIGHT, "1")
        props.setProperty(FontificatorProperties.KEY_FONT_SCALE, "1.0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_SCALE, "1.0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_X, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_Y, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACE_WIDTH, "100")
        props.setProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, "A")
        props.setProperty(FontificatorProperties.KEY_FONT_EXTENDED_CHAR, "false")
        props.setProperty(FontificatorProperties.KEY_FONT_CHARACTERS, "A")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_LINE, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_CHAR, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_MESSAGE, "0")
    }
}
