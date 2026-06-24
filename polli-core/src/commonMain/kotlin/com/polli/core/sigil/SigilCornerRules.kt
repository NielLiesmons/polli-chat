package com.polli.core.sigil

/**
 * Corner-wedge fill rules for the circle + leftover-corner sigil renderer.
 *
 * Each cell has an inscribed circle (ON = filled, OFF = empty) and four corner
 * wedges outside that circle.
 */
object SigilCornerRules {
  enum class Corner { TL, TR, BR, BL }

  fun neighborsForCorner(corner: Corner): Pair<String, String> = when (corner) {
    Corner.TL -> "n" to "w"
    Corner.TR -> "n" to "e"
    Corner.BR -> "s" to "e"
    Corner.BL -> "s" to "w"
  }

  /** ON cell: fill corner wedge unless both touching neighbors are OFF (exposed corner). */
  fun fillCornerWedgeOn(selfOn: Boolean, neighborA: Boolean, neighborB: Boolean): Boolean {
    if (!selfOn) return false
    return neighborA || neighborB
  }

  /** OFF cell: fill corner wedge only when both touching neighbors are ON (nook). */
  fun fillCornerWedgeOff(selfOn: Boolean, neighborA: Boolean, neighborB: Boolean): Boolean {
    if (selfOn) return false
    return neighborA && neighborB
  }

  fun fillCornerWedge(selfOn: Boolean, neighborA: Boolean, neighborB: Boolean): Boolean =
    fillCornerWedgeOn(selfOn, neighborA, neighborB) ||
      fillCornerWedgeOff(selfOn, neighborA, neighborB)
}
