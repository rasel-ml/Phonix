package app.mrm.phonix.lib.ui.recent.menu

import android.view.MenuItem
import androidx.fragment.app.activityViewModels
import app.mrm.phonix.R
import app.mrm.phonix.lib.interactor.dialog.DialogsInteractor
import app.mrm.phonix.lib.interactor.prompt.PromptsInteractor
import app.mrm.phonix.lib.ui.base.menu.BaseMenuFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecentMenuFragment @Inject constructor() : BaseMenuFragment() {
    override val viewState: RecentMenuViewState by activityViewModels()

    @Inject
    lateinit var prompts: PromptsInteractor

    @Inject
    lateinit var dialogs: DialogsInteractor


    override fun onSetup() {
        super.onSetup()
        viewState.apply {
            isBlocked.observe(this@RecentMenuFragment) {
                changeItemVisibility(R.id.menu_recent_block, !it)
                changeItemVisibility(R.id.menu_recent_unblock, it)
            }

            showHistoryEvent.observe(this@RecentMenuFragment) {
                it.ifNew?.let {
                    prompts.showFragment(fragmentFactory.getRecentsHistoryFragment(viewState.recentNumber.value))
                }
            }

            confirmDeleteEvent.observe(this@RecentMenuFragment) {
                it.ifNew?.let {
                    dialogs.askForValidation(R.string.explain_delete_contact) { bl ->
                        if (bl) viewState.onDelete()
                    }
                }
            }
        }
    }
}