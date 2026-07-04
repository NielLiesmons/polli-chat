package com.polli.android.data.engine

import android.content.Context
import com.polli.android.media.MediaGalleryLoad
import com.polli.core.chat.ChatMediaFilter
import com.polli.domain.repository.MediaRepository

/** Android adapter — wraps engine media queries behind [MediaRepository]. */
class EngineMediaRepository(context: Context) : MediaRepository {
    private val appContext = context.applicationContext

    override fun messageIdsForFilter(chatId: Int, filter: ChatMediaFilter): IntArray {
        val ids = MediaGalleryLoad.mediaIds(
            appContext,
            chatId,
            filter.type1,
            filter.type2,
            filter.type3,
        )
        return ids.reversedArray()
    }
}
