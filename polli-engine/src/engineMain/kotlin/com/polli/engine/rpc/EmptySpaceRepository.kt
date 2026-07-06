package com.polli.engine.rpc

import com.polli.domain.model.space.Space
import com.polli.domain.repository.SpaceRepository

/** Placeholder until `polli-spaces` JSON-RPC lands. */
class EmptySpaceRepository : SpaceRepository {
    override fun listSpaces(): List<Space> = emptyList()
}
