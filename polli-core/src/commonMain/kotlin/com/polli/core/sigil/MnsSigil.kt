package com.polli.core.sigil

/**
 * 40-bit Mlkut name-system sigil: 5×9 source grid mirrored to 9×9.
 * Bit layout matches the MNS visualizer (top/bottom rows use symmetric placement).
 */
object MnsSigil {
    const val ROWS = 9
    const val COLS = 9
    const val TOTAL_BITS = 40
    val MAX_VALUE: ULong = (1uL shl TOTAL_BITS) - 1uL

    /** Mirrored 9×9 grid; `true` = on (white). Out-of-range reads as off. */
    fun grid(value: ULong): Array<BooleanArray> {
        val bits = value and MAX_VALUE
        return Array(ROWS) { r ->
            BooleanArray(COLS) { c -> bitAt(bits, r, c) }
        }
    }

    /** Every row is mirrored: col `c` equals col `8 - c`. */
    fun isHorizontallyMirrored(value: ULong): Boolean {
        val g = grid(value)
        for (r in 0 until ROWS) {
            for (c in 0 until COLS / 2) {
                if (g[r][c] != g[r][COLS - 1 - c]) return false
            }
        }
        return true
    }

    fun bitAt(value: ULong, row: Int, col: Int): Boolean {
        val bits = value and MAX_VALUE
        val bitIdx: Int = when {
            row == 0 -> when (col) {
                2, 6 -> 0
                3, 5 -> 1
                4 -> 2
                else -> return false
            }
            row == 8 -> when (col) {
                3, 5 -> 38
                4 -> 39
                else -> return false
            }
            else -> {
                val srcCol = if (col < 5) col else (8 - col)
                3 + (row - 1) * 5 + srcCol
            }
        }
        return ((bits shr bitIdx) and 1uL) != 0uL
    }

    fun encodeName(value: ULong): String {
        val v = value and MAX_VALUE
        val mask = 1023uL
        val p1 = ((v shr 30) and mask).toInt()
        val s1 = ((v shr 20) and mask).toInt()
        val p2 = ((v shr 10) and mask).toInt()
        val s2 = (v and mask).toInt()
        return "${prefix(p1)}${suffix(s1)}-${prefix(p2)}${suffix(s2)}"
    }

    /** Inverse of [encodeName]; accepts the bare MNS name or chatmail local-part. */
    fun decodeName(name: String): ULong? {
        val normalized = localPartFromAddress(name)
        if (normalized.isEmpty()) return null
        val dash = normalized.indexOf('-')
        if (dash <= 0 || dash >= normalized.lastIndex) return null
        val left = normalized.substring(0, dash)
        val right = normalized.substring(dash + 1)
        if (left.length != TOKEN_LEN || right.length != TOKEN_LEN) return null
        val p1 = prefixIndex(left.substring(0, TOKEN_LEN / 2)) ?: return null
        val s1 = suffixIndex(left.substring(TOKEN_LEN / 2)) ?: return null
        val p2 = prefixIndex(right.substring(0, TOKEN_LEN / 2)) ?: return null
        val s2 = suffixIndex(right.substring(TOKEN_LEN / 2)) ?: return null
        return (
            (p1.toULong() shl 30) or
                (s1.toULong() shl 20) or
                (p2.toULong() shl 10) or
                s2.toULong()
            ) and MAX_VALUE
    }

    /** Local-part of a chatmail address, or the whole string when no `@` is present. */
    fun localPartFromAddress(address: String): String {
        val trimmed = address.trim().lowercase()
        val at = trimmed.indexOf('@')
        return if (at >= 0) trimmed.substring(0, at) else trimmed
    }

    /**
     * Deterministic 40-bit sigil for a chatmail address or MNS domain name.
     * MNS names decode directly; everything else hashes to a stable sigil.
     */
    fun valueFromIdentity(identity: String): ULong {
        val normalized = identity.trim().lowercase()
        if (normalized.isEmpty()) return 0uL
        decodeName(localPartFromAddress(normalized))?.let { return it }
        decodeName(normalized)?.let { return it }
        return hashToValue(normalized)
    }

