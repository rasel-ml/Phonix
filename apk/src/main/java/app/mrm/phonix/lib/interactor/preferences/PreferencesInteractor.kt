package app.mrm.phonix.lib.interactor.preferences

import app.mrm.phonix.R
import app.mrm.phonix.lib.interactor.base.BaseInteractor
import app.mrm.phonix.lib.interactor.theme.ThemesInteractor.ThemeMode

interface PreferencesInteractor : BaseInteractor<PreferencesInteractor.Listener> {
    interface Listener

    var isAnimations: Boolean
    var isGroupRecents: Boolean
    var isDialpadTones: Boolean
    var isDialpadVibrate: Boolean

    var defaultPage: Page
    var themeMode: ThemeMode
    var incomingCallMode: IncomingCallMode

    fun setDefaultValues()

    companion object {
        enum class Page(val key: String, val index: Int, val titleRes: Int) {
            CONTACTS("contacts", 0, R.string.contacts),
            RECENTS("recents", 1, R.string.recents);

            companion object {
                fun fromKey(key: String?) =
                    values().associateBy(Page::key).getOrDefault(key ?: "", CONTACTS)
            }
        }

        enum class IncomingCallMode(val key: String, val index: Int, val titleRes: Int) {
            POP_UP("popup_notification", 0, R.string.pop_up_notification),
            FULL_SCREEN("full_screen", 1, R.string.full_screen);

            companion object {
                fun fromKey(key: String?) =
                    IncomingCallMode.values().associateBy(IncomingCallMode::key)
                        .getOrDefault(key ?: "", FULL_SCREEN)
            }
        }
    }
}