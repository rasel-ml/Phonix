package app.mrm.phonix.lib.adapter

import android.net.Uri
import app.mrm.phonix.lib.data.model.ContactAccount
import app.mrm.phonix.lib.data.model.ListData
import app.mrm.phonix.lib.di.module.IoScope
import app.mrm.phonix.lib.di.module.MainScope
import app.mrm.phonix.lib.interactor.animation.AnimationsInteractor
import app.mrm.phonix.lib.interactor.phoneaccounts.PhonesInteractor
import app.mrm.phonix.lib.ui.widgets.listitemholder.ListItemHolder
import app.mrm.phonix.lib.util.initials
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject


open class ContactsAdapter @Inject constructor(
    animations: AnimationsInteractor,
    private val phones: PhonesInteractor,
    @IoScope private val ioScope: CoroutineScope,
    @MainScope private val mainScope: CoroutineScope
) : ListAdapter<ContactAccount>(animations) {
    private var _withFavs: Boolean = true
    private var _withHeaders: Boolean = true

    var withFavs: Boolean
        get() = _withFavs
        set(value) {
            _withFavs = value
        }

    var withHeaders: Boolean
        get() = _withHeaders
        set(value) {
            _withHeaders = value
        }


    override fun onBindListItem(
        listItemHolder: ListItemHolder,
        item: ContactAccount,
        position: Int
    ) {
        listItemHolder.apply {
            titleText = item.name
            imageInitials = item.name?.initials()

            ioScope.launch {
                val number = phones.getContactAccounts(item.id).firstOrNull()?.number
                mainScope.launch {
                    captionText = number
                }
            }

            setImageUri(if (item.photoUri != null) Uri.parse(item.photoUri) else null)
        }
    }

    override fun convertDataToListData(items: List<ContactAccount>) =
        if (_withHeaders) ListData.fromContacts(items, _withFavs) else ListData(items)
}