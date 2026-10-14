package app.mrm.phonix.lib.interactor.contacts

import app.mrm.phonix.lib.data.model.ContactAccount
import app.mrm.phonix.lib.interactor.base.BaseInteractor
import kotlinx.coroutines.flow.Flow

interface ContactsInteractor : BaseInteractor<ContactsInteractor.Listener> {
    interface Listener

    fun deleteContact(contactId: Long)
    fun getContact(contactId: Long): Flow<ContactAccount?>
    fun getContacts(): Flow<List<ContactAccount>>
    fun toggleContactFavorite(contactId: Long, isFavorite: Boolean)
    suspend fun blockContact(contactId: Long)
    suspend fun unblockContact(contactId: Long)
    suspend fun getIsContactBlocked(contactId: Long): Boolean
}