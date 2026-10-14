package app.mrm.phonix.lib.adapter

import app.mrm.phonix.R
import app.mrm.phonix.lib.interactor.animation.AnimationsInteractor
import app.mrm.phonix.lib.interactor.string.StringsInteractor
import app.mrm.phonix.lib.data.model.ListData
import app.mrm.phonix.lib.data.model.RawContactAccount
import app.mrm.phonix.lib.ui.widgets.listitemholder.ListItemHolder
import javax.inject.Inject

class AccountsAdapter @Inject constructor(
    animations: AnimationsInteractor,
    private val strings: StringsInteractor
) : ListAdapter<RawContactAccount>(animations) {
    override fun onBindListItem(
        listItemHolder: ListItemHolder,
        item: RawContactAccount,
        position: Int
    ) {
        listItemHolder.apply {
            captionText = null
            isImageVisible = false
            isRightButtonVisible = true
            titleText = strings.getString(item.type.titleStringRes)

            if (item.type == RawContactAccount.RawContactType.WHATSAPP) {
                setRightButtonIcon(R.drawable.whatsapp)
            }
        }
    }

    override fun convertDataToListData(items: List<RawContactAccount>) =
        ListData.fromRawContacts(items, accounts = true, withHeader = false)
}