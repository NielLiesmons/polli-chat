package com.polli.state.cryptree

/**
 * App-facing facade that ties MNS naming to Cryptree storage — the seam that
 * `polli-engine` (and, through it, the UI) uses to read/write private state.
 */
interface PolliStateRepository {
    val store: CryptreeStore
    val mns: MnsResolver

    /** Resolve an MNS [name] to its root and read that node with [keys]. */
    suspend fun openRoot(name: String, keys: NodeKeys): CryptreeNode?
}

class DefaultPolliStateRepository(
    override val store: CryptreeStore,
    override val mns: MnsResolver,
) : PolliStateRepository {

    override suspend fun openRoot(name: String, keys: NodeKeys): CryptreeNode? {
        val record = mns.resolve(name) ?: return null
        return store.read(record.root, keys)
    }
}

/**
 * Entry point for obtaining a [PolliStateRepository].
 *
 * Today this returns an in-memory scaffold. When the Rust `cryptree`/`mlkut`/`mns`
 * crates are bound, add a `native()`/`persistent(...)` factory here and switch the
 * engine over — call sites depend only on [PolliStateRepository].
 */
object PolliState {

    /** In-memory implementation for development and tests. */
    fun inMemory(): PolliStateRepository =
        DefaultPolliStateRepository(InMemoryCryptreeStore(), InMemoryMnsResolver())
}
