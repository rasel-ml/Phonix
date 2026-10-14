package app.mrm.phonix.lib.ui.contacts

import androidx.fragment.app.activityViewModels
import app.mrm.phonix.lib.adapter.ContactsAdapter
import app.mrm.phonix.lib.interactor.prompt.PromptsInteractor
import app.mrm.phonix.lib.data.model.ContactAccount
import app.mrm.phonix.lib.ui.list.ListFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class ContactsFragment @Inject constructor() :
    ListFragment<ContactAccount, ContactsViewState>() {
    override val viewState: ContactsViewState by activityViewModels()

    @Inject lateinit var prompts: PromptsInteractor
    @Inject override lateinit var adapter: ContactsAdapter


    override fun onSetup() {
        super.onSetup()

        viewState.showContactEvent.observe(this@ContactsFragment) { ev ->
            ev.ifNew?.let {
                prompts.showFragment(fragmentFactory.getBriefContactFragment(it))
            }
        }
    }
}