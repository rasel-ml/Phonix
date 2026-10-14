package app.mrm.phonix.lib.ui.phones

import android.Manifest.permission.*
import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.mrm.phonix.R
import app.mrm.phonix.lib.data.model.PhoneAccount
import app.mrm.phonix.lib.data.repository.phones.PhonesRepository
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.ui.list.ListViewState
import app.mrm.phonix.lib.util.DataLiveEvent
import app.mrm.phonix.lib.util.MutableDataLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class PhonesViewState @Inject constructor(
    private val permissions: PermissionsInteractor,
    private val phonesRepository: PhonesRepository,
    private val clipboardManager: ClipboardManager
) :
    ListViewState<PhoneAccount>(permissions) {

    override val noResultsIconRes = R.drawable.call
    override val noResultsTextRes = R.string.error_no_results_phones
    override val requiredPermissions = listOf(READ_CONTACTS)

    private val _contactId = MutableLiveData(0L)
    private val _callEvent = MutableDataLiveEvent<String>()

    val contactId = _contactId as LiveData<Long>
    val callEvent = _callEvent as DataLiveEvent<String>


    override fun getItemsFlow(filter: String?): Flow<List<PhoneAccount>> =
        phonesRepository.getPhones(if (contactId.value == 0L) null else contactId.value, filter)

    override fun onItemRightClick(item: PhoneAccount) {
        super.onItemRightClick(item)
        permissions.runWithPermissions(arrayOf(READ_PHONE_STATE, CALL_PHONE), {
            _callEvent.call(item.number)
        }, {
            onError(R.string.error_no_permissions_make_call)
        })
    }

    override fun onItemLongClick(item: PhoneAccount) {
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText("Copied number", item.number)
        )
        onMessage(R.string.number_copied_to_clipboard)
    }

    fun onContactId(contactId: Long) {
        _contactId.value = contactId
        updateItemsFlow()
    }
}
