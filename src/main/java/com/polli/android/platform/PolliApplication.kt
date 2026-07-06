package com.polli.android.platform

import android.content.Context
import org.thoughtcrime.securesms.ApplicationContext

object PolliApplication {
    fun getInstance(context: Context): ApplicationContext = ApplicationContext.getInstance(context)
}
