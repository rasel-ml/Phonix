package app.mrm.phonix.lib.interactor.rawcontacts

import app.mrm.phonix.lib.data.model.RawContactAccount
import app.mrm.phonix.lib.interactor.base.BaseInteractor
import kotlinx.coroutines.flow.Flow

interface RawContactsInteractor : BaseInteractor<RawContactsInteractor.Listener> {
    interface Listener

    fun getRawContacts(contactId: Long): Flow<List<RawContactAccount>>
}