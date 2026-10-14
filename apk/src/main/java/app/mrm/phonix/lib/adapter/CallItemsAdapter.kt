package app.mrm.phonix.lib.adapter

import android.net.Uri
import android.telecom.Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE
import app.mrm.phonix.R
import app.mrm.phonix.lib.data.model.Call
import app.mrm.phonix.lib.data.model.ListData
import app.mrm.phonix.lib.di.module.IoScope
import app.mrm.phonix.lib.di.module.MainScope
import app.mrm.phonix.lib.interactor.animation.AnimationsInteractor
import app.mrm.phonix.lib.interactor.phoneaccounts.PhonesInteractor
import app.mrm.phonix.lib.ui.widgets.listitemholder.ListItemHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class CallItemsAdapter @Inject constructor(
    animationsInteractor: AnimationsInteractor,
    @IoScope private val ioScope: CoroutineScope,
    private val phonesInteractor: PhonesInteractor,
    @MainScope private val mainScope: CoroutineScope,
) : ListAdapter<Call>(animationsInteractor) {
    override fun onBindListItem(listItemHolder: ListItemHolder, item: Call, position: Int) {
        listItemHolder.apply {
            ioScope.launch {
                val account = phonesInteractor.lookupAccount(item.number)

                mainScope.launch {
                    account?.photoUri?.let {
                        setImageUri(Uri.parse(it))
                    } ?: run {
                        setImageResource(R.drawable.person)
                    }

                    account?.displayString?.let {
                        titleText = it
                        captionText = item.number
                    } ?: run {
                        titleText = item.number
                    }
                }
            }

            isLeftButtonVisible = true
            isRightButtonVisible = true
            isRightButtonEnabled = true
            isLeftButtonEnabled = item.isCapable(CAPABILITY_SEPARATE_FROM_CONFERENCE)

            setLeftButtonIcon(R.drawable.call_split)
            setRightButtonIcon(R.drawable.call_end)
            setRightButtonIconTint(R.color.negative)
        }
    }

    override fun convertDataToListData(items: List<Call>) = ListData(items)
}