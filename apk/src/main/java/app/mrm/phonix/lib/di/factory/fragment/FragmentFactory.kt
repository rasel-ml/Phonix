package app.mrm.phonix.lib.di.factory.fragment

import androidx.annotation.StringRes
import app.mrm.phonix.lib.ui.accounts.AccountsFragment
import app.mrm.phonix.lib.ui.base.BaseChoicesFragment
import app.mrm.phonix.lib.ui.briefcontact.BriefContactFragment
import app.mrm.phonix.lib.ui.briefcontact.menu.BriefContactMenuFragment
import app.mrm.phonix.lib.ui.callitems.CallItemsFragment
import app.mrm.phonix.lib.ui.contacts.ContactsFragment
import app.mrm.phonix.lib.ui.contacts.suggestions.ContactsSuggestionsFragment
import app.mrm.phonix.lib.ui.dialer.DialerFragment
import app.mrm.phonix.lib.ui.dialpad.DialpadFragment
import app.mrm.phonix.lib.ui.permission.PermissionFragment
import app.mrm.phonix.lib.ui.phones.PhonesFragment
import app.mrm.phonix.lib.ui.prompt.PromptFragment
import app.mrm.phonix.lib.ui.recent.RecentFragment
import app.mrm.phonix.lib.ui.recent.menu.RecentMenuFragment
import app.mrm.phonix.lib.ui.recents.RecentsFragment
import app.mrm.phonix.lib.ui.recentshistory.RecentsHistoryFragment
import app.mrm.phonix.lib.ui.settings.SettingsFragment

interface FragmentFactory {
    fun getDialpadFragment(): DialpadFragment
    fun getSettingsFragment(): SettingsFragment
    fun getContactsFragment(): ContactsFragment
    fun getCallItemsFragment(): CallItemsFragment
    fun getRecentMenuFragment(): RecentMenuFragment
    fun getPermissionFragment(): PermissionFragment
    fun getRecentFragment(recentId: Long): RecentFragment
    fun getBriefContactMenuFragment(): BriefContactMenuFragment
    fun getDialerFragment(text: String? = null): DialerFragment
    fun getPhonesFragment(contactId: Long? = null): PhonesFragment
    fun getContactsSuggestionsFragment(): ContactsSuggestionsFragment
    fun getBriefContactFragment(contactId: Long): BriefContactFragment
    fun getAccountsFragment(contactId: Long? = null): AccountsFragment
    fun getPromptFragment(title: String, subtitle: String, isActivated: Boolean): PromptFragment
    fun getRecentsHistoryFragment(filter: String? = null): RecentsHistoryFragment
    fun getRecentsFragment(filter: String? = null, isGrouped: Boolean? = null): RecentsFragment
    fun getChoicesFragment(
        @StringRes titleRes: Int,
        @StringRes subtitleRes: Int?,
        choices: List<String>,
        selectedChoiceIndex: Int? = null
    ): BaseChoicesFragment
}