package app.mrm.phonix.ui.settings

import androidx.fragment.app.activityViewModels
import app.mrm.phonix.R
import app.mrm.phonix.lib.ui.recents.RecentsViewState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import app.mrm.phonix.lib.ui.settings.SettingsFragment as ChoolooSettingsFragment

@AndroidEntryPoint
class SettingsFragment @Inject constructor() : ChoolooSettingsFragment() {
    override val viewState: SettingsViewState by activityViewModels()
    private val recentsViewState: RecentsViewState by activityViewModels()

    override fun onSetup() {
        super.onSetup()

        viewState.apply {
            askForDefaultPageEvent.observe(this@SettingsFragment) {
                it.ifNew?.let {
                    dialogs.askForDefaultPage {
                        viewState.onDefaultPageResponse(it)
                        true
                    }
                }
            }

            askForDialpadTonesEvent.observe(this@SettingsFragment) {
                it.ifNew?.let { isActivated ->
                    dialogs.askForBoolean(
                        R.string.hint_dialpad_tones,
                        isActivated
                    ) {
                        viewState.onDialpadTones(it)
                        true
                    }
                }
            }

            askForDialpadVibrateEvent.observe(this@SettingsFragment) {
                it.ifNew?.let { isActivated ->
                    dialogs.askForBoolean(
                        R.string.hint_dialpad_vibrate,
                        isActivated
                    ) {
                        viewState.onDialpadVibrate(it)
                        true
                    }
                }
            }

            askForGroupRecentsEvent.observe(this@SettingsFragment) {
                it.ifNew?.let { isActivated ->
                    dialogs.askForBoolean(
                        R.string.hint_group_recents,
                        isActivated
                    ) {
                        viewState.onGroupRecents(it)
                        true
                    }
                }
            }

            askForQuickCallEvent.observe(this@SettingsFragment) {
                it.ifNew?.let { isActivated ->
                    dialogs.askForBoolean(
                        R.string.hint_quick_call_in_recents,
                        isActivated
                    ) {
                        viewState.onQuickCallInRecents(it)
                        recentsViewState.onQuickCallChanged(it)
                        true
                    }
                }
            }

            clearRecentsEvent.observe(this@SettingsFragment) {
                it.ifNew?.let {
                    dialogs.askForValidation(R.string.explain_clear_recents) { bl ->
                        if (bl) viewState.onClearRecents()
                    }
                }
            }

            askForIncomingCallModeEvent.observe(this@SettingsFragment) {
                it.ifNew?.let {
                    dialogs.askForIncomingCallMode {
                        viewState.onIncomingCallMode(it)
                        true
                    }
                }
            }
        }
    }
}