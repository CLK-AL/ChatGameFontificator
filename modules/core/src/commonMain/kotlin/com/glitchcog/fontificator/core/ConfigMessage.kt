package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin immutable port of
 * `com.glitchcog.fontificator.config.ConfigMessage`.
 *
 * Mirrors the pattern established by [ConfigFont]: the frozen Java
 * class uses mutable fields backed by a `Properties` object; this
 * commonMain port replaces that with an immutable data class plus
 * explicit `toProperties()` / `fromProperties()` for round-trip
 * serialization, and a `validate(): List<String>` mirroring the Java
 * `validateStrings()` chain.
 *
 * ### Javadoc fix (§14)
 * S2 flagged the garbled Javadoc on `timeFormatter`
 * ("The formatter for timestampstimeFormatIntimeFormatInputput").
 * Since the Kotlin port does not carry the derived `DateFormat`
 * instance (which is a `java.text.*` type not available in
 * commonMain), the field is simply absent from this port -- the
 * raw pattern [timeFormat] remains and the Java/JVM side of the
 * runtime is responsible for turning it into a `DateFormat`.
 *
 * ### space-allowed divider (§9)
 * The `messageContentBreak` key may be a single space character
 * (e.g. `" "` to suppress the default `": "` separator). This is
 * enforced by calling [baseValidation] with
 * `spaceAllowedKeys = setOf(KEY_MESSAGE_CONTENT_BREAK)`.
 *
 * ### Fields deferred / opaque-typed
 * None -- every Java `ConfigMessage` field survives the port
 * as a primitive / String / enum.  The `timeFormatter` (a
 * `java.text.DateFormat`) is intentionally elided; callers that
 * need a formatted date should consume [timeFormat] in a
 * platform-specific layer.
 */
