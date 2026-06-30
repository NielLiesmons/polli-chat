package com.polli.core.sigil

/**
 * Cached chatmail / MNS identity → sigil resolution for avatar overlays and labels.
 * Grid + name are computed once per normalized identity string.
 */
object SigilIdentity {
    data class Resolved(val value: ULong, val name: String)

    private const val MAX_CACHE = 512
    private val cache = LinkedHashMap<String, Resolved>(MAX_CACHE, 0.75f, true)

    fun resolve(identity: String): Resolved {
        val key = identity.trim().lowercase()
        if (key.isEmpty()) {
            return Resolved(0uL, MnsSigil.encodeName(0uL))
        }
        synchronized(cache) {
            cache[key]?.let { return it }
            val value = MnsSigil.valueFromIdentity(key)
            val resolved = Resolved(value, MnsSigil.encodeName(value))
            cache[key] = resolved
            return resolved
        }
    }
}
