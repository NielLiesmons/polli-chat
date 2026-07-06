package com.polli.android.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.polli.android.settings.AppPrefs
import com.polli.android.settings.LocalAppPrefs

@get:JvmName("getPolliTypography")
val PolliTypography: Typography
    get() = com.polli.ui.theme.PolliTypography

object PolliDimens {
    val AvatarSize: Dp get() = com.polli.ui.theme.PolliDimens.AvatarSize
    val HomeBarPadding: Dp get() = com.polli.ui.theme.PolliDimens.HomeBarPadding
    val StoriesRowPadding: Dp get() = com.polli.ui.theme.PolliDimens.StoriesRowPadding
    val StoriesRowPaddingStart: Dp get() = com.polli.ui.theme.PolliDimens.StoriesRowPaddingStart
    val HomeBarHeight: Dp get() = com.polli.ui.theme.PolliDimens.HomeBarHeight
    val HomeProfileSize: Dp get() = com.polli.ui.theme.PolliDimens.HomeProfileSize
    val HomeProfileGap: Dp get() = com.polli.ui.theme.PolliDimens.HomeProfileGap
    val HomePillInsetBeforeSearch: Dp get() = com.polli.ui.theme.PolliDimens.HomePillInsetBeforeSearch
    val HomeSearchGlyphSize: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchGlyphSize
    val HomeSearchGapAfterGlyph: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchGapAfterGlyph
    val HomePillActionSize: Dp get() = com.polli.ui.theme.PolliDimens.HomePillActionSize
    val HomeSearchPlusGlyphSize: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchPlusGlyphSize
    val HomeBarVerticalPad: Dp get() = com.polli.ui.theme.PolliDimens.HomeBarVerticalPad
    val IconStrokeWidth: Dp get() = com.polli.ui.theme.PolliDimens.IconStrokeWidth
    val HomeSearchPanelExpandedHeight: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchPanelExpandedHeight
    val HomeSearchPanelIconSize: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchPanelIconSize
    val HomeSearchPanelTopFade: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchPanelTopFade
    val HomeSearchFavoriteColumnWidth: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchFavoriteColumnWidth
    val HomeSearchCreateCardWidth: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchCreateCardWidth
    val HomeSearchExpandDragDistance: Dp get() = com.polli.ui.theme.PolliDimens.HomeSearchExpandDragDistance
    val HomeSearchExpandSnapThreshold: Float get() = com.polli.ui.theme.PolliDimens.HomeSearchExpandSnapThreshold
    val TabButtonHeight: Dp get() = com.polli.ui.theme.PolliDimens.TabButtonHeight
    val TabButtonHPadding: Dp get() = com.polli.ui.theme.PolliDimens.TabButtonHPadding
    val TabButtonUnselectedHeight: Dp get() = com.polli.ui.theme.PolliDimens.TabButtonUnselectedHeight
    val TabButtonUnselectedHPadding: Dp get() = com.polli.ui.theme.PolliDimens.TabButtonUnselectedHPadding
    val TabButtonUnselectedCorner: Dp get() = com.polli.ui.theme.PolliDimens.TabButtonUnselectedCorner
    val TabGap: Dp get() = com.polli.ui.theme.PolliDimens.TabGap
    val ChatHeaderTabGap: Dp get() = com.polli.ui.theme.PolliDimens.ChatHeaderTabGap
    val TabSectionGap: Dp get() = com.polli.ui.theme.PolliDimens.TabSectionGap
    val TabContentTopGap: Dp get() = com.polli.ui.theme.PolliDimens.TabContentTopGap
    val TabContentBottomPad: Dp get() = com.polli.ui.theme.PolliDimens.TabContentBottomPad
    val StoryRingOuter: Dp get() = com.polli.ui.theme.PolliDimens.StoryRingOuter
    val StoryRingInner: Dp get() = com.polli.ui.theme.PolliDimens.StoryRingInner
    val StoryRingStroke: Dp get() = com.polli.ui.theme.PolliDimens.StoryRingStroke
    val StoryRingGap: Dp get() = com.polli.ui.theme.PolliDimens.StoryRingGap
    val StoryRingSpacing: Dp get() = com.polli.ui.theme.PolliDimens.StoryRingSpacing
    val StoryRowVerticalPadTop: Dp get() = com.polli.ui.theme.PolliDimens.StoryRowVerticalPadTop
    val StoryRowVerticalPadBottom: Dp get() = com.polli.ui.theme.PolliDimens.StoryRowVerticalPadBottom
    val StoryRowDividerGap: Dp get() = com.polli.ui.theme.PolliDimens.StoryRowDividerGap
    val StoryProgressHeight: Dp get() = com.polli.ui.theme.PolliDimens.StoryProgressHeight
    val ShellBorderWidth: Dp get() = com.polli.ui.theme.PolliDimens.ShellBorderWidth
    val ShellDividerWidth: Dp get() = com.polli.ui.theme.PolliDimens.ShellDividerWidth
    val ListRowPadding: Dp get() = com.polli.ui.theme.PolliDimens.ListRowPadding
    val GroupNameNotifGap: Dp get() = com.polli.ui.theme.PolliDimens.GroupNameNotifGap
    val InboxPreviewRowMinHeight: Dp get() = com.polli.ui.theme.PolliDimens.InboxPreviewRowMinHeight
    val InboxTitleRowMinHeight: Dp get() = com.polli.ui.theme.PolliDimens.InboxTitleRowMinHeight
    val UnreadBadgeMinSize: Dp get() = com.polli.ui.theme.PolliDimens.UnreadBadgeMinSize
    val UnreadBadgeHPadding: Dp get() = com.polli.ui.theme.PolliDimens.UnreadBadgeHPadding
    val ChatAvatarSize: Dp get() = com.polli.ui.theme.PolliDimens.ChatAvatarSize
    val ChatAvatarGap: Dp get() = com.polli.ui.theme.PolliDimens.ChatAvatarGap
    val ChatBubbleRadius: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleRadius
    val ChatBubbleTailRadius: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleTailRadius
    val ChatBubbleInsetH: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleInsetH
    val ChatBubblePaddingV: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubblePaddingV
    val ChatBubbleMetaRowPaddingV: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleMetaRowPaddingV
    val ChatBubbleMetaRowMarginTop: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleMetaRowMarginTop
    val ChatBubbleOutgoingShellBottom: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleOutgoingShellBottom
    val ChatBubbleIncomingBottomExtra: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleIncomingBottomExtra
    val ChatBubbleStackedIncomingTopExtra: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleStackedIncomingTopExtra
    val ChatBubbleTextOnlyExtraTop: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleTextOnlyExtraTop
    val ChatQuoteAccentWidth: Dp get() = com.polli.ui.theme.PolliDimens.ChatQuoteAccentWidth
    val ChatQuoteBubblePadH: Dp get() = com.polli.ui.theme.PolliDimens.ChatQuoteBubblePadH
    val ChatQuoteMarginBottom: Dp get() = com.polli.ui.theme.PolliDimens.ChatQuoteMarginBottom
    val ChatReactionRowTop: Dp get() = com.polli.ui.theme.PolliDimens.ChatReactionRowTop
    val ChatRowPaddingH: Dp get() = com.polli.ui.theme.PolliDimens.ChatRowPaddingH
    val ChatRowOutgoingExtraStart: Dp get() = com.polli.ui.theme.PolliDimens.ChatRowOutgoingExtraStart
    val ChatRowIncomingRight: Dp get() = com.polli.ui.theme.PolliDimens.ChatRowIncomingRight
    val ChatRowTop: Dp get() = com.polli.ui.theme.PolliDimens.ChatRowTop
    val ChatRowTopCollapsed: Dp get() = com.polli.ui.theme.PolliDimens.ChatRowTopCollapsed
    val ChatBubbleMaxWidthFraction: Float get() = com.polli.ui.theme.PolliDimens.ChatBubbleMaxWidthFraction
    val ChatBubbleImageMinWidth: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleImageMinWidth
    val ChatBubbleImageMinHeight: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleImageMinHeight
    val ChatBubbleImageMaxHeight: Dp get() = com.polli.ui.theme.PolliDimens.ChatBubbleImageMaxHeight
    val ChatIncomingGroupAvatarOffset: Dp get() = com.polli.ui.theme.PolliDimens.ChatIncomingGroupAvatarOffset
    val ChatIncomingGroupBubbleGap: Dp get() = com.polli.ui.theme.PolliDimens.ChatIncomingGroupBubbleGap
    val ChatFloatingChromeSize: Dp get() = com.polli.ui.theme.PolliDimens.ChatFloatingChromeSize
    val ChatComposerMinHeight: Dp get() = com.polli.ui.theme.PolliDimens.ChatComposerMinHeight
    val ChatComposerDockHPadding: Dp get() = com.polli.ui.theme.PolliDimens.ChatComposerDockHPadding
    val ChatComposerDockBottomMin: Dp get() = com.polli.ui.theme.PolliDimens.ChatComposerDockBottomMin
    val ChatComposerKeyboardGap: Dp get() = com.polli.ui.theme.PolliDimens.ChatComposerKeyboardGap
    val ChatComposerDockClearanceExtra: Dp get() = com.polli.ui.theme.PolliDimens.ChatComposerDockClearanceExtra
    val ChatScrollFabGapAboveComposer: Dp get() = com.polli.ui.theme.PolliDimens.ChatScrollFabGapAboveComposer
    val ChatComposerPlusSize: Dp get() = com.polli.ui.theme.PolliDimens.ChatComposerPlusSize
    val ChatComposerSendSize: Dp get() = com.polli.ui.theme.PolliDimens.ChatComposerSendSize
    val ChatComposerFieldGap: Dp get() = com.polli.ui.theme.PolliDimens.ChatComposerFieldGap
    val ScrollFadeBottom: Dp get() = com.polli.ui.theme.PolliDimens.ScrollFadeBottom
    val ScrollFadeTop: Dp get() = com.polli.ui.theme.PolliDimens.ScrollFadeTop
    val ChatFeedTopFadeExtend: Dp get() = com.polli.ui.theme.PolliDimens.ChatFeedTopFadeExtend
    val ChatFeedBottomFadeExtend: Dp get() = com.polli.ui.theme.PolliDimens.ChatFeedBottomFadeExtend
    val ChatScrollFadeBottom: Dp get() = com.polli.ui.theme.PolliDimens.ChatScrollFadeBottom
    val ChatScrollFadeBottomMid: Dp get() = com.polli.ui.theme.PolliDimens.ChatScrollFadeBottomMid
    val ModalInset: Dp get() = com.polli.ui.theme.PolliDimens.ModalInset
    val ModalScreenInset: Dp get() = com.polli.ui.theme.PolliDimens.ModalScreenInset
    val ModalRadius: Dp get() = com.polli.ui.theme.PolliDimens.ModalRadius
    val ModalBottomFade: Dp get() = com.polli.ui.theme.PolliDimens.ModalBottomFade
    val ModalTitleTopPad: Dp get() = com.polli.ui.theme.PolliDimens.ModalTitleTopPad
    val ModalTitleDescGap: Dp get() = com.polli.ui.theme.PolliDimens.ModalTitleDescGap
    val DetailBackButtonSize: Dp get() = com.polli.ui.theme.PolliDimens.DetailBackButtonSize
    val ProfileCardWidth: Dp get() = com.polli.ui.theme.PolliDimens.ProfileCardWidth
    val GroupHeaderClearance: Dp get() = com.polli.ui.theme.PolliDimens.GroupHeaderClearance
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
fun PolliTheme(
    prefs: AppPrefs,
    uiScaleRevision: Int = 0,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppPrefs provides prefs) {
        com.polli.ui.theme.PolliTheme(prefs = prefs, uiScaleRevision = uiScaleRevision, content = content)
    }
}
