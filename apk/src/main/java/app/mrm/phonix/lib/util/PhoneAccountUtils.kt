package app.mrm.phonix.lib.util

import android.telecom.PhoneAccount

/**
 * The phone number of a SIM account, as the user knows it (e.g. +123456789).
 *
 * `address.toString()` is the *encoded* uri ("tel:%2B123456789"), so the number has to be read from
 * the scheme specific part, which is already decoded. Encoding it again turned "+" into "%252B".
 */
private fun PhoneAccount.phoneNumber(): String? =
    address
        ?.takeIf { it.scheme == PhoneAccount.SCHEME_TEL }
        ?.schemeSpecificPart
        ?.takeIf { it.isNotEmpty() }

fun PhoneAccount.fullAddress(): String = phoneNumber() ?: address.toString()

fun PhoneAccount.fullLabel(): String {
    val number = phoneNumber() ?: return label.toString()
    return "$label ($number)"
}
