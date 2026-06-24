package com.polli.core.sigil

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MnsSigilTest {
    @Test
    fun topRowMirrorsBits012() {
        val v = 0b111uL
        val g = MnsSigil.grid(v)
        assertTrue(g[0][2])
        assertTrue(g[0][6])
        assertTrue(g[0][3])
        assertTrue(g[0][5])
        assertTrue(g[0][4])
        assertFalse(g[0][0])
        assertFalse(g[0][8])
    }

    @Test
    fun bottomRowMirrorsBits3839() {
        val v = (1uL shl 38) or (1uL shl 39)
        val g = MnsSigil.grid(v)
        assertTrue(g[8][3])
        assertTrue(g[8][5])
        assertTrue(g[8][4])
        assertFalse(g[8][0])
    }

    @Test
    fun middleRowMirrorsHorizontally() {
        val v = 1uL shl 3 // first bit of row 1, col 0
        val g = MnsSigil.grid(v)
        assertTrue(g[1][0])
        assertTrue(g[1][8])
        assertFalse(g[1][1])
    }

    @Test
    fun randomValueStaysInRange() {
        repeat(20) { i ->
            val v = MnsSigil.randomValue(i.toULong())
            assertTrue(v <= MnsSigil.MAX_VALUE)
        }
    }

    @Test
    fun gridIsAlwaysHorizontallyMirrored() {
        val samples = listOf(
            0uL,
            0b111uL,
            (1uL shl 3),
            0x0A1B2C3D4EuL,
            MnsSigil.MAX_VALUE,
        )
        for (v in samples) {
            assertTrue("value $v should mirror", MnsSigil.isHorizontallyMirrored(v))
        }
        repeat(50) { i ->
            assertTrue(MnsSigil.isHorizontallyMirrored(MnsSigil.randomValue(i.toULong())))
        }
    }

    @Test
    fun goldenZeroIsAllOff() {
        val g = MnsSigil.grid(0uL)
        for (r in 0 until MnsSigil.ROWS) {
            for (c in 0 until MnsSigil.COLS) {
                assertFalse(g[r][c])
            }
        }
    }

    @Test
    fun goldenAllBitsOn_middleRowsFullyLit() {
        val g = MnsSigil.grid(MnsSigil.MAX_VALUE)
        for (r in 1 until 8) {
            for (c in 0 until MnsSigil.COLS) {
                assertTrue("row $r col $c", g[r][c])
            }
        }
        assertTrue(g[0][4])
        assertTrue(g[8][4])
    }

    @Test
    fun goldenSingleMiddleBitMirrorsToBothSides() {
        val g = MnsSigil.grid(1uL shl 3)
        assertTrue(g[1][0])
        assertTrue(g[1][8])
        for (c in 1 until 8) {
            assertFalse(g[1][c])
        }
    }

    /** Middle row fully ON — flat horizontal bar for visual sanity. */
    @Test
    fun horizontalBarPattern() {
        var bits = 0uL
        for (row in 1 until 8) {
            for (col in 0 until 5) {
                val idx = 3 + (row - 1) * 5 + col
                bits = bits or (1uL shl idx)
            }
        }
        val g = MnsSigil.grid(bits)
        for (c in 0 until MnsSigil.COLS) {
            assertTrue(g[4][c])
        }
        assertFalse(g[0][4])
        assertFalse(g[8][4])
    }

    /** L-shape: middle row + left column — OFF cell at (5,1) has N+W ON neighbors. */
    @Test
    fun lShapePattern_hasOffNookCell() {
        var bits = 0uL
        for (col in 0 until 5) {
            bits = bits or (1uL shl (3 + (4 - 1) * 5 + col))
        }
        for (row in 5 until 8) {
            bits = bits or (1uL shl (3 + (row - 1) * 5 + 0))
        }
        val g = MnsSigil.grid(bits)
        assertTrue(g[4][1])
        assertTrue(g[5][0])
        assertFalse(g[5][1])
        assertTrue(MnsSigil.isHorizontallyMirrored(bits))
    }
}
