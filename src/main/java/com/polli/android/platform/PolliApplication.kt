package com.polli.android.platform

import android.content.Context
import com.polli.android.ApplicationContext

object PolliApplication {
    fun getInstance(context: Context): ApplicationContext = ApplicationContext.getInstance(context)
}
