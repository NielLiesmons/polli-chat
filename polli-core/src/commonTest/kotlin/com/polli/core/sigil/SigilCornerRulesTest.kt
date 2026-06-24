package com.polli.core.sigil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SigilCornerRulesTest {
  @Test
  fun onCell_fillsWedgeWhenEitherNeighborOn() {
    assertTrue(SigilCornerRules.fillCornerWedgeOn(true, neighborA = true, neighborB = false))
    assertTrue(SigilCornerRules.fillCornerWedgeOn(true, neighborA = false, neighborB = true))
    assertTrue(SigilCornerRules.fillCornerWedgeOn(true, neighborA = true, neighborB = true))
  }

  @Test
  fun onCell_skipsWedgeWhenBothNeighborsOff() {
    assertFalse(SigilCornerRules.fillCornerWedgeOn(true, neighborA = false, neighborB = false))
  }

  @Test
  fun offCell_fillsNookOnlyWhenBothNeighborsOn() {
    assertTrue(SigilCornerRules.fillCornerWedgeOff(false, neighborA = true, neighborB = true))
    assertFalse(SigilCornerRules.fillCornerWedgeOff(false, neighborA = true, neighborB = false))
    assertFalse(SigilCornerRules.fillCornerWedgeOff(false, neighborA = false, neighborB = false))
  }

  @Test
  fun verticalBarCell_fillsAllFourCorners() {
    // N and S on, W and E off — every corner has at least one ON neighbor.
    assertTrue(SigilCornerRules.fillCornerWedgeOn(true, true, false)) // TL: n
    assertTrue(SigilCornerRules.fillCornerWedgeOn(true, true, false)) // TR: n
    assertTrue(SigilCornerRules.fillCornerWedgeOn(true, false, true)) // BL: s — swapped args
    assertTrue(SigilCornerRules.fillCornerWedgeOn(true, false, true)) // BR: s
  }
}