    /** Human-readable MNS domain name for an address or raw identity string. */
    fun sigilName(identity: String): String = encodeName(valueFromIdentity(identity))

    fun formatHex(value: ULong): String =
        "0x${(value and MAX_VALUE).toString(16).uppercase().padStart(10, '0')}"

    /** Uniform random 40-bit identity (same scheme as the MNS HTML visualizer). */
    fun randomValue(seed: ULong = System.nanoTime().toULong()): ULong {
        var x = seed xor (seed shl 13) xor (seed shr 7) xor (seed shl 17)
        if (x == 0uL) x = 0xDEAD_BEEFuL
        var bits = 0uL
        for (i in 0 until TOTAL_BITS) {
            x = x xor (x shl 7) xor (x shr 9) xor 0x2545_F491_4F6C_DD1DuL
            if ((x and 1uL) != 0uL) bits = bits or (1uL shl i)
        }
        return bits and MAX_VALUE
    }

    private const val TOKEN_LEN = 8

    private fun hashToValue(input: String): ULong {
        var seed = 0uL
        for (ch in input) {
            seed = seed xor (ch.code.toULong() and 0xFFuL)
            seed = seed * 0x9E37_79B9_7F4A_7C15uL
            seed = seed xor (seed shr 33)
        }
        return randomValue(seed)
    }

    private fun prefix(index: Int): String =
        RAW_PREFIX[index / 4] + PREFIX_VOWELS[index % 4]

    private fun suffix(index: Int): String =
        RAW_SUFFIX[index / 4] + SUFFIX_VOWELS[index % 4]

    private fun prefixIndex(token: String): Int? = prefixByToken[token]
    private fun suffixIndex(token: String): Int? = suffixByToken[token]

    private val prefixByToken: Map<String, Int> by lazy {
        buildMap(1024) {
            for (i in 0 until 1024) put(prefix(i), i)
        }
    }

    private val suffixByToken: Map<String, Int> by lazy {
        buildMap(1024) {
            for (i in 0 until 1024) put(suffix(i), i)
        }
    }

    private val PREFIX_VOWELS = charArrayOf('i', 'a', 'o', 'u')
    private val SUFFIX_VOWELS = charArrayOf('y', 'a', 'o', 'u')

    private val RAW_PREFIX = arrayOf(
        "doz", "mar", "bin", "wan", "sam", "lit", "sig", "hid", "fid", "lis", "sog", "dir", "wak",
        "sab", "wis", "sib", "rig", "sol", "dop", "mod", "fog", "lid", "hop", "dar", "dor", "lor",
        "hod", "fol", "rin", "tog", "sil", "mir", "hol", "pas", "lak", "rov", "liv", "dal", "sat",
        "lib", "tab", "han", "tik", "pid", "tor", "bol", "fos", "dot", "los", "dil", "for", "pil",
        "ram", "tir", "win", "tad", "bik", "dif", "rok", "wid", "bis", "das", "mid", "lop", "ril",
        "nar", "dap", "mol", "san", "lok", "nov", "sit", "nid", "tip", "sik", "rop", "wit", "nat",
        "pan", "min", "rit", "pod", "mot", "tam", "tol", "sav", "pos", "nap", "nop", "som", "fin",
        "fon", "ban", "mor", "wor", "sip", "ron", "nor", "bot", "wik", "sok", "wat", "dol", "mag",
        "pik", "dav", "bid", "bal", "tim", "tas", "mal", "lig", "siv", "tag", "pad", "sal", "div",
        "dak", "tan", "sid", "fab", "tar", "mon", "ran", "nis", "wol", "mis", "pal", "las", "dis",
        "map", "rab", "tob", "rol", "lat", "lon", "nod", "nav", "fig", "nom", "nib", "pag", "sop",
        "ral", "bil", "had", "dok", "rid", "mok", "pak", "rav", "rip", "fal", "tod", "til", "tin",
        "hap", "mik", "fan", "pat", "tak", "lab", "mog", "sim", "son", "pin", "lom", "rik", "tap",
        "fir", "has", "bos", "bat", "pok", "hak", "tid", "hav", "sap", "lin", "dib", "hos", "dab",
        "bit", "bar", "rak", "par", "lod", "dos", "bor", "tok", "hil", "mak", "tom", "dig", "fil",
        "fas", "mit", "hob", "har", "mig", "hin", "rad", "mas", "hal", "rag", "lag", "fad", "top",
        "mop", "hab", "nil", "nos", "mil", "fop", "fam", "dat", "nol", "din", "hat", "nak", "ris",
        "fot", "rib", "hok", "nim", "lar", "fit", "wal", "rap", "sar", "nal", "mos", "lan", "don",
        "dan", "lad", "dov", "riv", "bak", "pol", "lap", "tal", "pit", "nam", "bon", "ros", "ton",
        "fob", "pon", "sov", "nok", "sor", "lav", "mat", "mip", "fep",
    )

