package com.polli.state.cryptree

/**
 * Read/write access to a Cryptree.
 *
 * All operations are keyed: a caller must present [NodeKeys] proving read (for
 * [read]) or write (for mutations) access. Implementations enforce that a write
 * key is present before mutating.
 */
interface CryptreeStore {

    /** Fetch a node, or null if absent / not readable with [keys]. */
    suspend fun read(id: NodeId, keys: NodeKeys): CryptreeNode?

    /** Resolve a slash-separated path from a root, following child links. */
    suspend fun resolvePath(root: NodeId, path: List<String>, keys: NodeKeys): CryptreeNode?

    /** Create or replace a node's payload. Requires a write key. */
    suspend fun write(id: NodeId, payload: ByteArray, keys: NodeKeys): CryptreeNode

    /** Link [child] under [parent] as [name]. Requires a write key on [parent]. */
    suspend fun link(parent: NodeId, name: String, child: NodeId, keys: NodeKeys)

    /** Remove the [name] link from [parent]. Requires a write key on [parent]. */
    suspend fun unlink(parent: NodeId, name: String, keys: NodeKeys)

    /**
     * Grant [moderator] access over [node]'s subtree, returning the keys the
     * moderator should hold. Requires ADMIN/write on [node].
     */
    suspend fun grant(node: NodeId, moderator: Moderator, keys: NodeKeys): NodeKeys

    /**
     * Revoke a moderator by re-keying [node]'s subtree; previously shared keys no
     * longer decrypt future writes. Requires ADMIN on [node].
     */
    suspend fun revoke(node: NodeId, moderator: Identity, keys: NodeKeys)
}

/** Thrown when an operation is attempted without sufficient key access. */
class CryptreeAccessException(message: String) : Exception(message)
