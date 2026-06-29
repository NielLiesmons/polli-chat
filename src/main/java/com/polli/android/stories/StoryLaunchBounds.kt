package com.polli.android.stories

/** Root-coordinates of the story ring tapped on the home screen (for expand animation). */
data class StoryLaunchBounds(
    val centerX: Float,
    val centerY: Float,
    val size: Float,
)

data class StorySession(
    val channelId: Int,
    val channelIds: List<Int>,
    val launchBounds: StoryLaunchBounds,
)
