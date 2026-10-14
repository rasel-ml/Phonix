package app.mrm.phonix.lib.ui.callitems

import app.mrm.phonix.lib.data.model.Call
import app.mrm.phonix.lib.data.repository.calls.CallsRepository
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.ui.list.ListViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class CallItemsViewState @Inject constructor(
    permissions: PermissionsInteractor,
    private val callsRepository: CallsRepository
) :
    ListViewState<Call>(permissions) {

    override fun onItemLeftClick(item: Call) {
        super.onItemLeftClick(item)
        item.leaveConference()
        onFinish()
    }

    override fun onItemRightClick(item: Call) {
        super.onItemRightClick(item)
        item.reject()
        onFinish()
    }

    override fun getItemsFlow(filter: String?): Flow<List<Call>>? =
        callsRepository.getCalls()
}