package app.mrm.phonix.lib.data.repository.recents

import app.mrm.phonix.lib.data.model.RecentAccount
import kotlinx.coroutines.flow.Flow

interface RecentsRepository {
    fun getRecent(recentId: Long? = null): Flow<RecentAccount?>
    fun getRecents(filter: String? = null): Flow<List<RecentAccount>>
}