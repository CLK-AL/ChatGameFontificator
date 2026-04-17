package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin immutable port of
 * `com.glitchcog.fontificator.config.ConfigCensor`.
 *
 * Mirrors the pattern established by [ConfigFont] / [ConfigMessage]:
 * the frozen Java class uses mutable fields backed by a `Properties`
 * object; this commonMain port replaces that with an immutable data
 * class plus explicit `toProperties()` / `fromProperties()` for
 * round-trip serialization, and a `validate(): List<String>` mirroring
 * the Java `validateStrings()` chain.
 *
 * ### Arrays as Lists
 * The Java class uses `String[]` for whitelist / blacklist /
 * banned-word list.  This port uses `List<String>` for immutability.
 * Empty lists are valid.
 */
public data class ConfigCensor(
    val censorshipEnabled: Boolean,
    val purgeOnTwitchBan: Boolean,
    val censorAllUrls: Boolean,
    val censorFirstUrls: Boolean,
    val censorUnknownChars: Boolean,
    val unknownCharsPercent: Int,
    val userWhitelist: List<String>,
    val userBlacklist: List<String>,
    val bannedWords: List<String>,
) {

    /**
     * Validate this configuration, replicating the Java
     * `ConfigCensor.validateStrings()` logic.
     *
     * Returns a list of human-readable error strings.  An empty list
     * means the configuration is valid.
     *
     * Note: the Java `ConfigCensor.load()` does NOT call
     * `baseValidation` on the full CENSOR_KEYS array.  It only
     * validates booleans and the `unknownCharsPercent` range.
     * The whitelist/blacklist/banned keys may be empty or missing.
     * We run `baseValidation` only on the required (non-list) keys.
     */
    public fun validate(): List<String> {
        val errors = mutableListOf<String>()

        errors += baseValidation(toProperties(), CENSOR_REQUIRED_KEYS)

        // unknownCharsPercent range check
        if (unknownCharsPercent < MIN_UNKNOWN_CHAR_PCT || unknownCharsPercent > MAX_UNKNOWN_CHAR_PCT) {
            errors += "Value \"$unknownCharsPercent\" for key \"$KEY_CENSOR_UNKNOWN_CHARS_PERCENT\" must be at least $MIN_UNKNOWN_CHAR_PCT and at most $MAX_UNKNOWN_CHAR_PCT"
        }

        return errors
    }

    /**
     * Serialize every field to a flat `Map<String, String>`.
     */
    public fun toProperties(): Map<String, String> = mapOf(
        KEY_CENSOR_ENABLED to censorshipEnabled.toString(),
        KEY_CENSOR_PURGE_ON_TWITCH_BAN to purgeOnTwitchBan.toString(),
        KEY_CENSOR_URL to censorAllUrls.toString(),
        KEY_CENSOR_FIRST_URL to censorFirstUrls.toString(),
        KEY_CENSOR_UNKNOWN_CHARS to censorUnknownChars.toString(),
        KEY_CENSOR_UNKNOWN_CHARS_PERCENT to unknownCharsPercent.toString(),
        KEY_CENSOR_WHITE to userWhitelist.joinToString(","),
        KEY_CENSOR_BLACK to userBlacklist.joinToString(","),
        KEY_CENSOR_BANNED to bannedWords.joinToString(","),
    )

    public companion object {
        // --- property key constants (from FontificatorProperties) ---
        public const val KEY_CENSOR_ENABLED: String = "censorEnabled"
        public const val KEY_CENSOR_PURGE_ON_TWITCH_BAN: String = "censorPurgeOnTwitchBan"
        public const val KEY_CENSOR_URL: String = "censorUrl"
        public const val KEY_CENSOR_FIRST_URL: String = "censorFirstUrl"
        public const val KEY_CENSOR_UNKNOWN_CHARS: String = "censorUnknownChars"
        public const val KEY_CENSOR_UNKNOWN_CHARS_PERCENT: String = "censorUnknownCharsPercent"
        public const val KEY_CENSOR_WHITE: String = "censorWhitelist"
        public const val KEY_CENSOR_BLACK: String = "censorBlacklist"
        public const val KEY_CENSOR_BANNED: String = "censorBannedWords"

        /**
         * Ordered list of every censor-config key, mirroring
         * `FontificatorProperties.CENSOR_KEYS`.
         */
        public val CENSOR_KEYS: List<String> = listOf(
            KEY_CENSOR_ENABLED,
            KEY_CENSOR_PURGE_ON_TWITCH_BAN,
            KEY_CENSOR_URL,
            KEY_CENSOR_FIRST_URL,
            KEY_CENSOR_UNKNOWN_CHARS,
            KEY_CENSOR_UNKNOWN_CHARS_PERCENT,
            KEY_CENSOR_WHITE,
            KEY_CENSOR_BLACK,
            KEY_CENSOR_BANNED,
        )

        /**
         * Required keys only (excludes whitelist/blacklist/banned which
         * are optional and may be empty).  Used by [validate] for
         * presence checking via [baseValidation], matching the Java
         * `ConfigCensor.load()` behavior which does not call
         * `baseValidation` and only validates booleans + percent.
         */
        public val CENSOR_REQUIRED_KEYS: List<String> = listOf(
            KEY_CENSOR_ENABLED,
            KEY_CENSOR_PURGE_ON_TWITCH_BAN,
            KEY_CENSOR_URL,
            KEY_CENSOR_FIRST_URL,
            KEY_CENSOR_UNKNOWN_CHARS,
            KEY_CENSOR_UNKNOWN_CHARS_PERCENT,
        )

        // --- validation limits ---
        public const val MIN_UNKNOWN_CHAR_PCT: Int = 0
        public const val MAX_UNKNOWN_CHAR_PCT: Int = 100

        /**
         * Sensible defaults matching `FontificatorProperties.loadDefaultValues()`
         * for the censor section.
         */
        public fun defaults(): ConfigCensor = ConfigCensor(
            censorshipEnabled = true,
            purgeOnTwitchBan = true,
            censorAllUrls = false,
            censorFirstUrls = false,
            censorUnknownChars = false,
            unknownCharsPercent = 20,
            userWhitelist = emptyList(),
            userBlacklist = emptyList(),
            bannedWords = emptyList(),
        )

        /**
         * Deserialize a `ConfigCensor` from a flat property map.
         *
         * Throws [IllegalArgumentException] on missing or unparseable values.
         *
         * Note: whitelist / blacklist / banned-words keys are optional
         * (defaulting to empty), matching the Java loader's
         * `props.containsKey(...)` fallback to `""`.
         */
        public fun fromProperties(props: Map<String, String>): ConfigCensor {
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

            fun parseList(key: String): List<String> {
                val raw = props[key] ?: ""
                return if (raw.isEmpty()) emptyList() else raw.split(",")
            }

            return ConfigCensor(
                censorshipEnabled = parseBool(KEY_CENSOR_ENABLED),
                purgeOnTwitchBan = parseBool(KEY_CENSOR_PURGE_ON_TWITCH_BAN),
                censorAllUrls = parseBool(KEY_CENSOR_URL),
                censorFirstUrls = parseBool(KEY_CENSOR_FIRST_URL),
                censorUnknownChars = parseBool(KEY_CENSOR_UNKNOWN_CHARS),
                unknownCharsPercent = require(KEY_CENSOR_UNKNOWN_CHARS_PERCENT).toInt(),
                userWhitelist = parseList(KEY_CENSOR_WHITE),
                userBlacklist = parseList(KEY_CENSOR_BLACK),
                bannedWords = parseList(KEY_CENSOR_BANNED),
            )
        }
    }
}
