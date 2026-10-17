package app.mrm.phonix.lib.data.model

import androidx.annotation.StringRes
import app.mrm.phonix.R

/** The quick filters shown as chips above the recents list. Display-only: never touches the call log. */
enum class RecentsFilter(@StringRes val titleRes: Int) {
    ALL(R.string.recents_filter_all),
    MISSED(R.string.recents_filter_missed),
    RECEIVED(R.string.recents_filter_received),
    DIALED(R.string.recents_filter_dialed),
    CONTACTS(R.string.recents_filter_contacts),
    UNKNOWN(R.string.recents_filter_unknown);

    /** True when matching a call needs to know whether its number is a saved contact. */
    val needsContactLookup get() = this == CONTACTS || this == UNKNOWN
}
