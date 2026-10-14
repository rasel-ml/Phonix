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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FragmentFactoryImpl @Inject constructor() : FragmentFactory {
    override fun getDialpadFragment() = DialpadFragment()
    override fun getSettingsFragment() = SettingsFragment()
    override fun getContactsFragment() = ContactsFragment()
    override fun getCallItemsFragment() = CallItemsFragment()
    override fun getPermissionFragment() = PermissionFragment()
    override fun getRecentMenuFragment() = RecentMenuFragment()
    override fun getContactsSuggestionsFragment() = ContactsSuggestionsFragment()
    override fun getDialerFragment(text: String?) = DialerFragment.newInstance(text)
    override fun getRecentFragment(recentId: Long) = RecentFragment.newInstance(recentId)
    override fun getBriefContactMenuFragment() = BriefContactMenuFragment()
    override fun getAccountsFragment(contactId: Long?) =
        AccountsFragment.newInstance(contactId)

    override fun getBriefContactFragment(contactId: Long) =
        BriefContactFragment.newInstance(contactId)

    override fun getPhonesFragment(contactId: Long?) =
        PhonesFragment.newInstance(contactId)

    override fun getPromptFragment(
        title: String,
        subtitle: String,
        isActivated: Boolean
    ): PromptFragment =
        PromptFragment.newInstance(title, subtitle, isActivated)

    override fun getRecentsHistoryFragment(filter: String?) =
        RecentsHistoryFragment.newInstance(filter)

    override fun getRecentsFragment(filter: String?, isGrouped: Boolean?) =
        RecentsFragment.newInstance(filter, isGrouped)

    override fun getChoicesFragment(
        @StringRes titleRes: Int,
        @StringRes subtitleRes: Int?,
        choices: List<String>,
        selectedChoiceIndex: Int?
    ) = BaseChoicesFragment.newInstance(titleRes, subtitleRes, choices, selectedChoiceIndex)

}