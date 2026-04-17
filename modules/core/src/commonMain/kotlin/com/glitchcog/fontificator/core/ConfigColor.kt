package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin immutable port of
 * `com.glitchcog.fontificator.config.ConfigColor`.
 *
 * ### `java.awt.Color` replacement
 * The Java class stores colours as `java.awt.Color`.  In commonMain
 * we represent each colour as a [ColorRGBA] data class (R, G, B, A
 * components 0..255).  The packed ARGB `Int` representation used by
 * `java.awt.Color.getRGB()` is available via [ColorRGBA.toArgbInt].
 *
 * ### Hex serialization
 * The Java code stores colours as 6-digit uppercase hex strings
 * (e.g. `"FF0000"` for red) via `Config.getColorHex(Color)`.
 * [ColorRGBA.toHex] produces the same 6-char format.
 *
 * ### Palette
 * The Java `ConfigColor` carries a `List<Color>` palette serialized
 * as a comma-separated hex string.  This port uses `List<ColorRGBA>`.
 * An empty palette is valid.
 */
public data class ConfigColor(
    val bgColor: ColorRGBA,
    val fgColor: ColorRGBA,
    val borderColor: ColorRGBA,
    val highlight: ColorRGBA,
    val chromaColor: ColorRGBA,
    val palette: List<ColorRGBA>,
    val colorUsername: Boolean,
    val colorTimestamp: Boolean,
    val colorMessage: Boolean,
    val colorJoin: Boolean,
    val useTwitchColors: Boolean,
) {

    /**
     * Validate this configuration, replicating the Java
     * `ConfigColor.validateStrings()` logic plus the shared
     * `baseValidation` required-fields check.
     *
     * Returns a list of human-readable error strings.  An empty list
     * means the configuration is valid.
     */
    public fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // Run required-fields check (excludes palette key, matching
        // Java's COLOR_KEYS_WITHOUT_PALETTE for baseValidation).
        errors += baseValidation(toProperties(), COLOR_KEYS_WITHOUT_PALETTE)

        // Validate individual colour hex values
        for ((color, label) in listOf(
            bgColor to KEY_COLOR_BG,
            fgColor to KEY_COLOR_FG,
            borderColor to KEY_COLOR_BORDER,
            highlight to KEY_COLOR_HIGHLIGHT,
            chromaColor to KEY_COLOR_CHROMA_KEY,
        )) {
            if (!isValidColorComponent(color)) {
                errors += "Color value for key \"$label\" is invalid"
            }
        }

        // Validate palette entries
        for (c in palette) {
            if (!isValidColorComponent(c)) {
                errors += "Color value \"${c.toHex()}\" in palette is invalid"
            }
        }

        return errors
    }

    /**
     * Serialize every field to a flat `Map<String, String>`.
     */
    public fun toProperties(): Map<String, String> {
        val paletteStr = palette.joinToString(",") { it.toHex() }
        return mapOf(
            KEY_COLOR_BG to bgColor.toHex(),
            KEY_COLOR_FG to fgColor.toHex(),
            KEY_COLOR_BORDER to borderColor.toHex(),
            KEY_COLOR_HIGHLIGHT to highlight.toHex(),
            KEY_COLOR_CHROMA_KEY to chromaColor.toHex(),
            KEY_COLOR_PALETTE to paletteStr,
            KEY_COLOR_USERNAME to colorUsername.toString(),
            KEY_COLOR_TIMESTAMP to colorTimestamp.toString(),
            KEY_COLOR_MESSAGE to colorMessage.toString(),
            KEY_COLOR_JOIN to colorJoin.toString(),
            KEY_COLOR_TWITCH to useTwitchColors.toString(),
        )
    }

    public companion object {
        // --- property key constants (from FontificatorProperties) ---
        public const val KEY_COLOR_BG: String = "colorBackground"
        public const val KEY_COLOR_FG: String = "colorForeground"
        public const val KEY_COLOR_BORDER: String = "colorBorder"
        public const val KEY_COLOR_HIGHLIGHT: String = "colorHighlight"
        public const val KEY_COLOR_CHROMA_KEY: String = "chromaKey"
        public const val KEY_COLOR_PALETTE: String = "colorPalette"
        public const val KEY_COLOR_USERNAME: String = "colorUsername"
        public const val KEY_COLOR_TIMESTAMP: String = "colorTimestamp"
        public const val KEY_COLOR_MESSAGE: String = "colorMessage"
        public const val KEY_COLOR_JOIN: String = "colorJoin"
        public const val KEY_COLOR_TWITCH: String = "colorUseTwitch"

        /**
         * All colour keys including palette, mirroring
         * `FontificatorProperties.COLOR_KEYS`.
         */
        public val COLOR_KEYS: List<String> = listOf(
            KEY_COLOR_BG,
            KEY_COLOR_FG,
            KEY_COLOR_BORDER,
            KEY_COLOR_HIGHLIGHT,
            KEY_COLOR_CHROMA_KEY,
            KEY_COLOR_PALETTE,
            KEY_COLOR_USERNAME,
            KEY_COLOR_TIMESTAMP,
            KEY_COLOR_MESSAGE,
            KEY_COLOR_JOIN,
            KEY_COLOR_TWITCH,
        )

        /**
         * Colour keys without palette, mirroring
         * `FontificatorProperties.COLOR_KEYS_WITHOUT_PALETTE`.
         * Used by [baseValidation] because the Java loader only
         * requires these keys -- a missing palette key is handled
         * separately.
         */
        public val COLOR_KEYS_WITHOUT_PALETTE: List<String> = listOf(
            KEY_COLOR_BG,
            KEY_COLOR_FG,
            KEY_COLOR_BORDER,
            KEY_COLOR_HIGHLIGHT,
            KEY_COLOR_CHROMA_KEY,
            KEY_COLOR_USERNAME,
            KEY_COLOR_TIMESTAMP,
            KEY_COLOR_MESSAGE,
            KEY_COLOR_JOIN,
            KEY_COLOR_TWITCH,
        )

        /**
         * Sensible defaults matching `FontificatorProperties.loadDefaultValues()`
         * for the color section.
         */
        public fun defaults(): ConfigColor = ConfigColor(
            bgColor = ColorRGBA.fromHex("000000"),
            fgColor = ColorRGBA.fromHex("FFFFFF"),
            borderColor = ColorRGBA.fromHex("FFFFFF"),
            highlight = ColorRGBA.fromHex("6699FF"),
            chromaColor = ColorRGBA.fromHex("00FF00"),
            palette = listOf(
                ColorRGBA.fromHex("F7977A"),
                ColorRGBA.fromHex("FDC68A"),
                ColorRGBA.fromHex("FFF79A"),
                ColorRGBA.fromHex("A2D39C"),
                ColorRGBA.fromHex("6ECFF6"),
                ColorRGBA.fromHex("A187BE"),
                ColorRGBA.fromHex("F6989D"),
            ),
            colorUsername = true,
            colorTimestamp = false,
            colorMessage = false,
            colorJoin = false,
            useTwitchColors = false,
        )

        /**
         * Deserialize a `ConfigColor` from a flat property map.
         *
         * Throws [IllegalArgumentException] on missing or unparseable values.
         */
        public fun fromProperties(props: Map<String, String>): ConfigColor {
            fun require(key: String): String =
                props[key] ?: throw IllegalArgumentException("Missing required key: $key")

            fun parseBool(key: String): Boolean {
                val v = require(key).trim().lowercase()
                return when (v) {
                    "true", "t", "yes", "y", "1" -> true
                    "false", "f", "no", "n", "0" -> false
                    else -> throw IllegalArgumentException("Unable to parse boolean value \"${require(key)}\" for key \"$key\"")
                }
            }

            fun parseColor(key: String): ColorRGBA {
                val hex = require(key)
                return try {
                    ColorRGBA.fromHex(hex)
                } catch (e: Exception) {
                    throw IllegalArgumentException("Color value \"$hex\" for key \"$key\" is invalid")
                }
            }

            val paletteStr = props[KEY_COLOR_PALETTE] ?: ""
            val palette = if (paletteStr.trim().isEmpty()) {
                emptyList()
            } else {
                paletteStr.split(",").map { hex ->
                    try {
                        ColorRGBA.fromHex(hex.trim())
                    } catch (e: Exception) {
                        throw IllegalArgumentException("Color value \"$hex\" in palette is invalid")
                    }
                }
            }

            return ConfigColor(
                bgColor = parseColor(KEY_COLOR_BG),
                fgColor = parseColor(KEY_COLOR_FG),
                borderColor = parseColor(KEY_COLOR_BORDER),
                highlight = parseColor(KEY_COLOR_HIGHLIGHT),
                chromaColor = parseColor(KEY_COLOR_CHROMA_KEY),
                palette = palette,
                colorUsername = parseBool(KEY_COLOR_USERNAME),
                colorTimestamp = parseBool(KEY_COLOR_TIMESTAMP),
                colorMessage = parseBool(KEY_COLOR_MESSAGE),
                colorJoin = parseBool(KEY_COLOR_JOIN),
                useTwitchColors = parseBool(KEY_COLOR_TWITCH),
            )
        }

        /**
         * Check that a [ColorRGBA] has valid component values (0..255).
         */
        private fun isValidColorComponent(c: ColorRGBA): Boolean =
            c.r in 0..255 && c.g in 0..255 && c.b in 0..255 && c.a in 0..255
    }
}

