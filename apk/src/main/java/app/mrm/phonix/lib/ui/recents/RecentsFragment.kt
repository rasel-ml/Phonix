package app.mrm.phonix.lib.ui.recents

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import app.mrm.phonix.lib.adapter.RecentsAdapter
import app.mrm.phonix.lib.data.model.RecentAccount
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.prompt.PromptsInteractor
import app.mrm.phonix.lib.ui.list.ListFragment
import app.mrm.phonix.lib.ui.recentshistory.RecentsHistoryViewState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class RecentsFragment @Inject constructor() : ListFragment<RecentAccount, RecentsViewState>() {
    @Inject override lateinit var adapter: RecentsAdapter
    override val viewState: RecentsViewState by activityViewModels()

    private val recentsHistoryViewState: RecentsHistoryViewState by activityViewModels()

    @Inject lateinit var prompts: PromptsInteractor
    @Inject lateinit var preferences: PreferencesInteractor


    override fun onSetup() {
        super.onSetup()

        adapter.groupSimilar = args.getBoolean(ARG_IS_GROUPED, preferences.isGroupRecents)

        viewState.showRecentEvent.observe(this@RecentsFragment) { ev ->
            ev.ifNew?.let {
                if (it.groupCount > 1) {
                    prompts.showFragment(
                        fragmentFactory.getRecentsHistoryFragment().apply {
                            arguments = arguments ?: Bundle()
                            requireArguments().putBoolean(ARG_OBSERVE, false)
                        })
                    recentsHistoryViewState.onItemsChanged(it.groupAccounts)
                } else {
                    prompts.showFragment(fragmentFactory.getRecentFragment(it.id))
                }
            }
        }
    }

    companion object {
        private const val ARG_IS_GROUPED = "is_grouped"

        fun newInstance(filter: String? = null, isGrouped: Boolean? = null) =
            RecentsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILTER, filter)
                    isGrouped?.let { putBoolean(ARG_IS_GROUPED, it) }
                }
            }
    }
}