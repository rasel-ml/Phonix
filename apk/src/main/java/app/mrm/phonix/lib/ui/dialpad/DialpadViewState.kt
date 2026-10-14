package app.mrm.phonix.lib.ui.dialpad

import android.content.ClipboardManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.mrm.phonix.lib.interactor.audio.AudiosInteractor
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.ui.permissioned.PermissionedViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
open class DialpadViewState @Inject constructor(
    permissions: PermissionsInteractor,
    private val audios: AudiosInteractor,
    private val preferences: PreferencesInteractor,
    private val clipboardManager: ClipboardManager,
) :
    PermissionedViewState(permissions) {

    private val _char = MutableLiveData<Char>()
    protected val _text = MutableLiveData("")

    val char = _char as LiveData<Char>
    val text = _text as LiveData<String>

    open fun onTextChanged(text: String) {
        _text.value = text
    }

    open fun onCharClick(char: Char) {
        _char.value = char
        if (preferences.isDialpadTones) audios.playToneByChar(char)
        if (preferences.isDialpadVibrate) audios.vibrate(AudiosInteractor.SHORT_VIBRATE_LENGTH)
        onTextChanged((text.value ?: "") + char)
    }

    open fun onLongKeyClick(char: Char) = true

    open fun onTextPasted() {
        val item = clipboardManager.primaryClip?.getItemAt(0)
        val text = item?.text.toString().replace(Regex("[^+#*0-9]"), "")
        onTextChanged(text)
    }
}

