package com.polli.domain.model.space

/**
 * Human-readable MNS name for a space (e.g. `alice.mail`).
 * Resolution and sigil validation live in Rust (`polli-mns`) — this is a typed boundary only.
 */
@JvmInline
value class MnsName(val value: String) {
    init {
        require(value.isNotBlank()) { "MNS name must not be blank" }
    }
}

/**
 * Placeholder for Cryptree-backed spaces ([docs/spaces/ARCHITECTURE.md]).
 * Implementation will live in Rust (`polli-spaces`) + JSON-RPC — not Kotlin.
 */
data class Space(
    val id: String,
    val name: String,
    val mnsName: MnsName? = null,
)

/** Encrypted drive subtree inside a space — wired when cryptree client lands. */
interface SpaceDrive {
    val spaceId: String

    fun listFiles(path: String): List<String>
}

/** Where an inbox row's backing store lives — chatmail today; cryptree spaces later. */
enum class InboxSource {
    Chatmail,
    Space,
}