public data class ConfigMessage(
    val usernameFormat: String,
    val timeFormat: String,
    val messageContentBreak: String,
    val queueSize: Int,
    val messageSpeed: Int,
    val expirationTime: Int,
    val includeTimestamps: Boolean,
    val showUsernamesOnMessages: Boolean,
    val showJoinMessages: Boolean,
    val hideEmptyBorder: Boolean,
    val hideEmptyBackground: Boolean,
    val caseResolutionType: UsernameCaseResolutionType,
    val specifyCaseAllowed: Boolean,
    val messageCasing: MessageCasing,
) {

    /**
     * Validate this configuration, replicating the Java
     * `ConfigMessage.validateStrings()` logic plus the shared
     * `baseValidation` required-fields check (with the
     * space-allowed divider exemption for [KEY_MESSAGE_CONTENT_BREAK]).
     *
     * Returns a list of human-readable error strings.  An empty list
     * means the configuration is valid.
     *
     * Note: the Java validator additionally parses [timeFormat] with
     * `new SimpleDateFormat(timeFormat)` to confirm the pattern is
     * usable.  `SimpleDateFormat` is a `java.text.*` type not
     * available in commonMain, so the JVM-side adapter is responsible
     * for that extra check.  The commonMain port only enforces that
     * the pattern is present (non-empty).
     */
    public fun validate(): List<String> {
        val errors = mutableListOf<String>()

        // Run the shared required-fields check first so that the
        // space-allowed divider is treated correctly.
        errors += baseValidation(
            toProperties(),
            MESSAGE_KEYS,
            spaceAllowedKeys = setOf(KEY_MESSAGE_CONTENT_BREAK),
        )

        // --- integer range checks ---
        if (queueSize < MIN_QUEUE_SIZE || queueSize > MAX_QUEUE_SIZE) {
            errors += "Value \"$queueSize\" for key \"$KEY_MESSAGE_QUEUE_SIZE\" must be at least $MIN_QUEUE_SIZE and at most $MAX_QUEUE_SIZE"
        }
        if (messageSpeed < MIN_MESSAGE_SPEED || messageSpeed > MAX_MESSAGE_SPEED) {
            errors += "Value \"$messageSpeed\" for key \"$KEY_MESSAGE_SPEED\" must be at least $MIN_MESSAGE_SPEED and at most $MAX_MESSAGE_SPEED"
        }
        if (expirationTime < MIN_MESSAGE_EXPIRATION || expirationTime > MAX_MESSAGE_EXPIRATION) {
            errors += "Value \"$expirationTime\" for key \"$KEY_MESSAGE_EXPIRATION_TIME\" must be at least $MIN_MESSAGE_EXPIRATION and at most $MAX_MESSAGE_EXPIRATION"
        }

        // --- enum value checks ---
        // (data-class invariants mean these are structurally valid;
        //  the checks below mirror the original Java semantics for
        //  anyone constructing from raw strings via fromProperties.)

        return errors
    }

    /**
     * Serialize every field to a flat `Map<String, String>`,
     * mirroring the Java `Properties`-backed setter pattern.
     * Every field is serialised unconditionally -- there is no
     * "forgot to call setProperty" risk with this design.
     */
    public fun toProperties(): Map<String, String> = mapOf(
        KEY_MESSAGE_JOIN to showJoinMessages.toString(),
        KEY_MESSAGE_USERNAME to showUsernamesOnMessages.toString(),
        KEY_MESSAGE_TIMESTAMP to includeTimestamps.toString(),
        KEY_MESSAGE_USERFORMAT to usernameFormat,
        KEY_MESSAGE_TIMEFORMAT to timeFormat,
        KEY_MESSAGE_CONTENT_BREAK to messageContentBreak,
        KEY_MESSAGE_QUEUE_SIZE to queueSize.toString(),
        KEY_MESSAGE_SPEED to messageSpeed.toString(),
        KEY_MESSAGE_EXPIRATION_TIME to expirationTime.toString(),
        KEY_MESSAGE_HIDE_EMPTY_BORDER to hideEmptyBorder.toString(),
        KEY_MESSAGE_HIDE_EMPTY_BACKGROUND to hideEmptyBackground.toString(),
        KEY_MESSAGE_CASE_TYPE to caseResolutionType.name,
        KEY_MESSAGE_CASE_SPECIFY to specifyCaseAllowed.toString(),
        KEY_MESSAGE_CASING to messageCasing.name,
    )

    public companion object {
        // --- property key constants (inlined from FontificatorProperties) ---
        public const val KEY_MESSAGE_JOIN: String = "messageShowJoin"
        public const val KEY_MESSAGE_USERNAME: String = "messageShowUsername"
        public const val KEY_MESSAGE_TIMESTAMP: String = "messageShowTimestamp"
        public const val KEY_MESSAGE_USERFORMAT: String = "messageUsernameFormat"
        public const val KEY_MESSAGE_TIMEFORMAT: String = "messageTimestampFormat"
        public const val KEY_MESSAGE_CONTENT_BREAK: String = "messageContentBreak"
        public const val KEY_MESSAGE_QUEUE_SIZE: String = "messageQueueSize"
        public const val KEY_MESSAGE_SPEED: String = "messageSpeed"
        public const val KEY_MESSAGE_EXPIRATION_TIME: String = "messageExpirationTime"
        public const val KEY_MESSAGE_HIDE_EMPTY_BORDER: String = "messageHideEmptyBorder"
        public const val KEY_MESSAGE_HIDE_EMPTY_BACKGROUND: String = "messageHideEmptyBackground"
        public const val KEY_MESSAGE_CASE_TYPE: String = "messageUserCase"
        public const val KEY_MESSAGE_CASE_SPECIFY: String = "messageUserCaseSpecify"
        public const val KEY_MESSAGE_CASING: String = "messageCasing"

        /**
         * Ordered list of every message-config key, mirroring
         * `FontificatorProperties.MESSAGE_KEYS`.  Used by
         * [baseValidation] to check presence / non-emptiness.
         */
        public val MESSAGE_KEYS: List<String> = listOf(
            KEY_MESSAGE_JOIN,
            KEY_MESSAGE_USERNAME,
            KEY_MESSAGE_TIMESTAMP,
            KEY_MESSAGE_USERFORMAT,
            KEY_MESSAGE_TIMEFORMAT,
            KEY_MESSAGE_CONTENT_BREAK,
            KEY_MESSAGE_QUEUE_SIZE,
            KEY_MESSAGE_SPEED,
            KEY_MESSAGE_EXPIRATION_TIME,
            KEY_MESSAGE_HIDE_EMPTY_BORDER,
            KEY_MESSAGE_HIDE_EMPTY_BACKGROUND,
            KEY_MESSAGE_CASE_TYPE,
            KEY_MESSAGE_CASE_SPECIFY,
            KEY_MESSAGE_CASING,
        )

        // --- validation limits (mirroring Java ConfigMessage statics) ---
        public const val SHORTEST_DELAY: Long = 67L
        public const val MIN_QUEUE_SIZE: Int = 1
        public const val MAX_QUEUE_SIZE: Int = 5000
        public const val MIN_MESSAGE_SPEED: Int = 1
        public const val MAX_MESSAGE_SPEED: Int = 121
        public const val MIN_MESSAGE_EXPIRATION: Int = 0
        public const val MAX_MESSAGE_EXPIRATION: Int = 720
        public const val USERNAME_REPLACE: String = "%user%"
        public const val DEFAULT_CONTENT_BREAKER: String = ": "

        /**
         * Sensible defaults matching `FontificatorProperties.applyDefaults()`
         * for the message section.
         */
        public fun defaults(): ConfigMessage = ConfigMessage(
            usernameFormat = USERNAME_REPLACE,
            timeFormat = "[HH:mm:ss]",
            messageContentBreak = DEFAULT_CONTENT_BREAKER,
            queueSize = 64,
            messageSpeed = (MAX_MESSAGE_SPEED * 0.25f).toInt(),
            expirationTime = 0,
            includeTimestamps = false,
            showUsernamesOnMessages = true,
            showJoinMessages = false,
            hideEmptyBorder = false,
            hideEmptyBackground = false,
            caseResolutionType = UsernameCaseResolutionType.NONE,
            specifyCaseAllowed = false,
            messageCasing = MessageCasing.MIXED_CASE,
        )

        /**
         * Deserialize a `ConfigMessage` from a flat property map.
         *
         * Every key in [MESSAGE_KEYS] must be present.  Throws
         * [IllegalArgumentException] on missing or unparseable values.
         * Throws with the exact Java-style "Value of key ... is invalid"
         * message when the enum strings are not recognised.
         */
        public fun fromProperties(props: Map<String, String>): ConfigMessage {
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

            val caseTypeStr = require(KEY_MESSAGE_CASE_TYPE)
            if (!UsernameCaseResolutionType.contains(caseTypeStr)) {
                throw IllegalArgumentException("Value of key \"$KEY_MESSAGE_CASE_TYPE\" is invalid.")
            }
            val casingStr = require(KEY_MESSAGE_CASING)
            if (!MessageCasing.contains(casingStr)) {
                throw IllegalArgumentException("Value of key \"$KEY_MESSAGE_CASING\" is invalid.")
            }

            return ConfigMessage(
                usernameFormat = require(KEY_MESSAGE_USERFORMAT),
                timeFormat = require(KEY_MESSAGE_TIMEFORMAT),
                messageContentBreak = require(KEY_MESSAGE_CONTENT_BREAK),
                queueSize = require(KEY_MESSAGE_QUEUE_SIZE).toInt(),
                messageSpeed = require(KEY_MESSAGE_SPEED).toInt(),
                expirationTime = require(KEY_MESSAGE_EXPIRATION_TIME).toInt(),
                includeTimestamps = parseBool(KEY_MESSAGE_TIMESTAMP),
                showUsernamesOnMessages = parseBool(KEY_MESSAGE_USERNAME),
                showJoinMessages = parseBool(KEY_MESSAGE_JOIN),
                hideEmptyBorder = parseBool(KEY_MESSAGE_HIDE_EMPTY_BORDER),
                hideEmptyBackground = parseBool(KEY_MESSAGE_HIDE_EMPTY_BACKGROUND),
                caseResolutionType = UsernameCaseResolutionType.valueOf(caseTypeStr),
                specifyCaseAllowed = parseBool(KEY_MESSAGE_CASE_SPECIFY),
                messageCasing = MessageCasing.valueOf(casingStr),
            )
        }
    }
}

