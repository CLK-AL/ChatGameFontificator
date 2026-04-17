package com.glitchcog.fontificator.core

import com.glitchcog.fontificator.config.Config as JavaConfig
import com.glitchcog.fontificator.config.ConfigFont as JavaConfigFont
import com.glitchcog.fontificator.config.ConfigMessage as JavaConfigMessage
import com.glitchcog.fontificator.config.FontType as JavaFontType
import com.glitchcog.fontificator.config.MessageCasing as JavaMessageCasing
import com.glitchcog.fontificator.config.UsernameCaseResolutionType as JavaUsernameCaseResolutionType
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import com.glitchcog.fontificator.emoji.LazyLoadEmoji
import com.glitchcog.fontificator.sprite.Sprite as JavaSprite
import com.glitchcog.fontificator.sprite.SpriteCache as JavaSpriteCache
import com.glitchcog.fontificator.sprite.SpriteCharacterKey as JavaSpriteCharacterKey
import com.glitchcog.fontificator.sprite.SpriteFont as JavaSpriteFont
import java.awt.Rectangle
import java.awt.image.BufferedImage
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
     * Convert a frozen-Java `ConfigMessage` into the commonMain
     * immutable [ConfigMessage] by reading every field via Java
     * getters.  Used by `ConfigMessageJvmParityTest` to drive the
     * same fixture through both implementations and assert
     * field-by-field parity.
     */
    public fun configMessageFromJava(javaMsg: JavaConfigMessage): ConfigMessage =
        ConfigMessage(
            usernameFormat = javaMsg.usernameFormat,
            timeFormat = javaMsg.timeFormat,
            messageContentBreak = javaMsg.contentBreaker,
            queueSize = javaMsg.queueSize,
            messageSpeed = javaMsg.messageSpeed,
            expirationTime = javaMsg.expirationTime,
            includeTimestamps = javaMsg.showTimestamps(),
            showUsernamesOnMessages = javaMsg.showUsernames(),
            showJoinMessages = javaMsg.showJoinMessages(),
            hideEmptyBorder = javaMsg.isHideEmptyBorder,
            hideEmptyBackground = javaMsg.isHideEmptyBackground,
            caseResolutionType = UsernameCaseResolutionType.valueOf(javaMsg.caseResolutionType.name),
            specifyCaseAllowed = javaMsg.isSpecifyCaseAllowed,
            messageCasing = MessageCasing.valueOf(javaMsg.messageCasing.name),
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

    // --- SpriteFontGeometry parity helpers (Stage S4) --------------------

    /**
     * Drive the frozen Java `SpriteFont.calculateFixedCharacterDimensions`
     * with a synthesised in-memory sprite, then project the resulting
     * `java.awt.Rectangle` map into the commonMain [CharacterBounds] map.
     *
     * @param key character key to load into the Java `ConfigFont`.
     * @param gridWidth grid columns.
     * @param gridHeight grid rows.
     * @param pixelsPerCell pixel side length of one cell.
     */
    public fun fixedBoundsViaJava(
        key: String,
        gridWidth: Int,
        gridHeight: Int,
        pixelsPerCell: Int,
    ): Map<Int, CharacterBounds> {
        val wholeWidth = gridWidth * pixelsPerCell
        val wholeHeight = gridHeight * pixelsPerCell
        // Build a zeroed ARGB sheet; fixed-grid geometry is purely
        // positional and ignores pixel alpha, so zeros are fine.
        val image = BufferedImage(wholeWidth, wholeHeight, BufferedImage.TYPE_INT_ARGB)
        return driveLegacySpriteFont(
            key = key,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            pixelsPerCell = pixelsPerCell,
            fontType = JavaFontType.FIXED_WIDTH,
            image = image,
        )
    }

    /**
     * Drive the frozen Java `SpriteFont.calculateVariableCharacterDimensions`
     * with a synthesised in-memory sprite built from a row-major ARGB
     * pixel matrix, then project the resulting `java.awt.Rectangle`
     * map into the commonMain [CharacterBounds] map.
     *
     * @param key character key to load into the Java `ConfigFont`.
     * @param gridRows number of grid rows.
     * @param pixels `pixels[y][x]` -> ARGB int at pixel (x, y).
     */
    public fun variableBoundsViaJava(
        key: String,
        gridRows: Int,
        pixels: Array<IntArray>,
    ): Map<Int, CharacterBounds> {
        val wholeHeight = pixels.size
        val wholeWidth = if (pixels.isEmpty()) 0 else pixels[0].size
        val gridCols = if (gridRows > 0) key.length / gridRows else 0
        require(gridCols > 0) { "gridCols must be > 0 (key.length=${key.length}, gridRows=$gridRows)" }
        val pixelsPerCell = wholeWidth / gridCols

        val image = BufferedImage(wholeWidth, wholeHeight, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until wholeHeight) {
            for (x in 0 until wholeWidth) {
                image.setRGB(x, y, pixels[y][x])
            }
        }
        return driveLegacySpriteFont(
            key = key,
            gridWidth = gridCols,
            gridHeight = gridRows,
            pixelsPerCell = pixelsPerCell,
            fontType = JavaFontType.VARIABLE_WIDTH,
            image = image,
        )
    }

    /**
     * Build a Java [JavaSpriteFont] whose internal [JavaSpriteCache]
     * is pre-populated with an in-memory sprite (bypassing the
     * file-loading path), then invoke `calculateCharacterDimensions()`
     * and return the internal `characterBounds` map.
     *
     * Uses reflection because:
     *  - `Sprite` has no public setter for its `BufferedImage`.
     *  - `SpriteFont.sprites` is a protected field.
     *  - `SpriteFont.characterBounds` is a protected field.
     */
    @Suppress("UNCHECKED_CAST")
    private fun driveLegacySpriteFont(
        key: String,
        gridWidth: Int,
        gridHeight: Int,
        pixelsPerCell: Int,
        fontType: JavaFontType,
        image: BufferedImage,
    ): Map<Int, CharacterBounds> {
        // 1) Build a ConfigFont with geometry-only fields populated.
        val javaConfig = buildLegacyConfigFont(
            key = key,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            fontType = fontType,
        )

        // 2) Build an empty Sprite and inject our image + dimensions.
        val sprite = JavaSprite()
        injectSpriteImage(sprite, image, gridWidth, gridHeight, pixelsPerCell, pixelsPerCell)

        // 3) Allocate a SpriteFont without running its constructor
        //    (the ctor's SpriteCache load would NPE against the null
        //    ChatWindow.popup in a headless test). Inject config,
        //    sprites, and characterBounds by hand.
        val spriteFont = allocateUninitialized(JavaSpriteFont::class.java)
        val newCache = JavaSpriteCache()
        injectSpriteIntoCache(newCache, javaConfig.fontFilename, sprite)
        JavaSpriteFont::class.java.getDeclaredField("sprites").apply { isAccessible = true }.set(spriteFont, newCache)
        JavaSpriteFont::class.java.getDeclaredField("config").apply { isAccessible = true }.set(spriteFont, javaConfig)
        JavaSpriteFont::class.java.getDeclaredField("characterBounds").apply { isAccessible = true }
            .set(spriteFont, HashMap<Int, Rectangle>())

        // 4) Invoke the public entry point -- it dispatches on fontType.
        spriteFont.calculateCharacterDimensions()

        // 5) Read the resulting characterBounds map.
        val boundsField = JavaSpriteFont::class.java.getDeclaredField("characterBounds").apply { isAccessible = true }
        val javaBounds = boundsField.get(spriteFont) as Map<Int, Rectangle>

        // 6) Project to CharacterBounds.
        return javaBounds.mapValues { (_, r) -> CharacterBounds(r.x, r.y, r.width, r.height) }
    }

    /**
     * Build a minimal Java `ConfigFont` carrying only the fields the
     * geometry methods read.
     */
    private fun buildLegacyConfigFont(
        key: String,
        gridWidth: Int,
        gridHeight: Int,
        fontType: JavaFontType,
    ): JavaConfigFont {
        val props = Properties()
        props.setProperty(FontificatorProperties.KEY_FONT_FILE_FONT, "memory://test_sprite_sheet.png")
        props.setProperty(FontificatorProperties.KEY_FONT_FILE_BORDER, "memory://test_border.png")
        props.setProperty(FontificatorProperties.KEY_FONT_GRID_WIDTH, gridWidth.toString())
        props.setProperty(FontificatorProperties.KEY_FONT_GRID_HEIGHT, gridHeight.toString())
        props.setProperty(FontificatorProperties.KEY_FONT_SCALE, "1.0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_SCALE, "1.0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_X, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_Y, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACE_WIDTH, "25")
        props.setProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_CHARACTERS, key)
        // Guarantee unknownChar is in the key (the load validator requires it).
        val unknownChar = if (key.isNotEmpty()) key[0] else ' '
        props.setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, unknownChar.toString())
        props.setProperty(FontificatorProperties.KEY_FONT_EXTENDED_CHAR, "false")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_LINE, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_CHAR, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_SPACING_MESSAGE, "0")
        props.setProperty(FontificatorProperties.KEY_FONT_TYPE, fontType.name)
        val cfg = JavaConfigFont()
        cfg.load(props, LoadConfigReport())
        return cfg
    }

    /**
     * Reflectively replace the [JavaSprite]'s `img`, grid dims, and
     * pixel dims so the sprite returns the caller's in-memory sheet.
     */
    private fun injectSpriteImage(
        sprite: JavaSprite,
        image: BufferedImage,
        gridWidth: Int,
        gridHeight: Int,
        pixelWidth: Int,
        pixelHeight: Int,
    ) {
        JavaSprite::class.java.getDeclaredField("img").apply { isAccessible = true }.set(sprite, image)
        JavaSprite::class.java.getDeclaredField("gridWidth").apply { isAccessible = true }.setInt(sprite, gridWidth)
        JavaSprite::class.java.getDeclaredField("gridHeight").apply { isAccessible = true }.setInt(sprite, gridHeight)
        JavaSprite::class.java.getDeclaredField("pixelWidth").apply { isAccessible = true }.setInt(sprite, pixelWidth)
        JavaSprite::class.java.getDeclaredField("pixelHeight").apply { isAccessible = true }.setInt(sprite, pixelHeight)
    }

    /**
     * Reflectively inject a pre-built [JavaSprite] into a
     * [JavaSpriteCache] under the given filename key, so future
     * `getSprite(config)` calls return it without touching the file
     * system. Also prime the `null` key so the default empty-sprite
     * path is harmless.
     */
    @Suppress("UNCHECKED_CAST")
    private fun injectSpriteIntoCache(
        cache: JavaSpriteCache,
        filename: String,
        sprite: JavaSprite,
    ) {
        val cacheField = JavaSpriteCache::class.java.getDeclaredField("cache").apply { isAccessible = true }
        val map = cacheField.get(cache) as MutableMap<String?, JavaSprite>
        map[filename] = sprite
    }

    /**
     * Allocate an instance of [type] without running any constructor.
     *
     * Used so the frozen Java `SpriteFont` can be populated
     * field-by-field in a headless test environment -- its real
     * constructor calls `new SpriteCache(config)` which NPEs when the
     * sprite file fails to load and the (null) `ChatWindow.popup`
     * error handler is invoked.
     *
     * Uses `sun.misc.Unsafe.allocateInstance` via reflection so we
     * don't need a direct compile-time dependency on an internal API.
     */
    private fun <T> allocateUninitialized(type: Class<T>): T {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = theUnsafeField.get(null)
        val allocate = unsafeClass.getMethod("allocateInstance", Class::class.java)
        @Suppress("UNCHECKED_CAST")
        return allocate.invoke(unsafe, type) as T
    }
}
