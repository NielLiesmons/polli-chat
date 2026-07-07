package com.polli.domain.model.space

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SigilTest {
    @Test
    fun deriveFromIsDeterministic() {
        assertEquals(
            SigilId.deriveFrom("alice@chatmail.example"),
            SigilId.deriveFrom("alice@chatmail.example"),
        )
    }

    @Test
    fun deriveFromNormalizesCaseAndWhitespace() {
        assertEquals(
            SigilId.deriveFrom("alice@chatmail.example"),
            SigilId.deriveFrom("  Alice@Chatmail.Example  "),
        )
    }

    @Test
    fun deriveFromStaysWithin40Bits() {
        for (seed in listOf("a", "bob@x", "npub1xyz", "dozmarbin-wansamlit", "", "🌱space")) {
            val id = SigilId.deriveFrom(seed)
            assertTrue(id.value in 0..SigilId.MAX, "out of range for '$seed': ${id.value}")
        }
    }

    @Test
    fun distinctInputsUsuallyDifferentIds() {
        val ids = List(200) { SigilId.deriveFrom("member-$it@spaces.example") }.toSet()
        // No collisions expected across a small deterministic sample.
        assertEquals(200, ids.size)
    }

    @Test
    fun glyphIsNineByNine() {
        val sigil = Sigil.from("team@spaces.example")
        assertEquals(Sigil.SIZE, sigil.cells.size)
        sigil.cells.forEach { assertEquals(Sigil.SIZE, it.size) }
    }

    @Test
    fun glyphIsHorizontallySymmetric() {
        val sigil = Sigil.from(SigilId.deriveFrom("symmetry-check"))
        for (row in 0 until Sigil.SIZE) {
            for (col in 0 until Sigil.SIZE) {
                assertEquals(
                    sigil.cells[row][col],
                    sigil.cells[row][Sigil.SIZE - 1 - col],
                    "asymmetry at ($row,$col)",
                )
            }
        }
    }

    @Test
    fun glyphIsDeterministicForSameId() {
        val id = SigilId.deriveFrom("stable@spaces.example")
        assertEquals(Sigil.from(id), Sigil.from(id))
    }

    @Test
    fun hueInRange() {
        repeat(50) { i ->
            val hue = Sigil.from("hue-$i").hue
            assertTrue(hue in 0..359, "hue out of range: $hue")
        }
    }

    @Test
    fun differentIdentitiesProduceDifferentGlyphs() {
        assertNotEquals(Sigil.from("alice@x"), Sigil.from("bob@x"))
    }

    @Test
    fun glyphIsNotEmptyOrFull() {
        // A well-distributed stream should not yield an all-on / all-off glyph for a typical id.
        val cells = Sigil.from("design-team@spaces.example").cells.flatten()
        assertTrue(cells.any { it })
        assertFalse(cells.all { it })
    }
}
