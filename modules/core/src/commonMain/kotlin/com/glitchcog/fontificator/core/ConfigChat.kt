package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin immutable port of
 * `com.glitchcog.fontificator.config.ConfigChat`.
 *
 * Mirrors the pattern established by [ConfigFont] / [ConfigMessage]:
 * the frozen Java class uses mutable fields backed by a `Properties`
 * object; this commonMain port replaces that with an immutable data
 * class plus explicit `toProperties()` / `fromProperties()` for
 * round-trip serialization, and a `validate(): List<String>` mirroring
 * the Java `validateStrings()` chain.
 *
 * ### `java.awt.Rectangle` replacement
 * The Java `ConfigChat` stores the chroma border as a
 * `java.awt.Rectangle(left, top, right, bottom)`.  This port
 * decomposes it into four plain `Int` fields:
 * [chromaLeft], [chromaTop], [chromaRight], [chromaBottom].
 *
 * ### Legacy window dimensions
 * The Java class carries `windowWidth` / `windowHeight` (legacy
 * window-frame size, no longer saved).  This port includes them as
 * nullable `Int?` fields to support loading older config files.
 */
public data class ConfigChat(
    val scrollable: Boolean,
    val resizable: Boolean,
    val rememberPosition: Boolean,
    val chatWindowPositionX: Int,
    val chatWindowPositionY: Int,
    val chatFromBottom: Boolean,
    val width: Int,
    val height: Int,
    val windowWidth: Int?,
    val windowHeight: Int?,
    val chromaEnabled: Boolean,
    val chromaInvert: Boolean,
    val chromaLeft: Int,
    val chromaTop: Int,
    val chromaRight: Int,
    val chromaBottom: Int,
    val chromaCornerRadius: Int,
    val reverseScrolling: Boolean,
    val alwaysOnTop: Boolean,
    val antiAlias: Boolean,
) {

    /**
     * Validate this configuration, replicating the Java
     * `ConfigChat.validateStrings()` logic plus the shared
     * `baseValidation` required-fields check.
     *
     * Returns a list of human-readable error strings.  An empty list
     * means the configuration is valid.
     */
    public fun validate(): List<String> {
        val errors = mutableListOf<String>()

        errors += baseValidation(toProperties(), CHAT_KEYS)

        // Dimension checks: width and height must be >= 1
        if (width < 1) {
            errors += "Value \"$width\" for key \"$KEY_CHAT_WIDTH\" must be at least 1"
        }
        if (height < 1) {
            errors += "Value \"$height\" for key \"$KEY_CHAT_HEIGHT\" must be at least 1"
        }

        // Chroma border values must be >= 0
        if (chromaLeft < 0) {
            errors += "Value \"$chromaLeft\" for key \"$KEY_CHAT_CHROMA_LEFT\" must be at least 0"
        }
        if (chromaTop < 0) {
            errors += "Value \"$chromaTop\" for key \"$KEY_CHAT_CHROMA_TOP\" must be at least 0"
        }
        if (chromaRight < 0) {
            errors += "Value \"$chromaRight\" for key \"$KEY_CHAT_CHROMA_RIGHT\" must be at least 0"
        }
        if (chromaBottom < 0) {
            errors += "Value \"$chromaBottom\" for key \"$KEY_CHAT_CHROMA_BOTTOM\" must be at least 0"
        }

        // Chroma corner radius must be in range
        if (chromaCornerRadius < MIN_CHROMA_CORNER_RADIUS || chromaCornerRadius > MAX_CHROMA_CORNER_RADIUS) {
            errors += "Value \"$chromaCornerRadius\" for key \"$KEY_CHAT_CHROMA_CORNER\" must be at least $MIN_CHROMA_CORNER_RADIUS and at most $MAX_CHROMA_CORNER_RADIUS"
        }

        return errors
    }

    /**
     * Serialize every field to a flat `Map<String, String>`.
     *
     * Note: [windowWidth] / [windowHeight] are legacy fields.
     * When non-null they are serialized under the legacy keys;
     * otherwise they are omitted.  The primary width/height
     * are always serialized.
     */
    public fun toProperties(): Map<String, String> {
        val map = mutableMapOf(
            KEY_CHAT_SCROLL to scrollable.toString(),
            KEY_CHAT_RESIZABLE to resizable.toString(),
            KEY_CHAT_POSITION to rememberPosition.toString(),
            KEY_CHAT_POSITION_X to chatWindowPositionX.toString(),
            KEY_CHAT_POSITION_Y to chatWindowPositionY.toString(),
            KEY_CHAT_FROM_BOTTOM to chatFromBottom.toString(),
            KEY_CHAT_WIDTH to width.toString(),
            KEY_CHAT_HEIGHT to height.toString(),
            KEY_CHAT_CHROMA_ENABLED to chromaEnabled.toString(),
            KEY_CHAT_INVERT_CHROMA to chromaInvert.toString(),
            KEY_CHAT_REVERSE_SCROLLING to reverseScrolling.toString(),
            KEY_CHAT_CHROMA_LEFT to chromaLeft.toString(),
            KEY_CHAT_CHROMA_TOP to chromaTop.toString(),
            KEY_CHAT_CHROMA_RIGHT to chromaRight.toString(),
            KEY_CHAT_CHROMA_BOTTOM to chromaBottom.toString(),
            KEY_CHAT_CHROMA_CORNER to chromaCornerRadius.toString(),
            KEY_CHAT_ALWAYS_ON_TOP to alwaysOnTop.toString(),
            KEY_CHAT_ANTIALIAS to antiAlias.toString(),
        )
        if (windowWidth != null) {
            map[KEY_CHAT_WINDOW_WIDTH] = windowWidth.toString()
        }
        if (windowHeight != null) {
            map[KEY_CHAT_WINDOW_HEIGHT] = windowHeight.toString()
        }
        return map
    }

    public companion object {
        // --- property key constants (from FontificatorProperties) ---
        public const val KEY_CHAT_SCROLL: String = "chatScrollEnabled"
        public const val KEY_CHAT_RESIZABLE: String = "chatResizable"
        public const val KEY_CHAT_POSITION: String = "chatPosition"
        public const val KEY_CHAT_POSITION_X: String = "chatPositionX"
        public const val KEY_CHAT_POSITION_Y: String = "chatPositionY"
        public const val KEY_CHAT_FROM_BOTTOM: String = "chatFromBottom"
        public const val KEY_CHAT_WINDOW_WIDTH: String = "chatWidth"   // Legacy
        public const val KEY_CHAT_WINDOW_HEIGHT: String = "chatHeight" // Legacy
        public const val KEY_CHAT_WIDTH: String = "chatPixelWidth"
        public const val KEY_CHAT_HEIGHT: String = "chatPixelHeight"
        public const val KEY_CHAT_CHROMA_ENABLED: String = "chromaEnabled"
        public const val KEY_CHAT_INVERT_CHROMA: String = "invertChroma"
        public const val KEY_CHAT_REVERSE_SCROLLING: String = "reverseScrolling"
        public const val KEY_CHAT_CHROMA_LEFT: String = "chromaLeft"
        public const val KEY_CHAT_CHROMA_TOP: String = "chromaTop"
        public const val KEY_CHAT_CHROMA_RIGHT: String = "chromaRight"
        public const val KEY_CHAT_CHROMA_BOTTOM: String = "chromaBottom"
        public const val KEY_CHAT_CHROMA_CORNER: String = "chromaCornerRadius"
        public const val KEY_CHAT_ALWAYS_ON_TOP: String = "chatAlwaysOnTop"
        public const val KEY_CHAT_ANTIALIAS: String = "chatAntialias"

        /**
         * Ordered list of every chat-config key, mirroring
         * `FontificatorProperties.CHAT_KEYS`.  Used by
         * [baseValidation] to check presence / non-emptiness.
         */
        public val CHAT_KEYS: List<String> = listOf(
            KEY_CHAT_SCROLL,
            KEY_CHAT_RESIZABLE,
            KEY_CHAT_POSITION,
            KEY_CHAT_POSITION_X,
            KEY_CHAT_POSITION_Y,
            KEY_CHAT_FROM_BOTTOM,
            KEY_CHAT_WIDTH,
            KEY_CHAT_HEIGHT,
            KEY_CHAT_CHROMA_ENABLED,
            KEY_CHAT_INVERT_CHROMA,
            KEY_CHAT_REVERSE_SCROLLING,
            KEY_CHAT_CHROMA_LEFT,
            KEY_CHAT_CHROMA_TOP,
            KEY_CHAT_CHROMA_RIGHT,
            KEY_CHAT_CHROMA_BOTTOM,
            KEY_CHAT_CHROMA_CORNER,
            KEY_CHAT_ALWAYS_ON_TOP,
            KEY_CHAT_ANTIALIAS,
        )

        // --- validation limits ---
        public const val MIN_CHROMA_CORNER_RADIUS: Int = 0
        public const val MAX_CHROMA_CORNER_RADIUS: Int = 128

        /**
         * Sensible defaults matching `FontificatorProperties.loadDefaultValues()`
         * for the chat section.
         */
        public fun defaults(): ConfigChat = ConfigChat(
            scrollable = false,
            resizable = true,
            rememberPosition = false,
            chatWindowPositionX = 0,
            chatWindowPositionY = 0,
            chatFromBottom = false,
            width = 550,
            height = 450,
            windowWidth = null,
            windowHeight = null,
            chromaEnabled = false,
            chromaInvert = false,
            chromaLeft = 10,
            chromaTop = 10,
            chromaRight = 10,
            chromaBottom = 10,
            chromaCornerRadius = 10,
            reverseScrolling = false,
            alwaysOnTop = false,
            antiAlias = false,
        )

        /**
         * Deserialize a `ConfigChat` from a flat property map.
         *
         * Every key in [CHAT_KEYS] must be present.  Throws
         * [IllegalArgumentException] on missing or unparseable values.
         *
         * Legacy keys [KEY_CHAT_WINDOW_WIDTH] / [KEY_CHAT_WINDOW_HEIGHT]
         * are optional; when present they populate [windowWidth] / [windowHeight].
         */
        public fun fromProperties(props: Map<String, String>): ConfigChat {
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

            return ConfigChat(
                scrollable = parseBool(KEY_CHAT_SCROLL),
                resizable = parseBool(KEY_CHAT_RESIZABLE),
                rememberPosition = parseBool(KEY_CHAT_POSITION),
                chatWindowPositionX = require(KEY_CHAT_POSITION_X).toInt(),
                chatWindowPositionY = require(KEY_CHAT_POSITION_Y).toInt(),
                chatFromBottom = parseBool(KEY_CHAT_FROM_BOTTOM),
                width = require(KEY_CHAT_WIDTH).toInt(),
                height = require(KEY_CHAT_HEIGHT).toInt(),
                windowWidth = props[KEY_CHAT_WINDOW_WIDTH]?.toIntOrNull(),
                windowHeight = props[KEY_CHAT_WINDOW_HEIGHT]?.toIntOrNull(),
                chromaEnabled = parseBool(KEY_CHAT_CHROMA_ENABLED),
                chromaInvert = parseBool(KEY_CHAT_INVERT_CHROMA),
                chromaLeft = require(KEY_CHAT_CHROMA_LEFT).toInt(),
                chromaTop = require(KEY_CHAT_CHROMA_TOP).toInt(),
                chromaRight = require(KEY_CHAT_CHROMA_RIGHT).toInt(),
                chromaBottom = require(KEY_CHAT_CHROMA_BOTTOM).toInt(),
                chromaCornerRadius = require(KEY_CHAT_CHROMA_CORNER).toInt(),
                reverseScrolling = parseBool(KEY_CHAT_REVERSE_SCROLLING),
                alwaysOnTop = parseBool(KEY_CHAT_ALWAYS_ON_TOP),
                antiAlias = parseBool(KEY_CHAT_ANTIALIAS),
            )
        }
    }
}
