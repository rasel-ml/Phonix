package app.mrm.phonix.lib.data.repository.rawcontacts

import app.mrm.phonix.lib.data.model.RawContactAccount
import kotlinx.coroutines.flow.Flow

interface RawContactsRepository {
    fun getRawContacts(contactId: Long, filter: String? = null): Flow<List<RawContactAccount>>
}