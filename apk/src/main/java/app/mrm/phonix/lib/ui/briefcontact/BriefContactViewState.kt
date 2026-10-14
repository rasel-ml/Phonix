package app.mrm.phonix.lib.ui.briefcontact

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.mrm.phonix.R
import app.mrm.phonix.lib.data.model.ContactAccount
import app.mrm.phonix.lib.interactor.contacts.ContactsInteractor
import app.mrm.phonix.lib.interactor.navigation.NavigationsInteractor
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.interactor.phoneaccounts.PhonesInteractor
import app.mrm.phonix.lib.ui.base.BaseViewState
import app.mrm.phonix.lib.util.DataLiveEvent
import app.mrm.phonix.lib.util.LiveEvent
import app.mrm.phonix.lib.util.MutableDataLiveEvent
import app.mrm.phonix.lib.util.MutableLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BriefContactViewState @Inject constructor(
    private val phones: PhonesInteractor,
    private val contacts: ContactsInteractor,
    private val navigations: NavigationsInteractor,
) : BaseViewState() {
    private var firstNumber: String? = null
    private var contact: ContactAccount? = null

    private val _contactId = MutableLiveData<Long>()
    private val _contactImage = MutableLiveData<Uri>()
    private val _isFavorite = MutableLiveData<Boolean>()
    private val _contactName = MutableLiveData<String?>()
    private val _contactNumber = MutableLiveData<String?>()
    private val _showMoreEvent = MutableLiveEvent()
    private val _callEvent = MutableDataLiveEvent<String>()

    val contactId = _contactId as LiveData<Long>
    val contactImage = _contactImage as LiveData<Uri>
    val isFavorite = _isFavorite as LiveData<Boolean>
    val contactName = _contactName as LiveData<String?>
    val contactNumber = _contactNumber as LiveData<String?>
    val showMoreEvent = _showMoreEvent as LiveEvent
    val callEvent = _callEvent as DataLiveEvent<String>


    private fun withFirstNumber(callback: (String?) -> Unit) {
        if (firstNumber != null) {
            callback.invoke(firstNumber)
        } else {
            contact?.let {
                viewModelScope.launch {
                    firstNumber = phones.getContactAccounts(it.id).getOrNull(0)?.number
                    callback.invoke(firstNumber)
                }
            }
        }
    }

    fun onContactId(contactId: Long) {
        _contactId.value = contactId
        viewModelScope.launch {
            contacts.getContact(contactId).collect {
                contact = it
                _contactName.value = it?.name
                _isFavorite.value = it?.starred == true
                withFirstNumber { _contactNumber.value = it }
                it?.photoUri?.let { uri ->
                    _contactImage.value = Uri.parse(uri)
                }
            }
        }
    }

    fun onCallClick() {
        withFirstNumber {
            it?.let(_callEvent::call) ?: run {
                onError(R.string.error_no_number_to_call)
            }
        }
    }

    fun onSmsClick() {
        withFirstNumber { it?.let(navigations::sendSMS) }
    }

    fun onEditClick() {
        contactId.value?.let(navigations::editContact)
    }

    fun onMoreClick() {
        _showMoreEvent.call()
    }
}