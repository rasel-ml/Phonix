package app.mrm.phonix.lib.ui.recents

import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import app.mrm.phonix.R
import app.mrm.phonix.lib.data.model.RecentAccount
import app.mrm.phonix.lib.data.model.RecentsFilter
import app.mrm.phonix.lib.data.repository.recents.RecentsRepository
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.interactor.phoneaccounts.PhonesInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.ui.list.ListViewState
import app.mrm.phonix.lib.util.DataLiveEvent
import app.mrm.phonix.lib.util.MutableDataLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
open class RecentsViewState @Inject constructor(
    permissions: PermissionsInteractor,
    private val clipboardManager: ClipboardManager,
    private val recentsRepository: RecentsRepository,
    private val phones: PhonesInteractor,
    private val preferences: PreferencesInteractor,
) :
    ListViewState<RecentAccount>(permissions) {

    override val noResultsIconRes = R.drawable.history
    override val noResultsTextRes = R.string.error_no_results_recents
    override val permissionsImageRes = R.drawable.history
    override val permissionsTextRes = R.string.error_no_permissions_recents
    override val requiredPermissions = listOf(READ_CALL_LOG, READ_CONTACTS)

    private val _showRecentEvent = MutableDataLiveEvent<RecentAccount>()
    private val _callEvent = MutableDataLiveEvent<String>()

    private val _selectedFilterFlow = MutableStateFlow(RecentsFilter.ALL)
    private val _selectedFilter = MutableLiveData(RecentsFilter.ALL)
    private val _isQuickCall = MutableLiveData(preferences.isQuickCallInRecents)

    /** number -> is it saved in the contacts. Only ever filled when a contact filter is used. */
    private val savedNumbers = ConcurrentHashMap<String, Boolean>()

    val showRecentEvent = _showRecentEvent as DataLiveEvent<RecentAccount>
    val callEvent = _callEvent as DataLiveEvent<String>
    val selectedFilter = _selectedFilter as LiveData<RecentsFilter>
    val isQuickCall = _isQuickCall as LiveData<Boolean>


    override fun onItemClick(item: RecentAccount) {
        super.onItemClick(item)
        _showRecentEvent.call(item)
    }

    override fun onItemLongClick(item: RecentAccount) {
        super.onItemLongClick(item)
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied number", item.number))
        onMessage(R.string.number_copied_to_clipboard)
    }

    /** The quick call button: calls exactly the number of the tapped call log. */
    override fun onItemRightClick(item: RecentAccount) {
        super.onItemRightClick(item)
        if (item.number.isNotBlank()) _callEvent.call(item.number)
    }

    fun onQuickCallChanged(isEnabled: Boolean) {
        _isQuickCall.value = isEnabled
    }

    fun onFilterSelected(filter: RecentsFilter) {
        if (_selectedFilterFlow.value == filter) return
        // a contact may have been saved / deleted since the last lookup
        savedNumbers.clear()
        _selectedFilter.value = filter
        _selectedFilterFlow.value = filter
    }

    // The chip filter is applied on top of the call log flow, so changing the selected chip
    // re-filters the list right away without re-querying (or touching) the call log.
    override fun getItemsFlow(filter: String?): Flow<List<RecentAccount>>? =
        recentsRepository.getRecents(filter)
            .combine(_selectedFilterFlow) { items, selected -> applyFilter(items, selected) }
            .flowOn(Dispatchers.IO)

    private suspend fun applyFilter(
        items: List<RecentAccount>,
        filter: RecentsFilter
    ): List<RecentAccount> = when (filter) {
        RecentsFilter.ALL -> items
        RecentsFilter.MISSED -> items.filter { it.type == RecentAccount.TYPE_MISSED }
        RecentsFilter.RECEIVED -> items.filter { it.type == RecentAccount.TYPE_INCOMING }
        RecentsFilter.DIALED -> items.filter { it.type == RecentAccount.TYPE_OUTGOING }
        RecentsFilter.CONTACTS -> {
            resolveSaved(items)
            items.filter { savedNumbers[it.number] == true }
        }
        RecentsFilter.UNKNOWN -> {
            resolveSaved(items)
            items.filter { savedNumbers[it.number] != true }
        }
    }

    private suspend fun resolveSaved(items: List<RecentAccount>) {
        items.asSequence()
            .map { it.number }
            .filter { it.isNotBlank() && !savedNumbers.containsKey(it) }
            .distinct()
            .chunked(LOOKUP_BATCH)
            .forEach { batch ->
                coroutineScope {
                    batch.map { number ->
                        async { number to (phones.lookupAccount(number)?.contactId != null) }
                    }.awaitAll()
                }.forEach { (number, isSaved) -> savedNumbers[number] = isSaved }
            }
    }

    companion object {
        private const val LOOKUP_BATCH = 16
    }
}
