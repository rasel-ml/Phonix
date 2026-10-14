package app.mrm.phonix.lib.data.repository.calls

import app.mrm.phonix.lib.data.model.Call
import kotlinx.coroutines.flow.Flow

interface CallsRepository {
    fun getCalls(): Flow<List<Call>>
}