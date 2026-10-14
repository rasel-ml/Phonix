package app.mrm.phonix.lib.interactor.theme

import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import app.mrm.phonix.lib.interactor.theme.ThemesInteractor.ThemeMode
import app.mrm.phonix.lib.util.baseobservable.BaseObservable
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemesInteractorImpl @Inject constructor(
    private val uiManager: UiModeManager,
    @ApplicationContext private val context: Context,
) : BaseObservable<ThemesInteractor.Listener>(), ThemesInteractor {

    override fun applyThemeMode(themeMode: ThemeMode) {
        if (themeMode == ThemeMode.DYNAMIC) {
            DynamicColors.applyToActivitiesIfAvailable(context as Application)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themeMode != ThemeMode.SYSTEM) {
                uiManager.setApplicationNightMode(themeMode.mode)
            } else {
                AppCompatDelegate.setDefaultNightMode(themeMode.mode)
            }
        }
    }
}