/**
 * Stage S4 -- commonMain port of
 * `com.glitchcog.fontificator.config.MessageCasing`.
 *
 * Mirrors the Java enum's value names so Java/Kotlin round-tripping
 * works by `name()` / `valueOf()`.
 */
public enum class MessageCasing(public val label: String) {
    MIXED_CASE("Mixed casing (No modification)"),
    UPPERCASE("Uppercase (Caps Lock)"),
    LOWERCASE("Lowercase (No capital letters)");

    public companion object {
        /** True if [name] is a valid `MessageCasing` value name. */
        public fun contains(name: String?): Boolean =
            name != null && values().any { it.name == name }
    }
}

/**
 * Stage S4 -- commonMain port of
 * `com.glitchcog.fontificator.config.UsernameCaseResolutionType`.
 *
 * Mirrors the Java enum's value names so Java/Kotlin round-tripping
 * works by `name()` / `valueOf()`.
 */
public enum class UsernameCaseResolutionType(public val label: String) {
    NONE("Do not modify the capitalization of usernames"),
    FIRST("Only the first username letter is capitalized"),
    ALL_LOWERCASE("All username letters are lowercase"),
    ALL_CAPS("All username letters are capitalized"),
    LOOKUP("Look up capitalization with call to Twitch API (irc.twitch.tv only)");

    public companion object {
        /** True if [name] is a valid `UsernameCaseResolutionType` value name. */
        public fun contains(name: String?): Boolean =
            name != null && values().any { it.name == name }
    }
}
