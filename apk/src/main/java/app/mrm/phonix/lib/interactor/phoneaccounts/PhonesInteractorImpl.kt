package app.mrm.phonix.lib.interactor.phoneaccounts

import app.mrm.phonix.lib.data.model.PhoneAccount
import app.mrm.phonix.lib.data.model.PhoneLookupAccount
import app.mrm.phonix.lib.di.factory.contentresolver.ContentResolverFactory
import app.mrm.phonix.lib.interactor.base.BaseInteractorImpl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhonesInteractorImpl @Inject constructor(
    private val contentResolverFactory: ContentResolverFactory,
) : BaseInteractorImpl<PhonesInteractor.Listener>(), PhonesInteractor {

    override suspend fun lookupAccount(number: String?): PhoneLookupAccount? =
        if (number == null || number.isEmpty()) {
            PhoneLookupAccount.PRIVATE
        } else try {
            contentResolverFactory.getPhoneLookupContentResolver(number).queryItems().getOrNull(0)
        } catch (e: Exception) {
            null
        }

    override suspend fun getContactAccounts(contactId: Long): List<PhoneAccount> =
        contentResolverFactory.getPhonesContentResolver(contactId).queryItems().toList()
}