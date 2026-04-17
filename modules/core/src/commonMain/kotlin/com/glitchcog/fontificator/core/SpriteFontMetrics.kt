package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin immutable data class holding the geometry
 * inputs needed by [SpriteFontGeometry] to lay out characters on a
 * sprite sheet.
 *
 * Derived from a [ConfigFont] via [fromConfig]. Holds only the subset
 * of configuration that affects geometry -- no rendering flags, no
 * colors, no file paths.
 */
public data class SpriteFontMetrics(
    /** Grid columns on the sprite sheet. */
    val gridWidth: Int,
    /** Grid rows on the sprite sheet. */
    val gridHeight: Int,
    /** Pixel side length of one grid cell (assumed square by caller). */
    val pixelsPerCell: Int,
    /** The ordered character key mapping grid cells to codepoints. */
    val characterKey: String,
    /** Which layout strategy the sprite sheet uses. */
    val fontType: FontType,
) {
    public companion object {
        /**
         * Build metrics from a [ConfigFont] plus the measured pixel
         * side length of one grid cell.
         *
         * `pixelsPerCell` is measured from the decoded sprite sheet:
         * it is `sheetWidth / config.gridWidth` (and likewise for
         * height). In commonMain we don't have a `BufferedImage`, so
         * the caller (UI tier, later) passes it in.
         */
        public fun fromConfig(config: ConfigFont, pixelsPerCell: Int): SpriteFontMetrics =
            SpriteFontMetrics(
                gridWidth = config.gridWidth,
                gridHeight = config.gridHeight,
                pixelsPerCell = pixelsPerCell,
                characterKey = config.characterKey,
                fontType = config.fontType,
            )
    }
}
