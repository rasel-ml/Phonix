package app.mrm.phonix.lib.ui.phones

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import app.mrm.phonix.lib.adapter.PhonesAdapter
import app.mrm.phonix.lib.interactor.telecom.CallStarter
import app.mrm.phonix.lib.interactor.telecom.TelecomInteractor
import app.mrm.phonix.lib.data.model.PhoneAccount
import app.mrm.phonix.lib.ui.briefcontact.BriefContactFragment.Companion.ARG_CONTACT_ID
import app.mrm.phonix.lib.ui.list.ListFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PhonesFragment @Inject constructor() : ListFragment<PhoneAccount, PhonesViewState>() {
    override val viewState: PhonesViewState by activityViewModels()

    @Inject override lateinit var adapter: PhonesAdapter
    @Inject lateinit var telecomInteractor: TelecomInteractor
    @Inject lateinit var callStarter: CallStarter


    override fun onSetup() {
        viewState.onContactId(args.getLong(ARG_CONTACT_ID))
        super.onSetup()
        viewState.callEvent.observe(this@PhonesFragment) {
            it.ifNew?.let(callStarter::call)
        }
        binding.itemsScrollView.fastScroller.isVisible = false
    }

    companion object {
        fun newInstance(contactId: Long? = null) = PhonesFragment().apply {
            arguments = Bundle().apply {
                contactId?.let { putLong(ARG_CONTACT_ID, it) }
            }
        }
    }
}