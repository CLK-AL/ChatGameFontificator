package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin port of [com.glitchcog.fontificator.config.Config.baseValidation].
 *
 * The generic whitespace-aware config validator used across all `Config`
 * subclasses.  For every key in [keys] it checks the value in [props]:
 *
 *  - Key missing entirely -> error.
 *  - Value is empty (`""`) -> error (always).
 *  - Value is blank after [String.trim] **but** the key is **not** in
 *    [spaceAllowedKeys] -> error.
 *  - Value is blank after trim **and** the key **is** in
 *    [spaceAllowedKeys] **and** the raw value is non-empty (e.g. a
 *    single space `" "`) -> **accepted** (no error).
 *
 * ### Carried fix
 * - **83155b4**: Before this commit, `trim()` was applied
 *   unconditionally and a single-space value for `fontUnknownChar` or
 *   `messageContentBreak` would be wrongly rejected.  This port
 *   preserves the corrected behaviour.
 *
 * @param props           key-value map (typically loaded from a `.cgf` file).
 * @param keys            ordered list of keys to validate.
 * @param spaceAllowedKeys set of keys for which a single-space value is
 *                          acceptable (canonically `fontUnknownChar` and
 *                          `messageContentBreak`).
 * @return list of human-readable error strings; empty when valid.
 */
public fun baseValidation(
    props: Map<String, String>,
    keys: List<String>,
    spaceAllowedKeys: Set<String> = emptySet(),
): List<String> {
    val errors = mutableListOf<String>()
    for (key in keys) {
        if (!props.containsKey(key)) {
            errors.add("Key $key missing in the configuration")
        } else {
            val value = props[key]
            if (value == null || value.trim().isEmpty()) {
                // The unknown char / content break may be a space character,
                // which trims to empty, yet is valid — but only when the raw
                // value is genuinely non-empty.
                if (key !in spaceAllowedKeys || value == null || value.isEmpty()) {
                    errors.add("Value for key $key missing in the configuration")
                }
            }
        }
    }
    return errors
}
