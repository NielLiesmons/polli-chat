package com.polli.android.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.polli.android.settings.AppPrefs
import com.polli.android.settings.LocalAppPrefs

@get:JvmName("getLabTypography")
val LabTypography: Typography
    get() = com.polli.ui.theme.LabTypography

object LabDimens {
    val AvatarSize: Dp get() = com.polli.ui.theme.LabDimens.AvatarSize
    val HomeBarPadding: Dp get() = com.polli.ui.theme.LabDimens.HomeBarPadding
    val StoriesRowPadding: Dp get() = com.polli.ui.theme.LabDimens.StoriesRowPadding
    val StoriesRowPaddingStart: Dp get() = com.polli.ui.theme.LabDimens.StoriesRowPaddingStart
    val HomeBarHeight: Dp get() = com.polli.ui.theme.LabDimens.HomeBarHeight
    val HomeProfileSize: Dp get() = com.polli.ui.theme.LabDimens.HomeProfileSize
    val HomeProfileGap: Dp get() = com.polli.ui.theme.LabDimens.HomeProfileGap
    val HomePillInsetBeforeSearch: Dp get() = com.polli.ui.theme.LabDimens.HomePillInsetBeforeSearch
    val HomeSearchGlyphSize: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchGlyphSize
    val HomeSearchGapAfterGlyph: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchGapAfterGlyph
    val HomePillActionSize: Dp get() = com.polli.ui.theme.LabDimens.HomePillActionSize
    val HomeSearchPlusGlyphSize: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchPlusGlyphSize
    val HomeBarVerticalPad: Dp get() = com.polli.ui.theme.LabDimens.HomeBarVerticalPad
    val IconStrokeWidth: Dp get() = com.polli.ui.theme.LabDimens.IconStrokeWidth
    val HomeSearchPanelExpandedHeight: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchPanelExpandedHeight
    val HomeSearchPanelIconSize: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchPanelIconSize
    val HomeSearchPanelTopFade: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchPanelTopFade
    val HomeSearchFavoriteColumnWidth: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchFavoriteColumnWidth
    val HomeSearchCreateCardWidth: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchCreateCardWidth
    val HomeSearchExpandDragDistance: Dp get() = com.polli.ui.theme.LabDimens.HomeSearchExpandDragDistance
    val HomeSearchExpandSnapThreshold: Float get() = com.polli.ui.theme.LabDimens.HomeSearchExpandSnapThreshold
    val TabButtonHeight: Dp get() = com.polli.ui.theme.LabDimens.TabButtonHeight
    val TabButtonHPadding: Dp get() = com.polli.ui.theme.LabDimens.TabButtonHPadding
    val TabButtonUnselectedHeight: Dp get() = com.polli.ui.theme.LabDimens.TabButtonUnselectedHeight
    val TabButtonUnselectedHPadding: Dp get() = com.polli.ui.theme.LabDimens.TabButtonUnselectedHPadding
    val TabButtonUnselectedCorner: Dp get() = com.polli.ui.theme.LabDimens.TabButtonUnselectedCorner
    val TabGap: Dp get() = com.polli.ui.theme.LabDimens.TabGap
    val ChatHeaderTabGap: Dp get() = com.polli.ui.theme.LabDimens.ChatHeaderTabGap
    val TabSectionGap: Dp get() = com.polli.ui.theme.LabDimens.TabSectionGap
    val TabContentTopGap: Dp get() = com.polli.ui.theme.LabDimens.TabContentTopGap
    val TabContentBottomPad: Dp get() = com.polli.ui.theme.LabDimens.TabContentBottomPad
    val StoryRingOuter: Dp get() = com.polli.ui.theme.LabDimens.StoryRingOuter
    val StoryRingInner: Dp get() = com.polli.ui.theme.LabDimens.StoryRingInner
    val StoryRingStroke: Dp get() = com.polli.ui.theme.LabDimens.StoryRingStroke
    val StoryRingGap: Dp get() = com.polli.ui.theme.LabDimens.StoryRingGap
    val StoryRingSpacing: Dp get() = com.polli.ui.theme.LabDimens.StoryRingSpacing
    val StoryRowVerticalPadTop: Dp get() = com.polli.ui.theme.LabDimens.StoryRowVerticalPadTop
    val StoryRowVerticalPadBottom: Dp get() = com.polli.ui.theme.LabDimens.StoryRowVerticalPadBottom
    val StoryRowDividerGap: Dp get() = com.polli.ui.theme.LabDimens.StoryRowDividerGap
    val StoryProgressHeight: Dp get() = com.polli.ui.theme.LabDimens.StoryProgressHeight
    val ShellBorderWidth: Dp get() = com.polli.ui.theme.LabDimens.ShellBorderWidth
    val ShellDividerWidth: Dp get() = com.polli.ui.theme.LabDimens.ShellDividerWidth
    val ListRowPadding: Dp get() = com.polli.ui.theme.LabDimens.ListRowPadding
    val GroupNameNotifGap: Dp get() = com.polli.ui.theme.LabDimens.GroupNameNotifGap
    val InboxPreviewRowMinHeight: Dp get() = com.polli.ui.theme.LabDimens.InboxPreviewRowMinHeight
    val InboxTitleRowMinHeight: Dp get() = com.polli.ui.theme.LabDimens.InboxTitleRowMinHeight
    val UnreadBadgeMinSize: Dp get() = com.polli.ui.theme.LabDimens.UnreadBadgeMinSize
    val UnreadBadgeHPadding: Dp get() = com.polli.ui.theme.LabDimens.UnreadBadgeHPadding
    val ChatAvatarSize: Dp get() = com.polli.ui.theme.LabDimens.ChatAvatarSize
    val ChatAvatarGap: Dp get() = com.polli.ui.theme.LabDimens.ChatAvatarGap
    val ChatBubbleRadius: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleRadius
    val ChatBubbleTailRadius: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleTailRadius
    val ChatBubbleInsetH: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleInsetH
    val ChatBubblePaddingV: Dp get() = com.polli.ui.theme.LabDimens.ChatBubblePaddingV
    val ChatBubbleMetaRowPaddingV: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleMetaRowPaddingV
    val ChatBubbleMetaRowMarginTop: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleMetaRowMarginTop
    val ChatBubbleOutgoingShellBottom: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleOutgoingShellBottom
    val ChatBubbleIncomingBottomExtra: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleIncomingBottomExtra
    val ChatBubbleStackedIncomingTopExtra: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleStackedIncomingTopExtra
    val ChatBubbleTextOnlyExtraTop: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleTextOnlyExtraTop
    val ChatQuoteAccentWidth: Dp get() = com.polli.ui.theme.LabDimens.ChatQuoteAccentWidth
    val ChatQuoteBubblePadH: Dp get() = com.polli.ui.theme.LabDimens.ChatQuoteBubblePadH
    val ChatQuoteMarginBottom: Dp get() = com.polli.ui.theme.LabDimens.ChatQuoteMarginBottom
    val ChatReactionRowTop: Dp get() = com.polli.ui.theme.LabDimens.ChatReactionRowTop
    val ChatRowPaddingH: Dp get() = com.polli.ui.theme.LabDimens.ChatRowPaddingH
    val ChatRowOutgoingExtraStart: Dp get() = com.polli.ui.theme.LabDimens.ChatRowOutgoingExtraStart
    val ChatRowIncomingRight: Dp get() = com.polli.ui.theme.LabDimens.ChatRowIncomingRight
    val ChatRowTop: Dp get() = com.polli.ui.theme.LabDimens.ChatRowTop
    val ChatRowTopCollapsed: Dp get() = com.polli.ui.theme.LabDimens.ChatRowTopCollapsed
    val ChatBubbleMaxWidthFraction: Float get() = com.polli.ui.theme.LabDimens.ChatBubbleMaxWidthFraction
    val ChatBubbleImageMinWidth: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleImageMinWidth
    val ChatBubbleImageMinHeight: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleImageMinHeight
    val ChatBubbleImageMaxHeight: Dp get() = com.polli.ui.theme.LabDimens.ChatBubbleImageMaxHeight
    val ChatIncomingGroupAvatarOffset: Dp get() = com.polli.ui.theme.LabDimens.ChatIncomingGroupAvatarOffset
    val ChatIncomingGroupBubbleGap: Dp get() = com.polli.ui.theme.LabDimens.ChatIncomingGroupBubbleGap
    val ChatFloatingChromeSize: Dp get() = com.polli.ui.theme.LabDimens.ChatFloatingChromeSize
    val ChatComposerMinHeight: Dp get() = com.polli.ui.theme.LabDimens.ChatComposerMinHeight
    val ChatComposerDockHPadding: Dp get() = com.polli.ui.theme.LabDimens.ChatComposerDockHPadding
    val ChatComposerDockBottomMin: Dp get() = com.polli.ui.theme.LabDimens.ChatComposerDockBottomMin
    val ChatComposerKeyboardGap: Dp get() = com.polli.ui.theme.LabDimens.ChatComposerKeyboardGap
    val ChatComposerDockClearanceExtra: Dp get() = com.polli.ui.theme.LabDimens.ChatComposerDockClearanceExtra
    val ChatScrollFabGapAboveComposer: Dp get() = com.polli.ui.theme.LabDimens.ChatScrollFabGapAboveComposer
    val ChatComposerPlusSize: Dp get() = com.polli.ui.theme.LabDimens.ChatComposerPlusSize
    val ChatComposerSendSize: Dp get() = com.polli.ui.theme.LabDimens.ChatComposerSendSize
    val ChatComposerFieldGap: Dp get() = com.polli.ui.theme.LabDimens.ChatComposerFieldGap
    val ScrollFadeBottom: Dp get() = com.polli.ui.theme.LabDimens.ScrollFadeBottom
    val ScrollFadeTop: Dp get() = com.polli.ui.theme.LabDimens.ScrollFadeTop
    val ChatFeedTopFadeExtend: Dp get() = com.polli.ui.theme.LabDimens.ChatFeedTopFadeExtend
    val ChatFeedBottomFadeExtend: Dp get() = com.polli.ui.theme.LabDimens.ChatFeedBottomFadeExtend
    val ChatScrollFadeBottom: Dp get() = com.polli.ui.theme.LabDimens.ChatScrollFadeBottom
    val ChatScrollFadeBottomMid: Dp get() = com.polli.ui.theme.LabDimens.ChatScrollFadeBottomMid
    val ModalInset: Dp get() = com.polli.ui.theme.LabDimens.ModalInset
    val ModalScreenInset: Dp get() = com.polli.ui.theme.LabDimens.ModalScreenInset
    val ModalRadius: Dp get() = com.polli.ui.theme.LabDimens.ModalRadius
    val ModalBottomFade: Dp get() = com.polli.ui.theme.LabDimens.ModalBottomFade
    val ModalTitleTopPad: Dp get() = com.polli.ui.theme.LabDimens.ModalTitleTopPad
    val ModalTitleDescGap: Dp get() = com.polli.ui.theme.LabDimens.ModalTitleDescGap
    val DetailBackButtonSize: Dp get() = com.polli.ui.theme.LabDimens.DetailBackButtonSize
    val ProfileCardWidth: Dp get() = com.polli.ui.theme.LabDimens.ProfileCardWidth
    val GroupHeaderClearance: Dp get() = com.polli.ui.theme.LabDimens.GroupHeaderClearance
}

object ProfileColor {
    fun hue(seed: String): Float = com.polli.ui.theme.ProfileColor.hue(seed)
    fun background(seed: String): Color = com.polli.ui.theme.ProfileColor.background(seed)
    fun text(seed: String): Color = com.polli.ui.theme.ProfileColor.text(seed)
}

object ProfileColors {
    fun stringToColor(value: String): Color = com.polli.ui.theme.ProfileColors.stringToColor(value)
    fun profileTextColor(base: Color): Color = com.polli.ui.theme.ProfileColors.profileTextColor(base)
    fun authorNameColor(colorSeed: String): Color = com.polli.ui.theme.ProfileColors.authorNameColor(colorSeed)
    fun fromDcRgb(rgb: Int): Color = com.polli.ui.theme.ProfileColors.fromDcRgb(rgb)
}

@Composable
fun LabTheme(
    prefs: AppPrefs,
    uiScaleRevision: Int = 0,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppPrefs provides prefs) {
        com.polli.ui.theme.LabTheme(prefs = prefs, uiScaleRevision = uiScaleRevision, content = content)
    }
}
