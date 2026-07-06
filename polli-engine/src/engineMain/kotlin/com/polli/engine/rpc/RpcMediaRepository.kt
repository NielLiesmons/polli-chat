package com.polli.engine.rpc

import chat.delta.rpc.Rpc
import chat.delta.rpc.RpcException
import chat.delta.rpc.types.Viewtype
import com.polli.core.chat.ChatMediaFilter
import com.polli.core.chat.MsgTypes
import com.polli.domain.repository.MediaRepository

class RpcMediaRepository(
    private val rpc: Rpc,
    private val accountId: Int,
) : MediaRepository {
    override fun messageIdsForFilter(chatId: Int, filter: ChatMediaFilter): IntArray {
        return try {
            val t1 = filter.type1.toViewtype()
            val t2 = filter.type2.toViewtype()
            val t3 = filter.type3.toViewtype()
            val ids =
                rpc.getChatMedia(accountId, chatId, t1, t2, t3) ?: emptyList()
            ids.map { it.toInt() }.reversed().toIntArray()
        } catch (_: RpcException) {
            intArrayOf()
        }
    }

    private fun Int.toViewtype(): Viewtype? =
        when (this) {
            0 -> null
            MsgTypes.IMAGE -> Viewtype.Image
            MsgTypes.GIF -> Viewtype.Gif
            MsgTypes.AUDIO -> Viewtype.Audio
            MsgTypes.VOICE -> Viewtype.Voice
            MsgTypes.VIDEO -> Viewtype.Video
            MsgTypes.FILE -> Viewtype.File
            MsgTypes.WEBXDC -> Viewtype.Webxdc
            else -> null
        }
}
