package app.mrm.phonix.lib.interactor.sim

import android.Manifest.permission.READ_PHONE_STATE
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.SystemClock
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Which SIM a call log entry belongs to. */
class CallSimInfo(
    /** 1 for the first SIM slot, 2 for the second…; null when unknown */
    val slotNumber: Int?,
    /** the colour chosen for the SIM in the system settings, if known */
    @ColorInt val color: Int?,
    /** false when the SIM that made / received the call isn't in the phone anymore */
    val isAvailable: Boolean
)

/**
 * Works out the SIM of a call from the phone account saved with the call log entry (not from the
 * SIMs that happen to be installed right now).
 */
@Singleton
class CallSimResolver @Inject constructor(
    private val telecomManager: TelecomManager,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager,
    @ApplicationContext private val context: Context
) {
    private var cachedSubscriptions: List<SubscriptionInfo> = emptyList()
    private var cachedAt = 0L

    /**
     * @return null when the call wasn't made over a SIM (or nothing was saved about it),
     * so no SIM icon should be shown
     */
    fun resolve(componentName: String?, accountId: String?): CallSimInfo? {
        if (componentName.isNullOrEmpty() || accountId.isNullOrEmpty()) return null

        if (ContextCompat.checkSelfPermission(context, READ_PHONE_STATE) != PERMISSION_GRANTED) {
            return CallSimInfo(null, null, false)
        }

        return try {
            val component = ComponentName.unflattenFromString(componentName)
                ?: return CallSimInfo(null, null, false)
            val handle = PhoneAccountHandle(component, accountId)

            // an account that is no longer registered belongs to a SIM that was removed
            val account = telecomManager.getPhoneAccount(handle)
                ?: return CallSimInfo(null, null, false)
            if (!account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) return null

            val subscriptions = activeSubscriptions()
            val subscriptionId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                telephonyManager.getSubscriptionId(handle)
            } else {
                SubscriptionManager.INVALID_SUBSCRIPTION_ID
            }

            val subscription = subscriptions.firstOrNull { it.subscriptionId == subscriptionId }
                ?: subscriptions.firstOrNull {
                    (!it.iccId.isNullOrEmpty() && it.iccId == accountId) ||
                            it.subscriptionId.toString() == accountId
                }

            if (subscription == null) {
                CallSimInfo(null, null, true)
            } else {
                CallSimInfo(
                    slotNumber = subscription.simSlotIndex.takeIf { it >= 0 }?.plus(1),
                    color = subscription.iconTint,
                    isAvailable = true
                )
            }
        } catch (e: SecurityException) {
            CallSimInfo(null, null, false)
        }
    }

    @Synchronized
    private fun activeSubscriptions(): List<SubscriptionInfo> {
        val now = SystemClock.elapsedRealtime()
        if (now - cachedAt > CACHE_MILLIS) {
            cachedSubscriptions = subscriptionManager.activeSubscriptionInfoList.orEmpty()
            cachedAt = now
        }
        return cachedSubscriptions
    }

    private companion object {
        const val CACHE_MILLIS = 3000L
    }
}
