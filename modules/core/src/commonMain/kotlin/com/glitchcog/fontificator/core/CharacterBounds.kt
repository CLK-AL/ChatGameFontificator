package com.glitchcog.fontificator.core

/**
 * Stage S4 -- pure-Kotlin immutable replacement for the mutable
 * `java.awt.Rectangle` the frozen Java `SpriteFont` stored in its
 * `characterBounds` map.
 *
 * The legacy code occasionally mutated the rectangle after storing it
 * (see `getCharacterBounds` which writes `spaceBounds.width = ...`
 * for `VARIABLE_WIDTH`). The commonMain tier treats bounds as
 * immutable geometry; any width adjustment (e.g. the `spaceWidth`
 * override for the space character) is the caller's responsibility
 * and is expressed by producing a fresh [CharacterBounds].
 */
public data class CharacterBounds(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)