    private val RAW_SUFFIX = arrayOf(
        "zod", "nek", "bud", "wes", "sev", "per", "sut", "let", "ful", "pen", "syt", "dur", "wep",
        "ser", "wyl", "sun", "ryp", "syk", "dyr", "nup", "heb", "pog", "lup", "dep", "dys", "but",
        "lug", "hek", "ryt", "tyv", "syd", "nex", "lun", "mep", "lut", "sep", "pes", "del", "sul",
        "ked", "tem", "led", "tul", "met", "wen", "byn", "hex", "feb", "pyl", "dul", "het", "mev",
        "rut", "tyl", "wyd", "tep", "bes", "dex", "sef", "wyk", "bur", "der", "nep", "pur", "rys",
        "reb", "den", "nut", "sub", "pet", "rul", "syn", "reg", "tyd", "sup", "sem", "wyn", "rek",
        "meg", "net", "sek", "mul", "nym", "tev", "web", "sum", "mut", "nyx", "rex", "teb", "fus",
        "hep", "ben", "mus", "wyx", "sym", "sel", "ruk", "dek", "wex", "syr", "wet", "dyl", "myn",
        "mes", "det", "bet", "bel", "tux", "tug", "myr", "pel", "syp", "ter", "meb", "set", "dut",
        "deg", "tex", "sur", "fel", "tud", "nux", "rux", "ren", "wyt", "nub", "med", "lyt", "dus",
        "neb", "rum", "tyn", "seg", "lyx", "pun", "res", "red", "fun", "rev", "ref", "mek", "ted",
        "rus", "bex", "leb", "dux", "ryn", "num", "pyx", "ryg", "ryx", "fep", "tyr", "tus", "tyk",
        "leg", "nem", "fer", "mer", "ten", "lus", "nus", "syl", "tek", "mex", "pud", "rym", "tuk",
        "fyl", "lep", "deb", "ber", "mug", "hut", "tun", "byl", "sud", "pem", "dev", "lur", "def",
        "bus", "bep", "run", "mel", "pex", "dyt", "byt", "typ", "lev", "myl", "wed", "duk", "fur",
        "fex", "nul", "luk", "len", "ner", "lex", "rup", "ned", "lek", "ryd", "lyd", "fen", "wel",
        "nyd", "hus", "rel", "rud", "nes", "hes", "fet", "des", "ret", "dun", "ler", "nyr", "seb",
        "hul", "ryl", "lud", "rem", "lys", "fyn", "wer", "ryk", "sug", "nys", "nyl", "lyn", "dyn",
        "dem", "lux", "fed", "sed", "bek", "mun", "lyr", "tes", "mud", "nyt", "byr", "sen", "weg",
        "fyr", "mur", "tel", "rep", "teg", "pek", "nel", "nev", "fes",
    )

}
