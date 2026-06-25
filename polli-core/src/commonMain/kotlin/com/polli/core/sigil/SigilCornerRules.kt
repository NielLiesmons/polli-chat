package com.polli.core.sigil

/**
 * Corner-wedge fill rules for the circle + leftover-corner sigil renderer.
 */
object SigilCornerRules {
  enum class Corner { TL, TR, BR, BL }

  /** Five cells around a corner vertex (cardinals + corner diagonal + two wing diagonals). */
  data class CornerNeighbors(
    val cardinalA: Boolean,
    val cardinalB: Boolean,
    val diagonal: Boolean,
    val wingA: Boolean,
    val wingB: Boolean,
  ) {
    val allOff: Boolean = !cardinalA && !cardinalB && !diagonal && !wingA && !wingB
  }

  fun cornerNeighbors(
    corner: Corner,
    n: Boolean,
    e: Boolean,
    s: Boolean,
    w: Boolean,
    nw: Boolean,
    ne: Boolean,
    sw: Boolean,
    se: Boolean,
  ): CornerNeighbors = when (corner) {
    Corner.TL -> CornerNeighbors(n, w, nw, ne, sw)
    Corner.TR -> CornerNeighbors(n, e, ne, nw, se)
    Corner.BR -> CornerNeighbors(s, e, se, sw, ne)
    Corner.BL -> CornerNeighbors(s, w, sw, se, nw)
  }

  /** The two cardinals of this cell that do not touch [corner]. */
  fun innerCardinalsOn(corner: Corner, n: Boolean, e: Boolean, s: Boolean, w: Boolean): Boolean =
    when (corner) {
      Corner.TL -> s && e
      Corner.TR -> s && w
      Corner.BR -> n && w
      Corner.BL -> n && e
    }

  /**
   * ON cell: large convex quarter (radius = full cell) only when all five neighbors
   * around that corner are OFF **and** the blob continues inward (e.g. S + E for TL).
   * Standalone ON cells (no inner neighbors) stay a plain inscribed circle.
   */
  fun isConvexOuterCorner(
    selfOn: Boolean,
    corner: Corner,
    neighbors: CornerNeighbors,
    n: Boolean,
    e: Boolean,
    s: Boolean,
    w: Boolean,
  ): Boolean {
    if (!selfOn) return false
    if (!neighbors.allOff) return false
    return innerCardinalsOn(corner, n, e, s, w)
  }

  /**
   * ON cell: fill small corner wedge when any tight neighbor is ON.
   * Convex outer corners use the large quarter instead (no wedge).
   */
  fun fillCornerWedgeOn(
    selfOn: Boolean,
    neighborA: Boolean,
    neighborB: Boolean,
    diagonal: Boolean,
    corner: Corner,
    neighbors: CornerNeighbors,
    n: Boolean,
    e: Boolean,
    s: Boolean,
    w: Boolean,
  ): Boolean {
    if (!selfOn) return false
    if (isConvexOuterCorner(selfOn, corner, neighbors, n, e, s, w)) return false
    return neighborA || neighborB || diagonal
  }

  /** OFF cell: fill corner wedge only when both cardinals are ON (nook). */
  fun fillCornerWedgeOff(selfOn: Boolean, neighborA: Boolean, neighborB: Boolean): Boolean {
    if (selfOn) return false
    return neighborA && neighborB
  }

  fun fillCornerWedge(
    selfOn: Boolean,
    corner: Corner,
    neighbors: CornerNeighbors,
    n: Boolean,
    e: Boolean,
    s: Boolean,
    w: Boolean,
  ): Boolean {
    val nbr = neighbors
    return fillCornerWedgeOn(selfOn, nbr.cardinalA, nbr.cardinalB, nbr.diagonal, corner, nbr, n, e, s, w) ||
      fillCornerWedgeOff(selfOn, nbr.cardinalA, nbr.cardinalB)
  }
}
