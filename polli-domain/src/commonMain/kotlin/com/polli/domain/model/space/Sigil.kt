package com.polli.domain.model.space

/**
 * A 40-bit identity value that backs a speakable handle + [Sigil] glyph
 * (see [docs/spaces/MNS_AND_SIGILS.md]).
 *
 * Canonical MNS identities carry their on-chain 40-bit value, resolved via Rust
 * (`polli-mns`). For **non-MNS** identities (email local-parts, npubs, ad-hoc names)
 * [deriveFrom] produces a *stable fallback* value so every identity still renders a
 * consistent glyph. This fallback path never overrides a canonical MNS sigil — it only
 * fills the gap for identities that have no on-chain record yet.
 */
@JvmInline
value class SigilId(val value: Long) {
    init {
        require(value in 0..MAX) { "SigilId must be a 40-bit value (0..2^40-1), was $value" }
    }

    companion object {
        const val BITS: Int = 40
        const val MAX: Long = (1L shl BITS) - 1L

        /**
         * Deterministic 40-bit derivation from an arbitrary identity string using FNV-1a
         * (dependency-free, stable across platforms). Input is trimmed and lower-cased so
         * `Alice@x` and `alice@x ` collapse to the same glyph.
         */
        fun deriveFrom(input: String): SigilId {
            var hash = FNV_OFFSET
            for (byte in input.trim().lowercase().encodeToByteArray()) {
                hash = hash xor (byte.toLong() and 0xFF)
                hash *= FNV_PRIME
            }
            // Fold the high 24 bits into the low 40 so entropy is not discarded by masking.
            val folded = hash xor (hash ushr BITS)
            return SigilId(folded and MAX)
        }

        private const val FNV_OFFSET: Long = -3750763034362895579L // 14695981039346656037 (unsigned)
        private const val FNV_PRIME: Long = 1099511628211L
    }
}

/**
 * A left-right symmetric [SIZE]×[SIZE] monochrome glyph plus a deterministic accent [hue],
 * derived from a [SigilId]. Rendering-agnostic: [cells] is `[row][col]` where `true` is a
 * filled cell; the platform layer paints it (e.g. accent-on-surface in Compose).
 *
 * This is the *derived fallback* rendering used for avatars and discovery. The canonical
 * MNS visualizer (9×9 mirrored from a 5×9 source, per the mlkut/mns spec) is the source of
 * truth for real MNS names; align this mapping with it when `polli-mns` integration lands.
 */
data class Sigil(
    val cells: List<List<Boolean>>,
    val hue: Int,
) {
    companion object {
        const val SIZE: Int = 9
        private const val HALF: Int = (SIZE + 1) / 2 // independent left columns (incl. center)

        fun from(input: String): Sigil = from(SigilId.deriveFrom(input))

        fun from(id: SigilId): Sigil {
            // SplitMix64 seeded by the identity — a deterministic, well-distributed bit stream
            // so glyphs for adjacent ids look unrelated.
            var state = id.value.toULong() xor GOLDEN_GAMMA
            fun next(): ULong {
                state += GOLDEN_GAMMA
                var z = state
                z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
                z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
                return z xor (z shr 31)
            }

            val grid = MutableList(SIZE) { MutableList(SIZE) { false } }
            for (row in 0 until SIZE) {
                for (col in 0 until HALF) {
                    val filled = (next() and 1uL) == 1uL
                    grid[row][col] = filled
                    grid[row][SIZE - 1 - col] = filled
                }
            }
            val hue = (next() % 360uL).toInt()
            return Sigil(cells = grid.map { it.toList() }, hue = hue)
        }

        private const val GOLDEN_GAMMA: ULong = 0x9E3779B97F4A7C15uL
    }
}
