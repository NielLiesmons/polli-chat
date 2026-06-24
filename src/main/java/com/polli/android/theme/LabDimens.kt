package com.polli.android.theme

import androidx.compose.ui.unit.dp

/** Layout constants — ported from polli/src/design_system/spacing.rs */
object LabDimens {
    val AvatarSize = 50.dp
    val HomeBarPadding = 14.dp
    val StoriesRowPadding = 12.dp // HOME_BAR - 2
    val HomeBarHeight = 42.dp
    val HomeProfileSize = 50.dp
    val HomeProfileGap = 14.dp
    val HomePillInsetBeforeSearch = 14.dp
    val HomeSearchGlyphSize = 16.dp
    val HomeSearchGapAfterGlyph = 8.dp
    val HomePillActionSize = 30.dp
    val HomeBarVerticalPad = 10.dp

    val TabButtonHeight = 34.dp
    val TabButtonHPadding = 15.dp
    val TabGap = 8.dp
    val TabSectionGap = 14.dp
    val TabContentTopGap = 7.dp
    val TabContentBottomPad = 24.dp

    val StoryRingOuter = 58.dp
    val StoryRingInner = 48.dp
    val StoryRingStroke = 2.dp
    val StoryRingGap = 3.dp
    val StoryRingSpacing = 14.dp
    val StoryRowVerticalPadTop = 10.dp
    val StoryRowVerticalPadBottom = 12.dp
    val StoryProgressHeight = 2.dp

    val ShellBorderWidth = 0.33.dp
    val ListRowPadding = 12.dp
    val GroupNameNotifGap = 6.dp
    val InboxPreviewRowMinHeight = 22.dp
    val InboxTitleRowMinHeight = 20.dp
    val UnreadBadgeMinSize = 22.dp
    val UnreadBadgeHPadding = 7.dp

    val ChatAvatarSize = 36.dp
    val ChatAvatarGap = 6.dp
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
    val ChatReactionRowTop = 6.dp
    val ChatRowPaddingH = 10.dp
    val ChatRowIncomingRight = 40.dp
    val ChatRowTop = 8.dp
    val ChatRowTopCollapsed = 2.dp
    val ChatBubbleMaxWidthFraction = 0.85f
    val ChatIncomingGroupAvatarOffset = 42.dp // avatar + gap
    val ChatIncomingGroupBubbleGap = 2.dp

    val ChatComposerMinHeight = 47.dp
    val ChatComposerDockHPadding = 10.dp
    val ChatComposerDockBottomMin = 10.dp
    val ChatComposerDockClearanceExtra = 16.dp
    /** Gap between scroll-to-bottom FAB and the top of the composer dock. */
    val ChatScrollFabGapAboveComposer = 10.dp
    val ChatComposerPlusSize = 33.dp
    val ChatComposerSendSize = 33.dp
    val ChatComposerFieldGap = 8.dp

    val ScrollFadeBottom = 14.dp
    val ScrollFadeTop = 40.dp
    /** Extra reach of chat feed top edge gradient below the header zone (+12dp vs prior). */
    val ChatFeedTopFadeExtend = 24.dp
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
