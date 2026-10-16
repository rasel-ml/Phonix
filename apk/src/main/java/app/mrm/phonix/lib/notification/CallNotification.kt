package app.mrm.phonix.lib.notification

import android.app.Notification
import android.app.Service
import android.app.Notification.CATEGORY_CALL
import android.app.Notification.EXTRA_NOTIFICATION_ID
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.telecom.Call.Details.CAPABILITY_MUTE
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import app.mrm.phonix.R
import app.mrm.phonix.lib.data.model.Call
import app.mrm.phonix.lib.data.model.Call.State.DISCONNECTED
import app.mrm.phonix.lib.data.model.Call.State.DISCONNECTING
import app.mrm.phonix.lib.data.model.PhoneLookupAccount
import app.mrm.phonix.lib.api.service.CallService
import app.mrm.phonix.lib.di.module.IoScope
import app.mrm.phonix.lib.interactor.callaudio.CallAudiosInteractor
import app.mrm.phonix.lib.interactor.calls.CallsInteractor
import app.mrm.phonix.lib.interactor.color.ColorsInteractor
import app.mrm.phonix.lib.interactor.phoneaccounts.PhonesInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor.Companion.IncomingCallMode
import app.mrm.phonix.lib.interactor.string.StringsInteractor
import app.mrm.phonix.lib.receiver.CallBroadcastReceiver
import app.mrm.phonix.lib.receiver.CallBroadcastReceiver.Companion.ACTION_ANSWER
import app.mrm.phonix.lib.receiver.CallBroadcastReceiver.Companion.ACTION_HANGUP
import app.mrm.phonix.lib.receiver.CallBroadcastReceiver.Companion.ACTION_MUTE
import app.mrm.phonix.lib.receiver.CallBroadcastReceiver.Companion.ACTION_SPEAKER
import app.mrm.phonix.lib.receiver.CallBroadcastReceiver.Companion.ACTION_UNMUTE
import app.mrm.phonix.lib.receiver.CallBroadcastReceiver.Companion.ACTION_UNSPEAKER
import app.mrm.phonix.lib.ui.call.CallActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(Build.VERSION_CODES.O)
class CallNotification @Inject constructor(
    private val calls: CallsInteractor,
    private val colors: ColorsInteractor,
    private val phones: PhonesInteractor,
    private val strings: StringsInteractor,
    @IoScope private val ioScope: CoroutineScope,
    private val callAudios: CallAudiosInteractor,
    private val preferences: PreferencesInteractor,
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManagerCompat
) : CallsInteractor.Listener, CallAudiosInteractor.Listener {

    private var _call: Call? = null

    /** the pop-up is only used while a call is ringing and the user chose pop-up notifications */
    private val _isPopUp
        get() = _call?.isIncoming == true && preferences.incomingCallMode == IncomingCallMode.POP_UP

    private val _channel
        get() = if (_isPopUp) _channelIncoming else _channelImportanceLow

    private val _channelImportanceLow by lazy {
        NotificationChannelCompat.Builder(CHANNEL_ID_PRIORITY_LOW, IMPORTANCE_DEFAULT)
            .setName(strings.getString(R.string.call_notification_channel_name))
            .setDescription(strings.getString(R.string.call_notification_channel_description))
            .build()
            .also(notificationManager::createNotificationChannel)
    }

    // heads-up channel for ringing calls; the ringtone itself is played by the system, so this
    // channel stays silent
    private val _channelIncoming by lazy {
        NotificationChannelCompat.Builder(CHANNEL_ID_INCOMING_CALL, IMPORTANCE_HIGH)
            .setName(strings.getString(R.string.call_notification_channel_incoming_name))
            .setDescription(strings.getString(R.string.call_notification_channel_incoming_description))
            .setSound(null, null)
            .setVibrationEnabled(false)
            .setShowBadge(false)
            .build()
            .also(notificationManager::createNotificationChannel)
    }

    private val _contentPendingIntent by lazy {
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, CallActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
            },
            FLAG_IMMUTABLE
        )
    }

    // shown over the lock screen (or when the screen is off) while the call is ringing
    private val _fullScreenPendingIntent by lazy {
        PendingIntent.getActivity(
            context,
            1,
            Intent(context, CallActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP
            },
            FLAG_IMMUTABLE
        )
    }

    private val _answerIntent by lazy { getCallPendingIntent(ACTION_ANSWER, 0) }
    private val _hangupIntent by lazy { getCallPendingIntent(ACTION_HANGUP, 1) }

    private val _muteAction by lazy {
        NotificationCompat.Action(
            R.drawable.mic,
            strings.getString(R.string.call_action_mute),
            getCallPendingIntent(ACTION_MUTE, 2)
        )
    }

    private val _unmuteAction by lazy {
        NotificationCompat.Action(
            R.drawable.mic_off,
            strings.getString(R.string.call_action_unmute),
            getCallPendingIntent(ACTION_UNMUTE, 3)
        )
    }

    private val _speakerAction by lazy {
        NotificationCompat.Action(
            R.drawable.volume_down,
            strings.getString(R.string.call_action_speaker),
            getCallPendingIntent(ACTION_SPEAKER, 4)
        )
    }

    private val _unspeakerAction by lazy {
        NotificationCompat.Action(
            R.drawable.volume_up,
            strings.getString(R.string.call_action_speaker_off),
            getCallPendingIntent(ACTION_UNSPEAKER, 5)
        )
    }


    override fun onNoCalls() {
        detach()
    }

    override fun onCallChanged(call: Call) {
    }

    override fun onMainCallChanged(call: Call) {
        _call = call
        refresh()
    }

    override fun onMuteChanged(isMuted: Boolean) {
        refresh()
    }

    override fun onAudioRouteChanged(audioRoute: CallAudiosInteractor.AudioRoute) {
        refresh()
    }


    private fun getCallIntent(callAction: String) =
        Intent(context, CallBroadcastReceiver::class.java).apply {
            action = callAction
            putExtra(EXTRA_NOTIFICATION_ID, ID)
        }

    private fun getCallPendingIntent(callAction: String, rc: Int) =
        PendingIntent.getBroadcast(
            context,
            rc,
            getCallIntent(callAction),
            FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
        )

    private suspend fun buildNotification(call: Call, callback: (Notification) -> Unit) {
        val phoneAccount = phones.lookupAccount(call.number)
        val number = call.number?.takeIf { it.isNotBlank() }
        val name = phoneAccount?.displayString?.takeIf { it.isNotBlank() }
            ?: number
            ?: PhoneLookupAccount.PRIVATE.displayString
        // the number is shown under the name, unless the name already is the number
        val numberLine = number?.takeIf { it != name }
        val stateLine = strings.getString(call.state.stringRes)

        val person = Person.Builder().setName(name).setImportant(true).build()
        val isPopUp = _isPopUp
        val isEnding = call.state in arrayOf(DISCONNECTED, DISCONNECTING)

        val builder = NotificationCompat.Builder(context, _channel.id)
            .setWhen(0)
            .setOngoing(true)
            .setPriority(PRIORITY)
            .setOnlyAlertOnce(true)
            .setCategory(CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle(name)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(_contentPendingIntent)
            .setColor(colors.getAttrColor(R.attr.colorSecondary))
            .setContentText(listOfNotNull(stateLine, numberLine).joinToString(" · "))

        if (call.isIncoming) {
            // big green Answer + big red Decline buttons (same look as Google Phone / WhatsApp)
            builder.setStyle(
                NotificationCompat.CallStyle.forIncomingCall(person, _hangupIntent, _answerIntent)
            )
            if (isPopUp) {
                builder.setFullScreenIntent(_fullScreenPendingIntent, true)
            }
        } else if (!isEnding) {
            builder.setStyle(NotificationCompat.CallStyle.forOngoingCall(person, _hangupIntent))
            callAudios.isMuted?.let { isMuted ->
                if (call.isCapable(CAPABILITY_MUTE)) {
                    builder.addAction(if (isMuted) _unmuteAction else _muteAction)
                }
            }

            callAudios.isSpeakerOn?.let { isSpeakerOn ->
                builder.addAction(if (isSpeakerOn) _unspeakerAction else _speakerAction)
            }
        }

        callback.invoke(builder.build())
    }


    fun attach() {
        calls.registerListener(this)
        callAudios.registerListener(this)
        Thread.currentThread().setUncaughtExceptionHandler { _, _ -> detach() }
    }

    fun detach() {
        calls.unregisterListener(this)
        callAudios.unregisterListener(this)
        // the notification is the service's foreground notification: drop that state first
        try {
            CallService.sInstance?.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            // service already gone
        }
        notificationManager.cancel(ID)
    }

    fun show(call: Call) {
        ioScope.launch {
            buildNotification(call) { post(it) }
        }
    }

    /**
     * Call styled notifications must belong to a foreground service (or have a full screen
     * intent), so the call service is moved to the foreground with this notification. The same
     * call is used to update it later.
     */
    private fun post(notification: Notification) {
        try {
            val service = CallService.sInstance
            if (service != null) {
                service.startForeground(ID, notification)
            } else {
                notificationManager.notify(ID, notification)
            }
        } catch (e: Exception) {
            try {
                notificationManager.notify(ID, notification)
            } catch (e2: Exception) {
                // nothing more we can do: the call screen itself still works
            }
        }
    }

    fun refresh() {
        _call?.let(::show)
    }


    companion object {
        const val ID = 420
        const val PRIORITY = NotificationCompat.PRIORITY_HIGH
        const val CHANNEL_ID_PRIORITY_LOW = "cnc_priority_low"
        const val CHANNEL_ID_INCOMING_CALL = "cnc_incoming_call"
    }
}