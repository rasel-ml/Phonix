package app.mrm.phonix.lib.data.model

import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import app.mrm.phonix.lib.util.fullAddress
import app.mrm.phonix.lib.util.fullLabel

data class SimAccount(
    val index: Int,
    val phoneAccount: PhoneAccount
) {
    val label: String
    val address: String
    val phoneAccountHandle: PhoneAccountHandle get() = phoneAccount.accountHandle

    init {
        label = phoneAccount.fullLabel()
        address = phoneAccount.fullAddress()
    }
}