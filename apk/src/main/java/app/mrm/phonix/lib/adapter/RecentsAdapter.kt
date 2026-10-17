package app.mrm.phonix.lib.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import app.mrm.phonix.R
import app.mrm.phonix.lib.data.model.ListData
import app.mrm.phonix.lib.data.model.PhoneLookupAccount
import app.mrm.phonix.lib.data.model.RecentAccount
import app.mrm.phonix.lib.di.module.IoScope
import app.mrm.phonix.lib.di.module.MainScope
import app.mrm.phonix.lib.interactor.animation.AnimationsInteractor
import app.mrm.phonix.lib.interactor.drawable.DrawablesInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.phoneaccounts.PhonesInteractor
import app.mrm.phonix.lib.interactor.recents.RecentsInteractor
import app.mrm.phonix.lib.interactor.sim.CallSimResolver
import app.mrm.phonix.lib.ui.widgets.listitemholder.ListItemHolder
import app.mrm.phonix.lib.util.getHoursString
import app.mrm.phonix.lib.util.initials
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class RecentsAdapter @Inject constructor(
    animations: AnimationsInteractor,
    private val phones: PhonesInteractor,
    private val recents: RecentsInteractor,
    private val drawables: DrawablesInteractor,
    private val simResolver: CallSimResolver,
    preferences: PreferencesInteractor,
    @IoScope private val ioScope: CoroutineScope,
    @MainScope private val mainScope: CoroutineScope,
    @ApplicationContext private val context: Context
) : ListAdapter<RecentAccount>(animations) {

    private var _groupSimilar: Boolean = false

    var groupSimilar: Boolean
        get() = _groupSimilar
        set(value) {
            _groupSimilar = value
        }


    /** Whether every row shows the quick call button on its far right. */
    @SuppressLint("NotifyDataSetChanged")
    var isQuickCall: Boolean = preferences.isQuickCallInRecents
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }


    override fun onBindListItem(
        listItemHolder: ListItemHolder,
        item: RecentAccount,
        position: Int
    ) {
        listItemHolder.apply {
            val date = context.getHoursString(item.date)
            val baseCaption = "$date ·"
            // grouped entries show how many calls they hold right after the name / number
            val countSuffix = if (item.groupCount > 1) " (${item.groupCount})" else ""

            // Always have something to show right away: the call log's cached name (may be an
            // empty string for unknown numbers) or, failing that, the number itself.
            val fallbackTitle = item.cachedName?.takeIf { it.isNotBlank() }
                ?: item.number.takeIf { it.isNotBlank() }
                ?: PhoneLookupAccount.PRIVATE.displayString

            val cachedName = item.cachedName?.takeIf { it.isNotBlank() }

            // Quick call: a phone icon on the far right that calls this log's own number.
            // Rows are recycled, so the visibility is always set. Private numbers can't be called.
            isRightButtonVisible = isQuickCall && item.number.isNotBlank()
            if (isRightButtonVisible) {
                isRightButtonEnabled = true
                setRightButtonIcon(R.drawable.call)
                // leaves room for the fast scroller thumb at the screen edge
                setRightButtonMarginEnd(context.resources.getDimensionPixelSize(R.dimen.recents_quick_call_margin_end))
                setRightButtonContentDescription(context.getString(R.string.content_description_quick_call))
            }

            captionText = baseCaption
            titleText = fallbackTitle + countSuffix
            // second line: the number (when the title is a name, not the number itself)
            subtitleText = if (cachedName != null) item.number.takeIf { it.isNotBlank() } else null
            showFallbackImage(item.cachedName?.takeIf { it.isNotBlank() })
            setCaptionImageRes(recents.getCallTypeImage(item.type))
            setCaptionImageTint(recents.getCallTypeColorRes(item.type)?.let(context::getColor))

            // Rows are recycled; remember which item this holder is bound to so a slow
            // lookup for a previous item can't overwrite the current one.
            itemView.tag = item
            hideSim()

            ioScope.launch {
                val account = phones.lookupAccount(item.number)
                val sim = simResolver.resolve(item.phoneAccountComponent, item.phoneAccountId)

                mainScope.launch {
                    if (itemView.tag !== item) return@launch

                    if (sim == null) hideSim() else showSim(sim.slotNumber, sim.color, sim.isAvailable)

                    val name = account?.name?.takeIf { it.isNotBlank() }
                    titleText = (name ?: fallbackTitle) + countSuffix
                    subtitleText = if (name != null || cachedName != null) {
                        item.number.takeIf { it.isNotBlank() }
                    } else null

                    val photo = account?.photoUri?.takeIf { it.isNotEmpty() }?.let(Uri::parse)
                    when {
                        photo != null -> setImageUri(photo)
                        else -> showFallbackImage(name ?: item.cachedName?.takeIf { it.isNotBlank() })
                    }

                    // a dot separates the SIM icon from the time (only when the icon is shown)
                    val simDot = if (sim != null) " " else ""
                    captionText = if (account?.contactId != null) {
                        "$simDot$baseCaption ${
                            Phone.getTypeLabel(context.resources, account.type, account.label)
                        } ·"
                    } else {
                        "$simDot$baseCaption"
                    }
                }
            }
        }
    }

    /** Initials when we know a name, a generic person icon otherwise (unknown number). */
    private fun ListItemHolder.showFallbackImage(name: String?) {
        if (name != null) {
            imageInitials = name.initials()
        } else {
            drawables.getDrawable(R.drawable.person)?.let(::setImageDrawable)
        }
    }

    override fun convertDataToListData(items: List<RecentAccount>) =
        ListData.fromRecents(items, groupSimilar)
}