package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin port of
 * `com.glitchcog.fontificator.config.FontType`.
 *
 * Mirrors the two font type variants from the frozen Java enum.
 */
public enum class FontType(public val label: String) {
    FIXED_WIDTH("Fixed-width"),
    VARIABLE_WIDTH("Variable-width");

    /**
     * Check whether the given [name] matches any variant's `name()`.
     */
    public companion object {
        public fun contains(name: String): Boolean =
            entries.any { it.name == name }
    }

    override fun toString(): String = label
}
