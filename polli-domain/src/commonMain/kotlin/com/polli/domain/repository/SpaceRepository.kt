package com.polli.domain.repository

import com.polli.domain.model.space.Space

/**
 * Cryptree space listing — stub until `polli-spaces` exposes JSON-RPC.
 * Chatmail inbox uses [ChatRepository]; spaces tab will use this.
 */
interface SpaceRepository {
    fun listSpaces(): List<Space>
}
