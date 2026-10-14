package app.mrm.phonix.ui.main

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.string.StringsInteractor
import app.mrm.phonix.lib.ui.base.BaseViewState
import app.mrm.phonix.lib.util.DataLiveEvent
import app.mrm.phonix.lib.util.LiveEvent
import app.mrm.phonix.lib.util.MutableDataLiveEvent
import app.mrm.phonix.lib.util.MutableLiveEvent
import app.mrm.phonix.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class MainViewState @Inject constructor(
    private val strings: StringsInteractor,
    private val permissions: PermissionsInteractor,
    private val preferences: PreferencesInteractor,
) :
    BaseViewState() {

    private val _searchText = MutableLiveData<String?>()
    private val _searchHint = MutableLiveData<String?>()
    private val _currentPageIndex = MutableLiveData(0)
    private val _isSearching = MutableLiveData(false)
    private val _showMenuEvent = MutableLiveEvent()
    private val _showDialerEvent = MutableDataLiveEvent<String>()

    val searchText = _searchText as LiveData<String?>
    val searchHint = _searchHint as LiveData<String?>
    val currentPageIndex = _currentPageIndex as LiveData<Int>
    val isSearching = _isSearching as LiveData<Boolean>
    val showMenuEvent = _showMenuEvent as LiveEvent
    val showDialerEvent = _showDialerEvent as DataLiveEvent<String>


    override fun attach() {
        super.attach()
        permissions.checkDefaultDialer { requestNotificationPermission() }
        _currentPageIndex.value = preferences.defaultPage.index
        _searchHint.value = strings.getString(R.string.hint_search_items)
    }

    /** Android 13+: call notifications are silently dropped without this permission. */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.runWithPermissions(Manifest.permission.POST_NOTIFICATIONS) {}
        }
    }

    fun onMenuClick() {
        _showMenuEvent.call()
    }

    fun onDialpadFabClick() {
        _showDialerEvent.call("")
    }

    fun onViewIntent(intent: Intent) {
        try {
            val intentText = URLDecoder.decode(intent.dataString ?: "", "utf-8")
            _showDialerEvent.call(intentText.substringAfter("tel:").trim())
        } catch (e: Exception) {
            onError(R.string.error_couldnt_get_number_from_intent)
        }
    }

    fun onSearchFocusChange(isFocus: Boolean) {
        _isSearching.value = isFocus
        _searchHint.value = if (isFocus) "" else strings.getString(R.string.hint_search_items)
    }

    fun onPageSelected(pageIndex: Int) {
        _currentPageIndex.value = pageIndex
    }
}