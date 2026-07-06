package com.polli.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Layout constants — ported from polli/src/design_system/spacing.rs */
object LabDimens {
    val AvatarSize = 50.dp
    val HomeBarPadding = 14.dp
    val StoriesRowPadding = 12.dp // trailing / end inset for the stories row
    val StoriesRowPaddingStart = 10.dp // HOME_BAR - 4 (was −6; +2px vs prior)
    val HomeBarHeight = 42.dp
    val HomeProfileSize = 50.dp
    val HomeProfileGap = 14.dp
    val HomePillInsetBeforeSearch = 14.dp
    val HomeSearchGlyphSize = 19.dp
    val HomeSearchGapAfterGlyph = 8.dp
    val HomePillActionSize = 32.dp
    val HomeSearchPlusGlyphSize = 18.dp
    val HomeBarVerticalPad = 10.dp
    /** Standard stroke icon weight — chevrons, arrows, etc. */
    val IconStrokeWidth = 1.6.dp
    val HomeSearchPanelExpandedHeight = 320.dp // fallback when layout not measured
    val HomeSearchPanelIconSize = 28.dp
    val HomeSearchPanelTopFade = 24.dp
    val HomeSearchFavoriteColumnWidth = 148.dp
    val HomeSearchCreateCardWidth = 72.dp
    val HomeSearchExpandDragDistance = 200.dp
    val HomeSearchExpandSnapThreshold = 0.32f

    val TabButtonHeight = 34.dp
    val TabButtonHPadding = 15.dp
    val TabMailAvatarSize = 20.dp
    val TabMailAvatarOverlap = 6.dp
    val TabButtonUnselectedHeight = 30.dp
    val TabButtonUnselectedHPadding = 13.dp
    val TabButtonUnselectedCorner = 15.dp
    val TabGap = 8.dp
    val ChatHeaderTabGap = 12.dp
    val TabSectionGap = 14.dp
    val TabContentTopGap = 14.dp
    val TabContentBottomPad = 24.dp

    val StoryRingOuter = 60.dp
    val StoryRingInner = 48.dp
    val StoryRingStroke = 2.dp
    val StoryRingGap = 4.dp
    val StoryRingSpacing = 10.dp
    /** Visual gap between home chrome blocks (header ↔ stories ↔ tabs). */
    val HomeChromeGap = 16.dp
    val StoryRowVerticalPadTop = 10.dp
    val StoryRowVerticalPadBottom = 10.dp
    /** @deprecated Use [HomeChromeGap] — stories row no longer uses hairline dividers. */
    val StoryRowDividerGap = 16.dp
    val StoryProgressHeight = 2.dp

    /** Hairline stroke on inputs, composer chrome, frosted panel borders. */
    val ShellBorderWidth = 0.33.dp
    /** Full-width divider lines ([ShellDivider]). */
    val ShellDividerWidth = 1.dp
    val ListRowPadding = 0.dp
    val GroupNameNotifGap = 6.dp
    val InboxPreviewRowMinHeight = 22.dp
    val InboxTitleRowMinHeight = 20.dp
    val UnreadBadgeMinSize = 22.dp
    val UnreadBadgeHPadding = 7.dp

    val ChatAvatarSize = 40.dp
    val ChatAvatarGap = 5.dp
    val ChatBubbleRadius = 16.dp
    val ChatBubbleTailRadius = 4.dp
    val ChatBubbleInsetH = 11.dp
    val ChatBubblePaddingV = 6.dp
    val ChatBubbleMetaRowPaddingV = 1.dp
    val ChatBubbleMetaRowMarginTop = (-2).dp
    val ChatBubbleOutgoingShellBottom = 0.dp
    val ChatBubbleIncomingBottomExtra = 1.dp
    val ChatBubbleStackedIncomingTopExtra = 2.dp
    val ChatBubbleTextOnlyExtraTop = 2.dp
    val ChatQuoteAccentWidth = 2.8.dp
    val ChatQuoteBubblePadH = 6.dp
    val ChatQuoteMarginBottom = 6.dp
    val ChatReactionRowTop = 5.dp
    val ChatRowPaddingH = 8.dp
    val ChatRowOutgoingExtraStart = 5.dp
    /** Trailing gutter beside incoming bubbles — ~⅓ tighter than avatar column reserve. */
    val ChatRowIncomingRight = 30.dp
    val ChatRowTop = 8.dp
    val ChatRowTopCollapsed = 2.dp
    val ChatBubbleMaxWidthFraction = 0.85f
    val ChatBubbleImageMinWidth = 180.dp
    val ChatBubbleImageMinHeight = 120.dp
    val ChatBubbleImageMaxHeight = 280.dp
    val ChatIncomingGroupAvatarOffset = ChatAvatarSize + ChatAvatarGap
    val ChatIncomingGroupBubbleGap = 2.dp

    /** Scroll-to-bottom FAB + voice lock pill — matches header chrome circles at UI scale. */
    val ChatFloatingChromeSize = 40.dp

    val ChatComposerMinHeight = 47.dp
    val ChatComposerDockHPadding = 10.dp
    val ChatComposerDockBottomMin = 10.dp
    /** Slightly tighter than [ChatComposerDockBottomMin] when the IME is open (imePadding handles lift). */
    val ChatComposerKeyboardGap = 8.dp
    val ChatComposerDockClearanceExtra = 16.dp
    /** Gap between scroll-to-bottom FAB and the top of the composer dock. */
    val ChatScrollFabGapAboveComposer = 10.dp
    val ChatComposerPlusSize = 33.dp
    val ChatComposerSendSize = 33.dp
    val ChatComposerFieldGap = 8.dp

    val ScrollFadeBottom = 14.dp
    val ScrollFadeTop = 40.dp
    /** Extra reach of chat feed top edge gradient below the header zone. */
    val ChatFeedDayPillPadTop = 10.dp
    val ChatFeedDayPillPadBottom = 6.dp
    val ChatFeedDayPillHPadding = 10.dp
    val ChatFeedDayPillVPadding = 4.dp
    val ChatFeedDayPillFontSize = 11.sp
    val ChatFeedNewPillPadV = 16.dp
    val ChatFeedNewPillGap = 14.dp
    val ChatFeedNewPillHPadding = 14.dp
    val ChatFeedNewPillVPadding = 6.dp
    val ChatFeedNewPillFontSize = 12.sp
    val ChatFeedPillRadius = 16.dp
    val ChatFeedTopFadeExtend = 16.dp
    /** Extra reach of chat feed bottom edge gradient above the composer. */
    val ChatFeedBottomFadeExtend = 8.dp
    val ChatScrollFadeBottom = 80.dp
    val ChatScrollFadeBottomMid = 40.dp

    val ModalInset = 14.dp
    val ModalScreenInset = 8.dp
    val ModalRadius = 32.dp
    val ModalBottomFade = 14.dp
    val ModalTitleTopPad = 20.dp
    val ModalTitleDescGap = 10.dp

    val DetailBackButtonSize = 36.dp
    val ProfileCardWidth = 272.dp

    /** Title row + tab row below status bar (status bar added separately in ChatScreen). */
    val GroupHeaderClearance = 110.dp
}
