package app.mrm.phonix.lib.ui.recentshistory

import android.content.ClipboardManager
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.data.repository.recents.RecentsRepository
import app.mrm.phonix.lib.ui.recents.RecentsViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RecentsHistoryViewState @Inject constructor(
    permissions: PermissionsInteractor,
    clipboardManager: ClipboardManager,
    recentsRepository: RecentsRepository
) : RecentsViewState(permissions, clipboardManager, recentsRepository) {
}