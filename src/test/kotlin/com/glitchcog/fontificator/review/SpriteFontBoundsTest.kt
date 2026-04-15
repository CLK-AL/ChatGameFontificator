package com.glitchcog.fontificator.review

import com.glitchcog.fontificator.config.ConfigFont
import com.glitchcog.fontificator.config.FontType
import com.glitchcog.fontificator.config.FontificatorProperties
import com.glitchcog.fontificator.config.loadreport.LoadConfigReport
import com.glitchcog.fontificator.sprite.SpriteFont
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.Properties
import javax.imageio.ImageIO
import kotlin.test.assertNotNull

/**
 * C3: `SpriteFont.getCharacterBounds` must never return null / dereference null.
 *
 * The original code at `SpriteFont.java:322-328` pulls `characterBounds.get(' ')`
 * in the VARIABLE_WIDTH branch and immediately writes `.width = ...` on the
 * result. If the space character was never populated in the map (for example:
 * characterKey doesn't contain `' '`, or grid_width * grid_height < key.length,
 * or the method is invoked before `calculateCharacterDimensions`), this is a
 * plain NullPointerException; callers at `:157`, `:164`, `:617` also read
 * `.width` on the returned value without guarding. The chosen fix is option
 * (a) from the migration plan: guarantee the returned bounds are never null
 * by falling back to a safe default when the character key is absent.
 */
class SpriteFontBoundsTest {

    @Test
    fun unknown_codepoint_returns_fallback_not_NPE() {
        val config = makeVariableWidthConfigWithoutSpace()
        val font = SpriteFont(config)

        // Intentionally do NOT call updateForConfigChange() — simulates the
        // window between construction and the first layout pass where
        // characterBounds is empty and getCharacterBounds(' ') used to NPE.
        val bounds = font.getCharacterBounds(' '.code)

        assertNotNull(bounds, "getCharacterBounds(' ') must not return null for a VARIABLE_WIDTH font")
        // Mirrors callers at SpriteFont:157, :164, :617 — `.width` must be safe.
        @Suppress("UNUSED_VARIABLE")
        val width = bounds.width
    }

    @Test
    fun space_lookup_on_key_containing_space_does_not_NPE() {
        // When the character key explicitly contains ' ', the VARIABLE_WIDTH
        // branch at SpriteFont.java:322-326 attempts `spaceBounds.width = ...`
        // without a null guard. If bounds haven't been populated yet this is
        // a direct NullPointerException.
        val config = makeVariableWidthConfigWithSpaceButNoCalc()
        val font = SpriteFont(config)

        val bounds = font.getCharacterBounds(' '.code)
        assertNotNull(bounds, "space bounds must be non-null even before calculateCharacterDimensions")
    }

    private fun makeVariableWidthConfigWithSpaceButNoCalc(): ConfigFont {
        val tempDir = Files.createTempDirectory("cgf-c3-test2").toFile().apply { deleteOnExit() }
        val pngFile = File(tempDir, "blank2.png")
        // 2x1 transparent image — matches gridWidth=2 gridHeight=1 below
        val img = BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB)
        ImageIO.write(img, "PNG", pngFile)
        pngFile.deleteOnExit()

        val config = ConfigFont()
        val props = Properties().apply {
            setProperty(FontificatorProperties.KEY_FONT_FILE_BORDER, pngFile.absolutePath)
            setProperty(FontificatorProperties.KEY_FONT_FILE_FONT, pngFile.absolutePath)
            setProperty(FontificatorProperties.KEY_FONT_TYPE, FontType.VARIABLE_WIDTH.name)
            setProperty(FontificatorProperties.KEY_FONT_GRID_WIDTH, "2")
            setProperty(FontificatorProperties.KEY_FONT_GRID_HEIGHT, "1")
            setProperty(FontificatorProperties.KEY_FONT_SCALE, "1.0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_SCALE, "1.0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_X, "0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_Y, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACE_WIDTH, "100")
            setProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET, "0")
            setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, "A")
            setProperty(FontificatorProperties.KEY_FONT_EXTENDED_CHAR, "false")
            // characterKey contains ' ' — triggers the VARIABLE_WIDTH+c==' ' branch.
            setProperty(FontificatorProperties.KEY_FONT_CHARACTERS, "A ")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_LINE, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_CHAR, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_MESSAGE, "0")
        }
        config.load(props, LoadConfigReport())
        return config
    }

    /**
     * Build a VARIABLE_WIDTH ConfigFont that points at an in-memory PNG
     * (written to a temp file — no asset added to the repo). The sprite is
     * a 1x1 fully-transparent image so the variable-width calculation has
     * no opaque pixels anywhere, and characterKey is only "A" — there is
     * no space glyph in the key.
     */
    private fun makeVariableWidthConfigWithoutSpace(): ConfigFont {
        val tempDir = Files.createTempDirectory("cgf-c3-test").toFile().apply { deleteOnExit() }
        val pngFile = File(tempDir, "blank.png")
        val img = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        // fully-transparent pixel by default
        ImageIO.write(img, "PNG", pngFile)
        pngFile.deleteOnExit()

        val config = ConfigFont()
        val props = Properties().apply {
            setProperty(FontificatorProperties.KEY_FONT_FILE_BORDER, pngFile.absolutePath)
            setProperty(FontificatorProperties.KEY_FONT_FILE_FONT, pngFile.absolutePath)
            setProperty(FontificatorProperties.KEY_FONT_TYPE, FontType.VARIABLE_WIDTH.name)
            setProperty(FontificatorProperties.KEY_FONT_GRID_WIDTH, "1")
            setProperty(FontificatorProperties.KEY_FONT_GRID_HEIGHT, "1")
            setProperty(FontificatorProperties.KEY_FONT_SCALE, "1.0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_SCALE, "1.0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_X, "0")
            setProperty(FontificatorProperties.KEY_FONT_BORDER_INSET_Y, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACE_WIDTH, "100")
            setProperty(FontificatorProperties.KEY_FONT_BASELINE_OFFSET, "0")
            setProperty(FontificatorProperties.KEY_FONT_UNKNOWN_CHAR, "A")
            setProperty(FontificatorProperties.KEY_FONT_EXTENDED_CHAR, "false")
            // characterKey intentionally does NOT contain ' ' — the bug site.
            setProperty(FontificatorProperties.KEY_FONT_CHARACTERS, "A")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_LINE, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_CHAR, "0")
            setProperty(FontificatorProperties.KEY_FONT_SPACING_MESSAGE, "0")
        }
        config.load(props, LoadConfigReport())
        return config
    }
}
