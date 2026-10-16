package app.mrm.phonix.lib.ui.settings

import app.mrm.phonix.R
import app.mrm.phonix.lib.interactor.color.ColorsInteractor
import app.mrm.phonix.lib.interactor.navigation.NavigationsInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.string.StringsInteractor
import app.mrm.phonix.lib.interactor.theme.ThemesInteractor
import app.mrm.phonix.lib.interactor.theme.ThemesInteractor.ThemeMode
import app.mrm.phonix.lib.ui.base.menu.BaseMenuViewState
import app.mrm.phonix.lib.util.DataLiveEvent
import app.mrm.phonix.lib.util.LiveEvent
import app.mrm.phonix.lib.util.MutableDataLiveEvent
import app.mrm.phonix.lib.util.MutableLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
open class SettingsViewState @Inject constructor(
    protected val themes: ThemesInteractor,
    protected val colors: ColorsInteractor,
    protected val strings: StringsInteractor,
    protected val navigations: NavigationsInteractor,
    protected val preferences: PreferencesInteractor
) :
    BaseMenuViewState() {
    override val menuResList = listOf(R.menu.menu_chooloo)

    private val _askForThemeModeEvent = MutableLiveEvent()
    private val _askForAnimationsEvent = MutableDataLiveEvent<Boolean>()
    private val _askForColorEvent = MutableDataLiveEvent<Int>()

    val askForThemeModeEvent = _askForThemeModeEvent as LiveEvent
    val askForColorEvent = _askForColorEvent as DataLiveEvent<Int>
    val askForAnimationsEvent = _askForAnimationsEvent as DataLiveEvent<Boolean>

    init {
        _title.value = strings.getString(R.string.settings)
    }

    override fun onMenuItemClick(itemId: Int) {
        when (itemId) {
            R.id.menu_chooloo_theme_mode -> _askForThemeModeEvent.call()
            R.id.menu_chooloo_animations -> _askForAnimationsEvent.call(preferences.isAnimations)
            else -> super.onMenuItemClick(itemId)
        }
    }

    fun onColorResponse(color: Int) {
//        preferences.accentTheme = when (color) {
//            colors.getColor(R.color.red_primary) -> RED
//            colors.getColor(R.color.primary) -> BLUE
//            colors.getColor(R.color.green_primary) -> GREEN
//            colors.getColor(R.color.purple_primary) -> PURPLE
//            else -> DEFAULT
//        }
        navigations.goToLauncherActivity()
    }

    fun onAnimationsResponse(response: Boolean) {
        preferences.isAnimations = response
    }

    fun onThemeModeResponse(response: ThemeMode) {
        preferences.themeMode = response
        themes.applyThemeMode(response)
        navigations.goToLauncherActivity()
    }
}