package com.polli.android.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens

private val recentSearches = listOf(
    "Weekend plans",
    "Design sync",
    "Grocery list",
)

private val suggestions = listOf(
    "Start a group chat",
    "Find a contact",
)

private val createTypes = listOf("Task", "Note", "Event", "Poll")

@Composable
internal fun HomeSearchPanelBody(
    expandProgress: Float,
    onRecentSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bodyAlpha = ((expandProgress - 0.25f) / 0.75f).coerceIn(0f, 1f)
    if (bodyAlpha <= 0.01f) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(bodyAlpha)
            .padding(top = 4.dp),
    ) {
        if (expandProgress > 0.35f) {
            Text(
                text = "Recent",
                color = LabColors.White33,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 14.dp, bottom = 4.dp),
            )
            recentSearches.forEach { term ->
                HomeSearchListRow(
                    label = term,
                    iconTint = LabColors.White16,
                    onClick = { onRecentSelect(term) },
                )
            }
        }

        if (expandProgress > 0.5f) {
            Text(
                text = "Suggestions",
                color = LabColors.White33,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp),
            )
            suggestions.forEach { term ->
                HomeSearchListRow(
                    label = term,
                    iconTint = LabColors.White16,
                    onClick = { onRecentSelect(term) },
                )
            }
        }

        if (expandProgress > 0.65f) {
            Text(
                text = "Create",
                color = LabColors.White33,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 6.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                createTypes.forEach { label ->
                    HomeCreateChip(label = label)
                }
            }
        }
    }
}

@Composable
private fun HomeSearchListRow(
    label: String,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabIcon(LabIconName.Search, LabDimens.HomeSearchGlyphSize, iconTint)
        Text(
            text = label,
            color = LabColors.White33,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = LabDimens.HomeSearchGapAfterGlyph + 1.dp),
        )
    }
}

@Composable
private fun HomeCreateChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(17.dp))
            .background(LabColors.White8)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = LabColors.White66,
            fontSize = 13.sp,
        )
    }
}
