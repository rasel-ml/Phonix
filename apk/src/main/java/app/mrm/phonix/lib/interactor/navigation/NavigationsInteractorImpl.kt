package app.mrm.phonix.lib.interactor.navigation

import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import app.mrm.phonix.R
import app.mrm.phonix.lib.interactor.string.StringsInteractor
import app.mrm.phonix.lib.ui.base.BaseActivity
import app.mrm.phonix.lib.ui.contacteditor.ContactEditorActivity
import app.mrm.phonix.lib.util.baseobservable.BaseObservable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationsInteractorImpl @Inject constructor(
    private val strings: StringsInteractor,
    private val telecomManager: TelecomManager,
    @ApplicationContext private val context: Context,
) :
    BaseObservable<NavigationsInteractor.Listener>(),
    NavigationsInteractor {

    override fun donate() {
        context.startActivity(
            Intent(
                ACTION_VIEW,
                Uri.parse(strings.getString(R.string.donation_link))
            ).addFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun sendEmail() {
        context.startActivity(
            Intent(ACTION_SEND)
                .setType("message/rfc822")
                .putExtra(EXTRA_EMAIL, arrayOf(strings.getString(R.string.support_email)))
                .setFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun goToAppGithub() {
        context.startActivity(
            Intent(
                ACTION_VIEW,
                Uri.parse(strings.getString(R.string.app_source_url))
            ).addFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun manageBlockedNumber() {
        context.startActivity(
            telecomManager.createManageBlockedNumbersIntent()
                .addFlags(FLAG_ACTIVITY_NEW_TASK),
            null
        )
    }

    override fun goToLauncherActivity() {
        context.startActivity(
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.addFlags(FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun sendSMS(number: String?) {
        context.startActivity(
            Intent(
                ACTION_SENDTO,
                Uri.parse("smsto:${PhoneNumberUtils.normalizeNumber(number)}")
            ).addFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun openWhatsapp(number: String?) {
        context.startActivity(
            Intent(ACTION_VIEW)
                .setData(Uri.parse("http://api.whatsapp.com/send?phone=$number"))
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun addContact(number: String) {
        context.startActivity(
            ContactEditorActivity.newIntent(context, number = number)
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun viewContact(contactId: Long) {
        context.startActivity(
            Intent(ACTION_VIEW)
                .setData(
                    Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_URI,
                        contactId.toString()
                    )
                )
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun goToActivity(activityClass: Class<out BaseActivity<*>>) {
        context.startActivity(
            Intent(context, activityClass)
                .addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun editContact(contactId: Long) {
        context.startActivity(
            ContactEditorActivity.newIntent(context, contactId = contactId)
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
