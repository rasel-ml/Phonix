package app.mrm.phonix.lib.ui.dialer

import android.Manifest
import android.content.ClipboardManager
import androidx.lifecycle.MutableLiveData
import app.mrm.phonix.lib.interactor.audio.AudiosInteractor
import app.mrm.phonix.lib.interactor.navigation.NavigationsInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.recents.RecentsInteractor
import app.mrm.phonix.lib.data.model.ContactAccount
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.ui.dialpad.DialpadViewState
import app.mrm.phonix.lib.util.DataLiveEvent
import app.mrm.phonix.lib.util.LiveEvent
import app.mrm.phonix.lib.util.MutableDataLiveEvent
import app.mrm.phonix.lib.util.MutableLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DialerViewState @Inject constructor(
    audios: AudiosInteractor,
    preferences: PreferencesInteractor,
    clipboardManager: ClipboardManager,
    permissions: PermissionsInteractor,
    private val recents: RecentsInteractor,
    private val navigations: NavigationsInteractor,
) :
    DialpadViewState(permissions, audios, preferences, clipboardManager) {

    override val requiredPermissions = listOf(Manifest.permission.CALL_PHONE)

    private val _isSuggestionsVisible = MutableLiveData(false)
    private val _isDeleteButtonVisible = MutableLiveData<Boolean>()
    private val _isAddContactButtonVisible = MutableLiveData<Boolean>()
    private val _callVoicemailEvent = MutableLiveEvent()
    private val _callNumberEvent = MutableDataLiveEvent<String>()

    val isSuggestionsVisible = _isSuggestionsVisible as MutableLiveData<Boolean>
    val isDeleteButtonVisible = _isDeleteButtonVisible as MutableLiveData<Boolean>
    val isAddContactButtonVisible = _isAddContactButtonVisible as MutableLiveData<Boolean>
    val callVoicemailEvent = _callVoicemailEvent as LiveEvent
    val callNumberEvent = _callNumberEvent as DataLiveEvent<String>


    override fun onLongKeyClick(char: Char) = when (char) {
        '0' -> {
            onTextChanged(text.value + "+")
            true
        }
        '1' -> {
            _callVoicemailEvent.call()
            true
        }
        else -> super.onLongKeyClick(char)
    }

    override fun onTextChanged(text: String) {
        isDeleteButtonVisible.value = text.isNotEmpty()
        isAddContactButtonVisible.value = text.isNotEmpty()
        if (text.isEmpty()) isSuggestionsVisible.value = false
        super.onTextChanged(text)
    }

    fun onDeleteClick() {
        _text.value?.dropLast(1)?.let(this::onTextChanged)
    }

    fun onLongDeleteClick(): Boolean {
        onTextChanged("")
        _text.value = ""
        return true
    }

    fun onCallClick() {
        if (text.value?.isEmpty() == true) {
            _text.value = recents.getLastOutgoingCall()
        } else {
            _text.value?.let(_callNumberEvent::call)
            onFinish()
        }
    }

    fun onAddContactClick() {
        _text.value?.let(navigations::addContact)
    }

    fun onSuggestionsChanged(contacts: List<ContactAccount>) {
        isSuggestionsVisible.value = contacts.isNotEmpty() && _text.value?.isNotEmpty() == true
    }
}