package com.polli.core

/** Feature toggles for incremental rollout — prefer Rust/engine flags for transport logic. */
object PolliFeatures {
    /** Webxdc mini-apps; off until chat path is stable. */
    const val WEBXDC_ENABLED: Boolean = false
}
