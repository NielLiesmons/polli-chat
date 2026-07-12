package com.polli.android.benchmark

import android.content.Intent
import android.util.Log
import androidx.core.app.FrameMetricsAggregator
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import org.hamcrest.Matchers.endsWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.polli.android.BuildConfig
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class ChatFeedBenchmark {
    @Test
    fun openBenchmarkChatUnder400Ms() {
        assumeTrue("Benchmark build only", BuildConfig.BENCHMARK_MODE)
        val start = System.currentTimeMillis()
        ActivityScenario.launch<ChatFeedBenchmarkActivity>(
            Intent(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ChatFeedBenchmarkActivity::class.java,
            ),
        ).use { scenario ->
            scenario.onActivity { activity ->
                activity.window.decorView.post {
                    // first layout pass
                }
            }
            onView(isRoot())
            val elapsed = System.currentTimeMillis() - start
            Log.i(TAG, "open_ms=$elapsed")
            check(elapsed < OPEN_BUDGET_MS) { "Open took ${elapsed}ms (budget ${OPEN_BUDGET_MS}ms)" }
        }
    }

    @Test
    fun scrollBenchmarkChatWithoutExcessiveJank() {
        assumeTrue("Benchmark build only", BuildConfig.BENCHMARK_MODE)
        val aggregator = FrameMetricsAggregator()
        ActivityScenario.launch<ChatFeedBenchmarkActivity>(
            Intent(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ChatFeedBenchmarkActivity::class.java,
            ),
        ).use { scenario ->
            scenario.onActivity { activity ->
                aggregator.add(activity)
            }
            onView(isRoot())
            scenario.onActivity { it.benchmarkRepository().resetBindPathCalls() }
            val feedMatcher = withClassName(endsWith("RecyclerView"))
            repeat(8) {
                onView(feedMatcher).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(500))
                onView(feedMatcher).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(50))
            }
            scenario.onActivity { activity ->
                val metrics = aggregator.remove(activity)?.get(FrameMetricsAggregator.TOTAL_INDEX)
                val total = metrics?.get(0) ?: 0
                val slow = metrics?.get(1) ?: 0
                val jankPct = if (total > 0) slow * 100f / total else 0f
                Log.i(TAG, "jank_pct=$jankPct bind_calls=${activity.benchmarkRepository().bindPathCallCount}")
                check(jankPct < JANK_BUDGET_PCT) { "Jank ${jankPct}% exceeds ${JANK_BUDGET_PCT}%" }
                check(activity.benchmarkRepository().bindPathCallCount == 0) {
                    "Bind path triggered ${activity.benchmarkRepository().bindPathCallCount} repository calls"
                }
            }
        }
    }

    companion object {
        private const val TAG = "ChatFeedBenchmark"
        private const val OPEN_BUDGET_MS = 400
        private const val JANK_BUDGET_PCT = 5f
    }
}
