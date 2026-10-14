package app.mrm.phonix.lib.ui.settings

import androidx.fragment.app.viewModels
import app.mrm.phonix.R
import app.mrm.phonix.lib.interactor.dialog.DialogsInteractor
import app.mrm.phonix.lib.ui.base.menu.BaseMenuFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class SettingsFragment @Inject constructor() : BaseMenuFragment() {
    override val viewState: SettingsViewState by viewModels()

    @Inject lateinit var dialogs: DialogsInteractor


    override fun onSetup() {
        super.onSetup()
        viewState.apply {
            askForColorEvent.observe(this@SettingsFragment) { ev ->
                ev.ifNew?.let { dialogs.askForColor(it, viewState::onColorResponse) }
            }

            askForThemeModeEvent.observe(this@SettingsFragment) {
                it.ifNew?.let {
                    dialogs.askForThemeMode {
                        viewState.onThemeModeResponse(it)
                        true
                    }
                }
            }

            askForAnimationsEvent.observe(this@SettingsFragment) {
                it.ifNew?.let { isActivated ->
                    dialogs.askForBoolean(
                        R.string.hint_animations,
                        isActivated
                    ) {
                        viewState.onAnimationsResponse(it)
                        true
                    }
                }
            }
        }
    }
}