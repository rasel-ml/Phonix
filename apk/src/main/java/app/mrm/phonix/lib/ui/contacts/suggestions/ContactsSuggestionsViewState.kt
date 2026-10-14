package app.mrm.phonix.lib.ui.contacts.suggestions

import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.data.repository.contacts.ContactsRepository
import app.mrm.phonix.lib.ui.contacts.ContactsViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ContactsSuggestionsViewState @Inject constructor(
    permissions: PermissionsInteractor,
    contactsRepository: ContactsRepository
) : ContactsViewState(permissions,contactsRepository)