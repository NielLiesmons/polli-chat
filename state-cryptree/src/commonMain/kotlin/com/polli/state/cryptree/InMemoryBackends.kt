package com.polli.state.cryptree

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pure-Kotlin, in-memory [CryptreeStore] used for development and tests.
 *
 * It models real access control (read/write keys are checked and re-keying revokes
 * old keys) but performs no actual encryption and holds no persistence. The
 * production backend will replace this by binding the Rust `cryptree`/`mlkut` crates.
 */
class InMemoryCryptreeStore : CryptreeStore {

    private val mutex = Mutex()
    private val nodes = mutableMapOf<NodeId, CryptreeNode>()
    private val readKeys = mutableMapOf<NodeId, ByteArray>()
    private val writeKeys = mutableMapOf<NodeId, ByteArray>()
    private var keyCounter = 0

    private fun nextKey(seed: String): ByteArray = "$seed-${keyCounter++}".encodeToByteArray()

    private fun requireRead(id: NodeId, keys: NodeKeys) {
        val truth = readKeys[id] ?: return
        if (!truth.contentEquals(keys.readKey)) {
            throw CryptreeAccessException("read key does not grant access to $id")
        }
    }

    private fun requireWrite(id: NodeId, keys: NodeKeys) {
        val wk = keys.writeKey ?: throw CryptreeAccessException("write key required for $id")
        val truth = writeKeys[id]
        if (truth != null && !truth.contentEquals(wk)) {
            throw CryptreeAccessException("write key does not grant access to $id")
        }
    }

    override suspend fun read(id: NodeId, keys: NodeKeys): CryptreeNode? = mutex.withLock {
        requireRead(id, keys)
        nodes[id]
    }

    override suspend fun resolvePath(root: NodeId, path: List<String>, keys: NodeKeys): CryptreeNode? =
        mutex.withLock {
            var current = nodes[root] ?: return@withLock null
            requireRead(current.id, keys)
            for (segment in path) {
                val childId = current.children[segment] ?: return@withLock null
                current = nodes[childId] ?: return@withLock null
            }
            current
        }

    override suspend fun write(id: NodeId, payload: ByteArray, keys: NodeKeys): CryptreeNode =
        mutex.withLock {
            requireWrite(id, keys)
            readKeys.putIfAbsentCompat(id, keys.readKey)
            keys.writeKey?.let { writeKeys.putIfAbsentCompat(id, it) }
            val existing = nodes[id]
            val node = (existing ?: CryptreeNode(id, payload)).copy(payload = payload)
            nodes[id] = node
            node
        }

    override suspend fun link(parent: NodeId, name: String, child: NodeId, keys: NodeKeys) =
        mutex.withLock {
            requireWrite(parent, keys)
            val node = nodes[parent] ?: throw CryptreeAccessException("no such parent $parent")
            nodes[parent] = node.copy(children = node.children + (name to child))
        }

    override suspend fun unlink(parent: NodeId, name: String, keys: NodeKeys) = mutex.withLock {
        requireWrite(parent, keys)
        val node = nodes[parent] ?: return@withLock
        nodes[parent] = node.copy(children = node.children - name)
    }

    override suspend fun grant(node: NodeId, moderator: Moderator, keys: NodeKeys): NodeKeys =
        mutex.withLock {
            requireWrite(node, keys)
            val read = readKeys[node] ?: keys.readKey
            val write = if (moderator.role == ModeratorRole.READER) null else writeKeys[node] ?: keys.writeKey
            NodeKeys(read, write)
        }

    override suspend fun revoke(node: NodeId, moderator: Identity, keys: NodeKeys) = mutex.withLock {
        requireWrite(node, keys)
        // Re-key: rotate read+write keys so previously shared keys stop working.
        readKeys[node] = nextKey("read")
        writeKeys[node] = nextKey("write")
    }

    private fun <K, V> MutableMap<K, V>.putIfAbsentCompat(key: K, value: V) {
        if (!containsKey(key)) put(key, value)
    }
}

/** In-memory [MnsResolver] for development and tests. */
class InMemoryMnsResolver : MnsResolver {

    private val mutex = Mutex()
    private val records = mutableMapOf<String, MnsRecord>()

    override suspend fun resolve(name: String): MnsRecord? = mutex.withLock { records[name] }

    override suspend fun publish(record: MnsRecord) = mutex.withLock {
        records[record.name] = record
    }

    override suspend fun namesOf(owner: Identity): List<String> = mutex.withLock {
        records.values.filter { it.owner == owner }.map { it.name }
    }
}
