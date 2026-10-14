package app.mrm.phonix.lib.interactor.blocked

import app.mrm.phonix.lib.interactor.base.BaseInteractor

interface BlockedInteractor : BaseInteractor<BlockedInteractor.Listener> {
    interface Listener

    suspend fun blockNumber(number: String)
    suspend fun unblockNumber(number: String)
    suspend fun isNumberBlocked(number: String): Boolean
}