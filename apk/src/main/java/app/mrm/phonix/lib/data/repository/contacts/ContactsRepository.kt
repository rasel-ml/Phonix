package app.mrm.phonix.lib.data.repository.contacts

import app.mrm.phonix.lib.data.model.ContactAccount
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    fun getContacts(filter: String? = null): Flow<List<ContactAccount>>
    fun getContact(contactId: Long): Flow<ContactAccount?>
}