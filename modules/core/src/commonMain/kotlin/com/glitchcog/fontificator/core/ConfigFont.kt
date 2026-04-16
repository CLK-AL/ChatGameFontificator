package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin immutable port of
 * `com.glitchcog.fontificator.config.ConfigFont`.
 *
 * The frozen Java class uses mutable fields backed by a `Properties`
 * object (each setter writes both the field and the property).
 * This commonMain port replaces that pattern with an immutable data
 * class plus explicit `toProperties()` / `fromProperties()` for
 * round-trip serialization.
 *
 * ### Carried fixes
 * - **C1** (`setBaselineOffset` -> `setProperty`): The original Java had
 *   a period where the setter forgot to persist `baselineOffset`.
 *   The immutable design makes such a bug structurally impossible --
 *   `toProperties()` always serialises every field.
 * - **C2** (`w > 0 && h > 0`): `validate()` checks grid dimensions are
 *   positive before computing `w * h`, matching the fixed Java logic.
 */
public data class ConfigFont(
    val fontFilename: String,
    val borderFilename: String,
    val gridWidth: Int,
    val gridHeight: Int,
    val fontScale: Float,
    val borderScale: Float,
    val borderInsetX: Int,
    val borderInsetY: Int,
    val spaceWidth: Int,
    val baselineOffset: Int,
    val characterKey: String,
    val unknownChar: Char,
    val extendedCharEnabled: Boolean,
    val lineSpacing: Int,
    val charSpacing: Int,
    val messageSpacing: Int,
    val fontType: FontType,
) {

    /**
     * Validate this configuration, replicating the Java
     * `ConfigFont.validateStrings()` logic.
     *
     * Returns a list of human-readable error strings. An empty list
     * means the configuration is valid.
     */
    public fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // --- file checks ---
        if (fontFilename.isEmpty()) {
            errors += "A font filename is required"
        }
        if (borderFilename.isEmpty()) {
            errors += "A border filename is required"
        }

        // --- unknown char must be single character (always true for Char, but guard the key membership) ---
        val unknownStr = unknownChar.toString()

        // --- grid dimensions ---
        if (gridWidth < 1) {
            errors += "Value of key \"$KEY_FONT_GRID_WIDTH\" must be at least 1"
        }
        if (gridHeight < 1) {
            errors += "Value of key \"$KEY_FONT_GRID_HEIGHT\" must be at least 1"
        }

        // --- C2 fix: only check key length when both dimensions are positive ---
        if (gridWidth > 0 && gridHeight > 0) {
            val expected = gridWidth * gridHeight
            if (expected != characterKey.length) {
                errors += "Character key length (${characterKey.length}) must match the number of characters in the font image ($gridWidth x $gridHeight = $expected)"
            }
        }

        // unknown char must be present in the character key
        if (!characterKey.contains(unknownStr)) {
            errors += "The value for $KEY_FONT_UNKNOWN_CHAR ($unknownStr) must also be in the value for $KEY_FONT_CHARACTERS"
        }

        // --- numeric range checks ---
        if (fontScale < MIN_FONT_SCALE || fontScale > MAX_FONT_SCALE) {
            errors += "Value of key \"$KEY_FONT_SCALE\" is out of range [$MIN_FONT_SCALE, $MAX_FONT_SCALE]"
        }
        if (borderScale < MIN_BORDER_SCALE || borderScale > MAX_BORDER_SCALE) {
            errors += "Value of key \"$KEY_FONT_BORDER_SCALE\" is out of range [$MIN_BORDER_SCALE, $MAX_BORDER_SCALE]"
        }
        if (borderInsetX < MIN_BORDER_INSET || borderInsetX > MAX_BORDER_INSET) {
            errors += "Value of key \"$KEY_FONT_BORDER_INSET_X\" is out of range [$MIN_BORDER_INSET, $MAX_BORDER_INSET]"
        }
        if (borderInsetY < MIN_BORDER_INSET || borderInsetY > MAX_BORDER_INSET) {
            errors += "Value of key \"$KEY_FONT_BORDER_INSET_Y\" is out of range [$MIN_BORDER_INSET, $MAX_BORDER_INSET]"
        }
        if (spaceWidth < MIN_SPACE_WIDTH || spaceWidth > MAX_SPACE_WIDTH) {
            errors += "Value of key \"$KEY_FONT_SPACE_WIDTH\" is out of range [$MIN_SPACE_WIDTH, $MAX_SPACE_WIDTH]"
        }
        if (baselineOffset < MIN_BASELINE_OFFSET || baselineOffset > MAX_BASELINE_OFFSET) {
            errors += "Value of key \"$KEY_FONT_BASELINE_OFFSET\" is out of range [$MIN_BASELINE_OFFSET, $MAX_BASELINE_OFFSET]"
        }
        if (lineSpacing < MIN_LINE_SPACING || lineSpacing > MAX_LINE_SPACING) {
            errors += "Value of key \"$KEY_FONT_SPACING_LINE\" is out of range [$MIN_LINE_SPACING, $MAX_LINE_SPACING]"
        }
        if (charSpacing < MIN_CHAR_SPACING || charSpacing > MAX_CHAR_SPACING) {
            errors += "Value of key \"$KEY_FONT_SPACING_CHAR\" is out of range [$MIN_CHAR_SPACING, $MAX_CHAR_SPACING]"
        }
        if (messageSpacing < MIN_MESSAGE_SPACING || messageSpacing > MAX_MESSAGE_SPACING) {
            errors += "Value of key \"$KEY_FONT_SPACING_MESSAGE\" is out of range [$MIN_MESSAGE_SPACING, $MAX_MESSAGE_SPACING]"
        }

        return errors
    }

    /**
     * Serialize every field to a flat `Map<String, String>`,
     * mirroring the Java `Properties`-backed setter pattern.
     *
     * C1 fix: every field is serialised unconditionally -- there is
     * no "forgot to call setProperty" risk with this design.
     */
    public fun toProperties(): Map<String, String> = mapOf(
        KEY_FONT_FILE_FONT to fontFilename,
        KEY_FONT_FILE_BORDER to borderFilename,
        KEY_FONT_GRID_WIDTH to gridWidth.toString(),
        KEY_FONT_GRID_HEIGHT to gridHeight.toString(),
        KEY_FONT_SCALE to fontScale.toString(),
        KEY_FONT_BORDER_SCALE to borderScale.toString(),
        KEY_FONT_BORDER_INSET_X to borderInsetX.toString(),
        KEY_FONT_BORDER_INSET_Y to borderInsetY.toString(),
        KEY_FONT_SPACE_WIDTH to spaceWidth.toString(),
        KEY_FONT_BASELINE_OFFSET to baselineOffset.toString(),
        KEY_FONT_CHARACTERS to characterKey,
        KEY_FONT_UNKNOWN_CHAR to unknownChar.toString(),
        KEY_FONT_EXTENDED_CHAR to extendedCharEnabled.toString(),
        KEY_FONT_SPACING_LINE to lineSpacing.toString(),
        KEY_FONT_SPACING_CHAR to charSpacing.toString(),
        KEY_FONT_SPACING_MESSAGE to messageSpacing.toString(),
        KEY_FONT_TYPE to fontType.name,
    )

    public companion object {
        // --- property key constants (inlined from FontificatorProperties) ---
        public const val KEY_FONT_FILE_BORDER: String = "fontBorderFile"
        public const val KEY_FONT_FILE_FONT: String = "fontFile"
        public const val KEY_FONT_TYPE: String = "fontType"
        public const val KEY_FONT_GRID_WIDTH: String = "fontGridWidth"
        public const val KEY_FONT_GRID_HEIGHT: String = "fontGridHeight"
        public const val KEY_FONT_SCALE: String = "fontScale"
        public const val KEY_FONT_BORDER_SCALE: String = "fontBorderScale"
        public const val KEY_FONT_BORDER_INSET_X: String = "fontBorderInsetX"
        public const val KEY_FONT_BORDER_INSET_Y: String = "fontBorderInsetY"
        public const val KEY_FONT_SPACE_WIDTH: String = "fontSpaceWidth"
        public const val KEY_FONT_BASELINE_OFFSET: String = "fontBaselineOffset"
        public const val KEY_FONT_UNKNOWN_CHAR: String = "fontUnknownChar"
        public const val KEY_FONT_EXTENDED_CHAR: String = "fontExtendedChar"
        public const val KEY_FONT_CHARACTERS: String = "fontCharacters"
        public const val KEY_FONT_SPACING_LINE: String = "fontLineSpacing"
        public const val KEY_FONT_SPACING_CHAR: String = "fontCharSpacing"
        public const val KEY_FONT_SPACING_MESSAGE: String = "fontMessageSpacing"

        // --- validation limits (mirroring Java ConfigFont statics) ---
        public const val FONT_BORDER_SCALE_GRANULARITY: Float = 0.25f
        public const val MIN_FONT_SCALE: Float = 0.25f
        public const val MAX_FONT_SCALE: Int = (8 / FONT_BORDER_SCALE_GRANULARITY).toInt()
        public const val MIN_BORDER_SCALE: Int = 0
        public const val MAX_BORDER_SCALE: Int = (8 / FONT_BORDER_SCALE_GRANULARITY).toInt()
        public const val MIN_BORDER_INSET: Int = -256
        public const val MAX_BORDER_INSET: Int = 256
        public const val MIN_SPACE_WIDTH: Int = 0
        public const val MAX_SPACE_WIDTH: Int = 250
        public const val MIN_BASELINE_OFFSET: Int = -32
        public const val MAX_BASELINE_OFFSET: Int = 64
        public const val MIN_LINE_SPACING: Int = -16
        public const val MAX_LINE_SPACING: Int = 32
        public const val MIN_CHAR_SPACING: Int = -16
        public const val MAX_CHAR_SPACING: Int = 32
        public const val MIN_MESSAGE_SPACING: Int = 0
        public const val MAX_MESSAGE_SPACING: Int = 128

        /**
         * Deserialize a `ConfigFont` from a flat property map.
         *
         * Every key listed in [toProperties] must be present.
         * Throws [IllegalArgumentException] on missing or unparseable values.
         */
        public fun fromProperties(props: Map<String, String>): ConfigFont {
            fun require(key: String): String =
                props[key] ?: throw IllegalArgumentException("Missing required key: $key")

            return ConfigFont(
                fontFilename = require(KEY_FONT_FILE_FONT),
                borderFilename = require(KEY_FONT_FILE_BORDER),
                gridWidth = require(KEY_FONT_GRID_WIDTH).toInt(),
                gridHeight = require(KEY_FONT_GRID_HEIGHT).toInt(),
                fontScale = require(KEY_FONT_SCALE).toFloat(),
                borderScale = require(KEY_FONT_BORDER_SCALE).toFloat(),
                borderInsetX = require(KEY_FONT_BORDER_INSET_X).toInt(),
                borderInsetY = require(KEY_FONT_BORDER_INSET_Y).toInt(),
                spaceWidth = require(KEY_FONT_SPACE_WIDTH).toInt(),
                baselineOffset = require(KEY_FONT_BASELINE_OFFSET).toInt(),
                characterKey = require(KEY_FONT_CHARACTERS),
                unknownChar = require(KEY_FONT_UNKNOWN_CHAR).let { s ->
                    require(s.length == 1) { "Unknown char value must be a single character, got: \"$s\"" }
                    s[0]
                },
                extendedCharEnabled = require(KEY_FONT_EXTENDED_CHAR).toBooleanStrict(),
                lineSpacing = require(KEY_FONT_SPACING_LINE).toInt(),
                charSpacing = require(KEY_FONT_SPACING_CHAR).toInt(),
                messageSpacing = require(KEY_FONT_SPACING_MESSAGE).toInt(),
                fontType = FontType.valueOf(require(KEY_FONT_TYPE)),
            )
        }
    }
}
