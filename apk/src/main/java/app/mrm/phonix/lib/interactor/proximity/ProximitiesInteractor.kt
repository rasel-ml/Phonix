package app.mrm.phonix.lib.interactor.proximity

import app.mrm.phonix.lib.interactor.base.BaseInteractor

interface ProximitiesInteractor : BaseInteractor<ProximitiesInteractor.Listener> {
    interface Listener

    fun acquire()
    fun release()
}