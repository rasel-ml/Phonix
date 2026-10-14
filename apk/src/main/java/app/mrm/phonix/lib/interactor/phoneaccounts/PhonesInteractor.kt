package app.mrm.phonix.lib.interactor.phoneaccounts

import app.mrm.phonix.lib.data.model.PhoneAccount
import app.mrm.phonix.lib.data.model.PhoneLookupAccount
import app.mrm.phonix.lib.interactor.base.BaseInteractor

interface PhonesInteractor : BaseInteractor<PhonesInteractor.Listener> {
    interface Listener

    suspend fun getContactAccounts(contactId: Long): List<PhoneAccount>
    suspend fun lookupAccount(number: String?): PhoneLookupAccount?
}