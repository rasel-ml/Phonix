package app.mrm.phonix.lib.interactor.sim

import android.telecom.PhoneAccount
import android.telephony.SubscriptionInfo
import app.mrm.phonix.lib.data.model.SimAccount
import app.mrm.phonix.lib.interactor.base.BaseInteractor

interface SimsInteractor : BaseInteractor<SimsInteractor.Listener> {
    interface Listener

    suspend fun getIsMultiSim(): Boolean
    suspend fun getSimAccounts(): List<SimAccount>
    suspend fun getPhoneAccounts(): List<PhoneAccount>
    suspend fun getSubscriptionInfos(): List<SubscriptionInfo>
}