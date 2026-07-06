package com.polli.domain.model.space

/**
 * Placeholder for Cryptree-backed spaces ([docs/spaces/ARCHITECTURE.md]).
 * Implementation will live in Rust (`polli-spaces`) + JSON-RPC — not Kotlin.
 */
data class Space(
    val id: String,
    val name: String,
    val mnsName: String? = null,
)

/** Encrypted drive subtree inside a space — wired when cryptree client lands. */
interface SpaceDrive {
    val spaceId: String

    fun listFiles(path: String): List<String>
}
