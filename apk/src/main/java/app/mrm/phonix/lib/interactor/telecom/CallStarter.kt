package app.mrm.phonix.lib.interactor.telecom

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.READ_PHONE_STATE
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import app.mrm.phonix.R
import app.mrm.phonix.lib.data.model.SimAccount
import app.mrm.phonix.lib.di.factory.fragment.FragmentFactory
import app.mrm.phonix.lib.interactor.permission.PermissionsInteractor
import app.mrm.phonix.lib.interactor.prompt.PromptsInteractor
import app.mrm.phonix.lib.util.PhoneNumberUtils
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

/**
 * Starts a call from the UI.
 *
 * With more than one SIM and no default SIM for calls, the SIM is chosen *before* the call is
 * placed, so the calling screen only opens once the user has picked one.
 */
@ActivityScoped
class CallStarter @Inject constructor(
    private val prompts: PromptsInteractor,
    private val telecomManager: TelecomManager,
    private val telecomInteractor: TelecomInteractor,
    private val permissions: PermissionsInteractor,
    private val fragmentFactory: FragmentFactory,
) {
    fun call(number: String) {
        if (PhoneNumberUtils.isUri(number)) {
            telecomInteractor.callNumber(number)
            return
        }

        permissions.runWithPermissions(
            arrayOf(READ_PHONE_STATE, CALL_PHONE),
            grantedCallback = { callWithSimChoice(number) },
            deniedCallback = { telecomInteractor.callNumber(number) }
        )
    }

    private fun callWithSimChoice(number: String) {
        val handles: List<PhoneAccountHandle> = try {
            telecomManager.callCapablePhoneAccounts
        } catch (e: SecurityException) {
            emptyList()
        }
        val hasDefaultSim = try {
            telecomManager.getDefaultOutgoingPhoneAccount(PhoneAccount.SCHEME_TEL) != null
        } catch (e: SecurityException) {
            false
        }

        val accounts = handles.mapIndexedNotNull { index, handle ->
            telecomManager.getPhoneAccount(handle)?.let { SimAccount(index, it) }
        }

        if (accounts.size < 2 || hasDefaultSim) {
            telecomInteractor.callNumber(number)
            return
        }

        // two SIMs can share a label, so they're told apart by position
        val labels = accounts.map { it.label }
        val choices = if (labels.toSet().size == labels.size) labels
        else accounts.map { "${it.label} · SIM ${it.index + 1}" }

        val fragment = fragmentFactory.getChoicesFragment(
            R.string.hint_sim_account,
            R.string.explain_choose_sim_account,
            choices
        )
        fragment.setOnChoiceClickListener { choice ->
            val picked = accounts.getOrNull(choices.indexOf(choice))
            fragment.finish()
            telecomInteractor.callNumber(number, picked)
            true
        }
        prompts.showFragment(fragment)
    }
}
