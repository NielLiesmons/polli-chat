package com.polli.domain.navigation

interface PolliNavigator {
    fun navigate(route: PolliRoute)
    fun back()
    fun openExternalUrl(url: String)
}
