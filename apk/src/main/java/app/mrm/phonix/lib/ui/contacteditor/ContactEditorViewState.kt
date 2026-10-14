package app.mrm.phonix.lib.ui.contacteditor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.mrm.phonix.R
import app.mrm.phonix.lib.ui.base.BaseViewState
import app.mrm.phonix.lib.util.LiveEvent
import app.mrm.phonix.lib.util.MutableLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactEditorViewState @Inject constructor(
    private val repository: ContactEditorRepository
) : BaseViewState() {
    private var isStarted = false

    /** signature of the draft as first shown, to detect unsaved changes */
    var initialSignature: String? = null

    private val _draft = MutableLiveData<ContactDraft>()
    private val _photo = MutableLiveData<Bitmap?>()
    private val _isSaving = MutableLiveData(false)
    private val _savedEvent = MutableLiveEvent()
    private val _accounts = MutableLiveData<List<AccountOption>>(emptyList())

    /** emitted once the contact has been read (or an empty draft was created) */
    val draft = _draft as LiveData<ContactDraft>
    val photo = _photo as LiveData<Bitmap?>
    val isSaving = _isSaving as LiveData<Boolean>
    val savedEvent = _savedEvent as LiveEvent
    val accounts = _accounts as LiveData<List<AccountOption>>

    /** the draft being edited; the screen writes its field values into it before saving */
    val currentDraft: ContactDraft? get() = _draft.value


    fun onStart(contactId: Long?, prefilledNumber: String?) {
        if (isStarted) return
        isStarted = true

        viewModelScope.launch {
            if (contactId == null) {
                val draft = ContactDraft()
                if (!prefilledNumber.isNullOrBlank()) {
                    draft.entries(FieldKind.PHONE).add(
                        FieldEntry(mutableListOf(prefilledNumber), FieldKind.PHONE.defaultType)
                    )
                }
                val accounts = repository.getAccounts()
                draft.account = accounts.firstOrNull { it.type == "com.google" } ?: accounts.first()
                _accounts.value = accounts
                _draft.value = draft
            } else {
                val draft = repository.loadDraft(contactId)
                if (draft == null) {
                    onError(R.string.editor_error_not_found)
                    onFinish()
                } else {
                    _draft.value = draft
                }
            }
        }
    }

    fun onAccountSelected(account: AccountOption) {
        currentDraft?.account = account
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val bitmap = repository.decodePhoto(uri) ?: return@launch onError(R.string.editor_error_save)
            currentDraft?.apply {
                newPhoto = bitmap
                photoRemoved = false
            }
            _photo.value = bitmap
        }
    }

    fun onPhotoRemoved() {
        currentDraft?.apply {
            newPhoto = null
            photoRemoved = true
        }
        _photo.value = null
    }

    fun onSave() {
        val draft = currentDraft ?: return
        if (_isSaving.value == true) return
        if (draft.isEmpty()) {
            onError(R.string.editor_error_empty)
            return
        }

        _isSaving.value = true
        viewModelScope.launch {
            try {
                repository.save(draft)
                onMessage(R.string.editor_saved)
                _savedEvent.call()
            } catch (e: Exception) {
                _isSaving.value = false
                onError(R.string.editor_error_save)
            }
        }
    }

    fun onDelete() {
        val contactId = currentDraft?.contactId ?: return
        viewModelScope.launch {
            try {
                repository.deleteContact(contactId)
                onMessage(R.string.editor_deleted)
                _savedEvent.call()
            } catch (e: Exception) {
                onError(R.string.editor_error_save)
            }
        }
    }
}
