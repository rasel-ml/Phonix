package app.mrm.phonix.lib.interactor.base

interface BaseInteractor<Listener> {
    fun registerListener(listener: Listener)
    fun unregisterListener(listener: Listener)
    fun invokeListeners(invoker: (Listener) -> Unit)
}