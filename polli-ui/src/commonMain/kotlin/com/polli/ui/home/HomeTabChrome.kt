package com.polli.ui.home

fun formatHomeTabUnreadCount(count: Int): String? =
    when {
        count <= 0 -> null
        count > 99 -> "99+"
        else -> count.toString()
    }

fun totalUnreadMessages(items: List<com.polli.domain.model.InboxItem>): Int =
    items.sumOf { it.unreadCount.coerceAtLeast(0) }
