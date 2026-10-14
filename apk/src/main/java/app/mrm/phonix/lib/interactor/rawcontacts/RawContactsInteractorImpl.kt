package app.mrm.phonix.lib.interactor.rawcontacts

import app.mrm.phonix.lib.data.repository.rawcontacts.RawContactsRepository
import app.mrm.phonix.lib.interactor.base.BaseInteractorImpl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RawContactsInteractorImpl @Inject constructor(
    private val rawContactsRepository: RawContactsRepository
) :
    BaseInteractorImpl<RawContactsInteractor.Listener>(),
    RawContactsInteractor {

    override fun getRawContacts(contactId: Long) = rawContactsRepository.getRawContacts(contactId)
}