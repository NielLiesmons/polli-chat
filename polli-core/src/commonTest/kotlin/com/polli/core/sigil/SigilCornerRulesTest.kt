package com.polli.core.sigil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SigilCornerRulesTest {
  private fun allOff() = SigilCornerRules.CornerNeighbors(
    cardinalA = false,
    cardinalB = false,
    diagonal = false,
    wingA = false,
    wingB = false,
  )

  @Test
  fun onCell_fillsWhenAnyTightNeighborOn() {
    val n = allOff().copy(cardinalA = true)
    assertTrue(
      SigilCornerRules.fillCornerWedgeOn(
        true, n.cardinalA, n.cardinalB, n.diagonal,
        SigilCornerRules.Corner.TL, n, false, false, true, false,
      ),
    )
  }

  @Test
  fun standaloneOnCell_staysRoundCircleNotConvex() {
    val n = allOff()
    assertFalse(
      SigilCornerRules.isConvexOuterCorner(
        true, SigilCornerRules.Corner.TL, n, false, false, false, false,
      ),
    )
    assertFalse(
      SigilCornerRules.fillCornerWedgeOn(
        true, n.cardinalA, n.cardinalB, n.diagonal,
        SigilCornerRules.Corner.TL, n, false, false, false, false,
      ),
    )
  }

  @Test
  fun convexOuter_whenFiveOffAndInnerCardinalsOn() {
    val n = allOff()
    assertTrue(
      SigilCornerRules.isConvexOuterCorner(
        true, SigilCornerRules.Corner.TL, n, false, true, true, false,
      ),
    )
    assertFalse(
      SigilCornerRules.fillCornerWedgeOn(
        true, n.cardinalA, n.cardinalB, n.diagonal,
        SigilCornerRules.Corner.TL, n, false, true, true, false,
      ),
    )
  }

  @Test
  fun convexOuter_falseWhenAnyWingOn() {
    val n = allOff().copy(wingA = true)
    assertFalse(
      SigilCornerRules.isConvexOuterCorner(
        true, SigilCornerRules.Corner.TL, n, false, true, true, false,
      ),
    )
  }

  @Test
  fun offCell_fillsNookOnlyWhenBothCardinalsOn() {
    assertTrue(SigilCornerRules.fillCornerWedgeOff(false, true, true))
    assertFalse(SigilCornerRules.fillCornerWedgeOff(false, true, false))
  }
}
