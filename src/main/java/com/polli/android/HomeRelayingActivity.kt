package com.polli.android

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import java.lang.ref.WeakReference

/**
 * Second home instance for share/forward picker flows.
 *
 * [HomeActivity] uses `singleTask`; relay needs a separate activity so the existing
 * home stack is not destroyed (same pattern as legacy ConversationListRelayingActivity).
 */
class HomeRelayingActivity : HomeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = WeakReference(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance?.get() === this) {
            instance = null
        }
    }

    companion object {
        private var instance: WeakReference<HomeRelayingActivity>? = null

        @JvmStatic
        fun start(fragment: Fragment, intent: Intent) {
            intent.component = ComponentName(fragment.requireContext(), HomeRelayingActivity::class.java)
            fragment.startActivity(intent)
        }

        @JvmStatic
        fun start(activity: Activity, intent: Intent) {
            intent.component = ComponentName(activity, HomeRelayingActivity::class.java)
            activity.startActivity(intent)
        }

        @JvmStatic
        fun finishActivity() {
            instance?.get()?.finish()
        }
    }
}
