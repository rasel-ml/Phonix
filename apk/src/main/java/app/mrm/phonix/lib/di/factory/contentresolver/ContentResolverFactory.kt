package app.mrm.phonix.lib.di.factory.contentresolver

import app.mrm.phonix.lib.data.contentresolver.*

interface ContentResolverFactory {
    fun getPhonesContentResolver(contactId: Long? = null): PhonesContentResolver
    fun getRecentsContentResolver(recentId: Long? = null): RecentsContentResolver
    fun getPhoneLookupContentResolver(number: String?): PhoneLookupContentResolver
    fun getRawContactsContentResolver(contactId: Long): RawContactsContentResolver
    fun getContactsContentResolver(contactId: Long? = null): ContactsContentResolver
}