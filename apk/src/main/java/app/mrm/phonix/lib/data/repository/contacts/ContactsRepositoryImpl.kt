package app.mrm.phonix.lib.data.repository.contacts

import app.mrm.phonix.lib.data.model.ContactAccount
import app.mrm.phonix.lib.di.factory.contentresolver.ContentResolverFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val contentResolverFactory: ContentResolverFactory
) : ContactsRepository {
    override fun getContacts(filter: String?): Flow<List<ContactAccount>> =
        contentResolverFactory.getContactsContentResolver().apply {
            this.filter = filter
        }.getItemsFlow()

    override fun getContact(contactId: Long): Flow<ContactAccount?> = flow {
        contentResolverFactory.getContactsContentResolver(contactId).getItemsFlow().collect {
            emit(it.getOrNull(0))
        }
    }
}