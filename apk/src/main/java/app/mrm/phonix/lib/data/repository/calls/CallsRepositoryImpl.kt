package app.mrm.phonix.lib.data.repository.calls

import app.mrm.phonix.lib.api.service.CallService
import app.mrm.phonix.lib.data.model.Call
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallsRepositoryImpl @Inject constructor() : CallsRepository {
    override fun getCalls(): Flow<List<Call>> = flow {
        CallService.sInstance?.calls?.collect(this::emit)
    }
}