package app.mrm.phonix.lib.ui.accounts

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import app.mrm.phonix.lib.adapter.AccountsAdapter
import app.mrm.phonix.lib.interactor.telecom.CallStarter
import app.mrm.phonix.lib.interactor.telecom.TelecomInteractor
import app.mrm.phonix.lib.data.model.RawContactAccount
import app.mrm.phonix.lib.ui.briefcontact.BriefContactFragment.Companion.ARG_CONTACT_ID
import app.mrm.phonix.lib.ui.list.ListFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountsFragment @Inject constructor() :
    ListFragment<RawContactAccount, AccountsViewState>() {

    override val viewState: AccountsViewState by activityViewModels()

    @Inject override lateinit var adapter: AccountsAdapter
    @Inject lateinit var telecomInteractor: TelecomInteractor
    @Inject lateinit var callStarter: CallStarter


    override fun onSetup() {
        viewState.onContactId(args.getLong(ARG_CONTACT_ID))
        super.onSetup()
        viewState.callEvent.observe(this@AccountsFragment) {
            it.ifNew?.let(callStarter::call)
        }
        binding.itemsScrollView.fastScroller.isVisible = false
    }

    companion object {
        fun newInstance(contactId: Long? = null) = AccountsFragment().apply {
            arguments = Bundle().apply {
                contactId?.let { putLong(ARG_CONTACT_ID, it) }
            }
        }
    }
}