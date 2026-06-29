package com.polli.android.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.polli.android.icons.LabIcon
import com.polli.android.icons.LabIconName
import com.polli.android.theme.LabColors
import com.polli.android.theme.LabDimens
import com.polli.android.ui.homeSearchPanelScrollFade
import org.thoughtcrime.securesms.R

private val recentSearches = listOf(
    "Weekend plans",
    "Design sync",
    "Grocery list",
)

private data class FavoriteFileDummy(val baseName: String, val extension: String)

private data class FavoriteColumnDummy(val files: List<FavoriteFileDummy>)

private val favoriteColumns = listOf(
    FavoriteColumnDummy(
        listOf(
            FavoriteFileDummy("logo", "svg"),
            FavoriteFileDummy("part", "stl"),
            FavoriteFileDummy("brief", "doc"),
        ),
    ),
    FavoriteColumnDummy(
        listOf(
            FavoriteFileDummy("sheet", "xls"),
            FavoriteFileDummy("readme", "md"),
            FavoriteFileDummy("render", "png"),
        ),
    ),
    FavoriteColumnDummy(
        listOf(
            FavoriteFileDummy("schema", "json"),
            FavoriteFileDummy("notes", "txt"),
            FavoriteFileDummy("model", "glb"),
        ),
    ),
    FavoriteColumnDummy(
        listOf(
            FavoriteFileDummy("cover", "jpg"),
            FavoriteFileDummy("track", "mp3"),
            FavoriteFileDummy("archive", "zip"),
        ),
    ),
)

private data class CreateTypeOption(val label: String, @DrawableRes val emojiRes: Int)

private val createTypes = listOf(
    CreateTypeOption("Task", R.drawable.ic_emoji_task),
    CreateTypeOption("Note", R.drawable.ic_emoji_note),
    CreateTypeOption("Event", R.drawable.ic_emoji_event),
    CreateTypeOption("Poll", R.drawable.ic_emoji_poll),
    CreateTypeOption("Article", R.drawable.ic_emoji_article),
    CreateTypeOption("Chat", R.drawable.ic_emoji_chat),
)

@Composable
internal fun HomeSearchPanelBody(
    expandProgress: Float,
    onRecentSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onHeightMeasured: ((Dp) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val bodyAlpha = ((expandProgress - 0.2f) / 0.55f).coerceIn(0f, 1f)
    if (bodyAlpha <= 0.01f) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(bodyAlpha)
            .homeSearchPanelScrollFade()
            .padding(bottom = 10.dp)
            .then(
                if (onHeightMeasured != null) {
                    Modifier.onSizeChanged {
                        if (it.height > 0) {
                            onHeightMeasured(with(density) { it.height.toDp() })
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        HomeSearchSectionLabel(label = "Recent")
        recentSearches.forEach { term ->
            HomeSearchListRow(
                label = term,
                onClick = { onRecentSelect(term) },
            )
        }

        HomeSearchSectionDivider()

        HomeSearchSectionLabel(label = "Favorites")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            favoriteColumns.forEach { column ->
                HomeFavoriteColumn(column = column)
            }
        }

        HomeSearchSectionDivider()

        HomeSearchSectionLabel(label = "Create")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            createTypes.forEach { option ->
                HomeCreateTypeCard(option = option)
            }
        }
    }
}

@Composable
private fun HomeSearchSectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        color = LabColors.White33,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.2.sp,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 3.dp),
    )
}

@Composable
private fun HomeSearchSectionDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(maxOf(LabDimens.ShellBorderWidth, 0.33.dp))
            .background(LabColors.ShellBorder),
    )
}

@Composable
private fun HomeSearchListRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabIcon(LabIconName.Search, LabDimens.HomeSearchGlyphSize, LabColors.White16)
        Text(
            text = label,
            color = LabColors.White66,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = LabDimens.HomeSearchGapAfterGlyph + 1.dp),
        )
    }
}

@Composable
private fun HomeFavoriteColumn(column: FavoriteColumnDummy) {
    Column(
        modifier = Modifier.width(LabDimens.HomeSearchFavoriteColumnWidth),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        column.files.forEach { file ->
            HomeFavoriteFileCell(file = file)
        }
    }
}

@Composable
private fun HomeFavoriteFileCell(file: FavoriteFileDummy) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FileExtensionIcon(extension = file.extension)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = file.baseName,
                color = LabColors.White66,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = ".${file.extension}",
                color = LabColors.White33,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HomeCreateTypeCard(option: CreateTypeOption) {
    Column(
        modifier = Modifier
            .width(LabDimens.HomeSearchCreateCardWidth)
            .clip(RoundedCornerShape(14.dp))
            .background(LabColors.White8)
            .clickable { }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(
            painter = painterResource(option.emojiRes),
            contentDescription = option.label,
            modifier = Modifier.size(LabDimens.HomeSearchPanelIconSize),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = option.label,
            color = LabColors.White66,
            style = MaterialTheme.typography.labelMedium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
