package app.mrm.phonix.lib.ui.recents

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import app.mrm.phonix.lib.adapter.RecentsAdapter
import android.view.LayoutInflater
import androidx.core.view.isVisible
import app.mrm.phonix.R
import app.mrm.phonix.databinding.RecentsFilterChipBinding
import app.mrm.phonix.lib.data.model.RecentAccount
import app.mrm.phonix.lib.data.model.RecentsFilter
import app.mrm.phonix.lib.interactor.telecom.CallStarter
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
    @Inject lateinit var callStarter: CallStarter

    /** The filter chips only make sense on the main recents list, not on a number's history. */
    protected open val showFilterChips = true

    private val chipIds = mutableMapOf<RecentsFilter, Int>()


    override fun onSetup() {
        super.onSetup()

        adapter.groupSimilar = args.getBoolean(ARG_IS_GROUPED, preferences.isGroupRecents)

        adapter.isQuickCall = preferences.isQuickCallInRecents

        viewState.isQuickCall.observe(this@RecentsFragment) { adapter.isQuickCall = it }

        viewState.callEvent.observe(this@RecentsFragment) { ev ->
            ev.ifNew?.let(callStarter::call)
        }

        if (showFilterChips) setupFilterChips()

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

    private fun setupFilterChips() {
        binding.itemsFilterScroll.isVisible = true
        val group = binding.itemsFilterChips

        RecentsFilter.values().forEach { filter ->
            val chip = RecentsFilterChipBinding.inflate(LayoutInflater.from(requireContext()), group, false).root
            chip.id = android.view.View.generateViewId()
            chip.setText(filter.titleRes)
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) viewState.onFilterSelected(filter)
            }
            chipIds[filter] = chip.id
            group.addView(chip)
        }

        // reflects the (view model kept) selection, All by default
        viewState.selectedFilter.observe(this@RecentsFragment) { filter ->
            chipIds[filter]?.let { if (group.checkedChipId != it) group.check(it) }
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