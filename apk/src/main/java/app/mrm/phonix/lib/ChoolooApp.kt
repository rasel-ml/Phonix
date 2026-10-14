package app.mrm.phonix.lib

import android.app.Application
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.theme.ThemesInteractor
import javax.inject.Inject

abstract class ChoolooApp : Application() {
    @Inject lateinit var themes: ThemesInteractor
    @Inject lateinit var preferences: PreferencesInteractor

    override fun onCreate() {
        super.onCreate()
        preferences.setDefaultValues()
        themes.applyThemeMode(preferences.themeMode)
    }
}