package app.mrm.phonix.ui.main

import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor.Companion.Page

/**
 * The order of the main tabs: the default page comes first, the other one after it.
 *
 * The order is decided once, the first time it is needed after the app process starts, and then
 * stays the same until the app is fully closed. So changing the default page in the settings
 * only reorders the tabs the next time the app is launched.
 */
object MainPages {
    private var _order: List<Page>? = null

    @Synchronized
    fun order(preferences: PreferencesInteractor): List<Page> =
        _order ?: preferences.defaultPage.let { first ->
            listOf(first) + Page.values().filter { it != first }
        }.also { _order = it }

    /** position of the page the app should open on, inside the tab order */
    fun initialIndex(preferences: PreferencesInteractor) =
        order(preferences).indexOf(preferences.defaultPage).coerceAtLeast(0)
}
