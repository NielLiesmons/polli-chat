package com.polli.state.cryptree

/**
 * Resolves MNS names to their published Cryptree root + moderators.
 *
 * Production backend reads the MNS registry (Rootstock/EVM) and caches records;
 * [InMemoryMnsResolver] is a local stand-in for development and tests.
 */
interface MnsResolver {

    /** Resolve a name (e.g. "alice.polli") to its record, or null if unregistered. */
    suspend fun resolve(name: String): MnsRecord?

    /** Register or update a record. In production this is an on-chain transaction. */
    suspend fun publish(record: MnsRecord)

    /** Reverse lookup: names owned by an identity. */
    suspend fun namesOf(owner: Identity): List<String>
}
