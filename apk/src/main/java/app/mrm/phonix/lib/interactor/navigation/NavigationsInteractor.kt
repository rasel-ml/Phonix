package app.mrm.phonix.lib.interactor.navigation

import app.mrm.phonix.lib.interactor.base.BaseInteractor
import app.mrm.phonix.lib.ui.base.BaseActivity

interface NavigationsInteractor : BaseInteractor<NavigationsInteractor.Listener> {
    interface Listener

    fun donate()
    fun sendEmail()
    fun goToAppGithub()
    fun manageBlockedNumber()
    fun goToLauncherActivity()
    fun sendSMS(number: String?)
    fun openWhatsapp(number: String?)
    fun addContact(number: String)
    fun viewContact(contactId: Long)
    fun editContact(contactId: Long)
    fun goToActivity(activityClass: Class<out BaseActivity<*>>)
}