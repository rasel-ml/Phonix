package app.mrm.phonix.lib.interactor.telecom

import app.mrm.phonix.lib.interactor.base.BaseInteractor
import app.mrm.phonix.lib.data.model.SimAccount

interface TelecomInteractor : BaseInteractor<TelecomInteractor.Listener> {
    interface Listener {}

    fun handleMmi(code: String): Boolean
    fun handleSecretCode(code: String): Boolean
    fun handleSpecialChars(code: String): Boolean
    fun callVoicemail()
    fun callNumber(number: String, simAccount: SimAccount? = null)
}