/**
 * Simple platform-independent colour representation.
 *
 * Replaces `java.awt.Color` in commonMain.  Each component is 0..255.
 *
 * ### Mapping to `java.awt.Color`
 * - `new Color(r, g, b)` -> `ColorRGBA(r, g, b)`
 * - `color.getRGB()` (packed 0xAARRGGBB) -> `ColorRGBA.toArgbInt()`
 * - `new Color(Integer.parseInt(hex, 16))` -> `ColorRGBA.fromHex(hex)`
 * - `String.format("%06X", 0xFFFFFF & color.getRGB())` -> `ColorRGBA.toHex()`
 */
public data class ColorRGBA(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 255,
) {
    /**
     * Pack into a 32-bit ARGB integer, matching `java.awt.Color.getRGB()`.
     */
    public fun toArgbInt(): Int =
        (a and 0xFF shl 24) or (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)

    /**
     * 6-character uppercase hex string (RGB only, no alpha), matching
     * the Java `Config.getColorHex(Color)` format.
     */
    public fun toHex(): String {
        val rgb = (r and 0xFF shl 16) or (g and 0xFF shl 8) or (b and 0xFF)
        return rgb.toString(16).uppercase().padStart(6, '0')
    }

    public companion object {
        /**
         * Parse a 6-character hex string (e.g. `"FF0000"`) into a [ColorRGBA].
         * Alpha defaults to 255 (fully opaque), matching `new Color(Integer.parseInt(hex, 16))`.
         */
        public fun fromHex(hex: String): ColorRGBA {
            val trimmed = hex.trim()
            val value = trimmed.toInt(16)
            return ColorRGBA(
                r = (value shr 16) and 0xFF,
                g = (value shr 8) and 0xFF,
                b = value and 0xFF,
                a = 255,
            )
        }

        /**
         * Create a [ColorRGBA] from a packed ARGB integer, matching
         * `java.awt.Color(int)`.
         */
        public fun fromArgbInt(argb: Int): ColorRGBA = ColorRGBA(
            r = (argb shr 16) and 0xFF,
            g = (argb shr 8) and 0xFF,
            b = argb and 0xFF,
            a = (argb shr 24) and 0xFF,
        )
    }
}
