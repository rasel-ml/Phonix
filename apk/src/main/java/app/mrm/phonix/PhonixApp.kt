package app.mrm.phonix

import android.telecom.TelecomManager
import androidx.preference.PreferenceManager
import app.mrm.phonix.lib.ChoolooApp
import app.mrm.phonix.lib.notification.CallNotification
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
open class PhonixApp : ChoolooApp() {
    @Inject lateinit var telecomManager: TelecomManager
    @Inject lateinit var callNotification: CallNotification

    override fun onCreate() {
        super.onCreate()
        PreferenceManager.setDefaultValues(this, R.xml.preferences_phonix, false)
    }
}