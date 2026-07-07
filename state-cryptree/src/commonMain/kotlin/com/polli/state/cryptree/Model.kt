package com.polli.state.cryptree

/**
 * Core data model for Polli's private-first state layer.
 *
 * The design mirrors homeserver-style architectures (ATProto PDS / Pubky) but is
 * **private by default**: state lives in a [Cryptree] — a cryptographic tree where a
 * node's *read key* unlocks its subtree for reading and its *write key* authorises
 * mutations. Naming is provided by MNS (see [MnsRecord]); ordered, verifiable
 * collections are backed by a Merkle Search Tree (MAST, "mlkut").
 *
 * Transport is orthogonal: chatmail/e-mail carries the encrypted deltas produced
 * here. This module intentionally knows nothing about transport.
 *
 * NOTE: This is the API scaffold. The production backend will bind the Rust
 * `cryptree` + `mlkut` (MAST) + `mns` crates (via uniffi/JNI); [InMemoryCryptreeStore]
 * is a pure-Kotlin stand-in so the seam is usable and testable today.
 */

/** Opaque, content-addressed identifier for a Cryptree node. */
data class NodeId(val value: String) {
    init {
        require(value.isNotBlank()) { "NodeId must not be blank" }
    }

    companion object {
        val ROOT = NodeId("root")
    }
}

/** A participant identity (public key, MNS-resolvable). */
data class Identity(val publicKey: String) {
    init {
        require(publicKey.isNotBlank()) { "Identity publicKey must not be blank" }
    }
}

/**
 * Cryptree access keys for a node.
 *
 * - [readKey] grants read access to this node and, via key derivation, its subtree.
 * - [writeKey] (present only for holders/moderators) authorises mutations.
 */
data class NodeKeys(
    val readKey: ByteArray,
    val writeKey: ByteArray?,
) {
    val canWrite: Boolean get() = writeKey != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NodeKeys) return false
        return readKey.contentEquals(other.readKey) &&
            (writeKey?.contentEquals(other.writeKey ?: ByteArray(0)) ?: (other.writeKey == null))
    }

    override fun hashCode(): Int = readKey.contentHashCode() * 31 + (writeKey?.contentHashCode() ?: 0)
}

/**
 * A node in the Cryptree: an encrypted payload plus named, encrypted links to children.
 * In the stub the payload is stored in the clear; the real backend stores ciphertext
 * decryptable only with [NodeKeys.readKey].
 */
data class CryptreeNode(
    val id: NodeId,
    val payload: ByteArray,
    val children: Map<String, NodeId> = emptyMap(),
    /** Merkle root of this subtree (MAST) — used for verification/sync. */
    val subtreeRoot: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CryptreeNode) return false
        return id == other.id &&
            payload.contentEquals(other.payload) &&
            children == other.children &&
            subtreeRoot == other.subtreeRoot
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + children.hashCode()
        result = 31 * result + (subtreeRoot?.hashCode() ?: 0)
        return result
    }
}

/** Access level granted to a moderator over a subtree. */
enum class ModeratorRole { READER, WRITER, ADMIN }

/**
 * A moderator holds keys over a subtree. Multi-moderator support means several
 * identities can hold write keys for the same node (shared administration), each
 * revocable independently by re-keying.
 */
data class Moderator(
    val identity: Identity,
    val role: ModeratorRole,
)

/**
 * An MNS record: a human-readable name resolving to an identity and the Cryptree
 * root that identity publishes. Anchored on-chain (Rootstock/EVM) in production.
 */
data class MnsRecord(
    val name: String,
    val owner: Identity,
    val root: NodeId,
    /** Additional moderators authorised over the root subtree. */
    val moderators: List<Moderator> = emptyList(),
)
