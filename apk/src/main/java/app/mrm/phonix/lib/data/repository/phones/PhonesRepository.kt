package app.mrm.phonix.lib.data.repository.phones

import app.mrm.phonix.lib.data.model.PhoneAccount
import kotlinx.coroutines.flow.Flow

interface PhonesRepository {
    fun getPhones(contactId: Long? = null, filter: String? = null): Flow<List<PhoneAccount>>
}