package com.glitchcog.fontificator.core

import com.glitchcog.fontificator.config.Config as JavaConfig
import com.glitchcog.fontificator.config.ConfigFont as JavaConfigFont
import com.glitchcog.fontificator.config.FontType as JavaFontType
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import com.glitchcog.fontificator.emoji.LazyLoadEmoji
import com.glitchcog.fontificator.sprite.SpriteCharacterKey as JavaSpriteCharacterKey
import java.util.Properties

/**
 * JVM-only adapters that bridge the frozen Java
 * `com.glitchcog.fontificator.sprite.SpriteCharacterKey` into the
 * commonMain `SpriteCharacterKey` API. Used by
 * `SpriteCharacterKeyJvmParityTest` to drive the same fixtures through
 * both implementations and assert byte-exact parity.
 *
 * This file never becomes commonMain — it exists only for the
 * duration of Stage S4 so the `jvmTest` side can perform
 * differential-parity checks. Deleted once every format module has
 * reached parity and the legacy Java profile is retired.
 */
public object JavaLegacyAdapter {

    /**
     * Lift a commonMain [SpriteCharacterKey] into a frozen-Java
     * [JavaSpriteCharacterKey] by replaying the public constructor
     * that matches the commonMain instance's shape.
     */
    public fun toJava(k: SpriteCharacterKey): JavaSpriteCharacterKey =
        if (k.isChar()) {
            // Char/codepoint construction: use the int-codepoint ctor so
            // non-BMP values survive the round-trip.
            JavaSpriteCharacterKey(k.getCodepoint())
        } else {
            // Emoji/badge construction: the commonMain opaque payload
            // must already be a LazyLoadEmoji for JVM round-trips.
            val emoji = k.getEmoji() as? LazyLoadEmoji
            JavaSpriteCharacterKey(emoji, k.isBadge())
        }

    /**
     * Lower a frozen-Java [JavaSpriteCharacterKey] back into the
     * commonMain [SpriteCharacterKey] by replaying the public
     * constructor that matches the Java instance's shape.
     */
    public fun fromJava(k: JavaSpriteCharacterKey): SpriteCharacterKey =
        if (k.isChar()) {
            SpriteCharacterKey(k.codepoint)
        } else {
            SpriteCharacterKey(k.emoji, k.isBadge())
        }

    /**
     * Convert a frozen-Java `ConfigFont` into the commonMain
     * immutable `ConfigFont` by reading every field via Java getters.
     */
    public fun configFontFromJava(javaConfig: JavaConfigFont): ConfigFont =
        ConfigFont(
            fontFilename = javaConfig.fontFilename,
            borderFilename = javaConfig.borderFilename,
            gridWidth = javaConfig.gridWidth,
            gridHeight = javaConfig.gridHeight,
            fontScale = javaConfig.fontScale,
            borderScale = javaConfig.borderScale,
            borderInsetX = javaConfig.borderInsetX,
            borderInsetY = javaConfig.borderInsetY,
            spaceWidth = javaConfig.spaceWidth,
            baselineOffset = javaConfig.baselineOffset,
            characterKey = javaConfig.characterKey,
            unknownChar = javaConfig.unknownChar,
            extendedCharEnabled = javaConfig.isExtendedCharEnabled,
            lineSpacing = javaConfig.lineSpacing,
            charSpacing = javaConfig.charSpacing,
            messageSpacing = javaConfig.messageSpacing,
            fontType = FontType.valueOf(javaConfig.fontType.name),
        )

    /**
     * Call the frozen Java `Config.baseValidation(Properties, String[], LoadConfigReport)`
     * and return the collected error messages as a plain list.
     *
     * Because `baseValidation` is `protected` on the abstract `Config`,
     * we use a minimal concrete subclass ([ConfigBridge]) that exposes
     * the method.
     */
    public fun baseValidationViaJava(
        props: Map<String, String>,
        keys: List<String>,
    ): List<String> {
        val javaProps = Properties()
        for ((k, v) in props) {
            javaProps.setProperty(k, v)
        }
        val report = LoadConfigReport()
        ConfigBridge().callBaseValidation(javaProps, keys.toTypedArray(), report)
        return report.messages.toList()
    }

    /**
     * Minimal concrete [JavaConfig] used only to surface the
     * `protected baseValidation` method for parity testing.
     */
    private class ConfigBridge : JavaConfig() {
        fun callBaseValidation(props: Properties, keys: Array<String>, report: LoadConfigReport): LoadConfigReport =
            baseValidation(props, keys, report)

        override fun load(props: Properties, report: LoadConfigReport): LoadConfigReport = report
        override fun reset() {}
    }
}
