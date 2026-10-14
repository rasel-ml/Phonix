package app.mrm.phonix.lib.ui.contacteditor

import android.graphics.Bitmap
import android.provider.ContactsContract.CommonDataKinds.*
import android.text.InputType.*
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.mrm.phonix.R
import java.util.EnumMap

class TypeOption(val value: Int, @StringRes val labelRes: Int)

private const val TEXT = TYPE_CLASS_TEXT
private const val PHONE_INPUT = TYPE_CLASS_PHONE
private const val EMAIL_INPUT = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_EMAIL_ADDRESS
private const val URL_INPUT = TYPE_CLASS_TEXT or TYPE_TEXT_VARIATION_URI
private const val WORDS = TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_CAP_WORDS
private const val SENTENCES = TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_CAP_SENTENCES
private const val ADDRESS_INPUT = TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_CAP_WORDS or TYPE_TEXT_VARIATION_POSTAL_ADDRESS
private const val NOTE_INPUT = TYPE_CLASS_TEXT or TYPE_TEXT_FLAG_CAP_SENTENCES or TYPE_TEXT_FLAG_MULTI_LINE

/**
 * Every repeatable (or optional) group of fields a contact can hold, besides the name and the photo.
 *
 * The declaration order is the order the sections are shown in.
 */
enum class FieldKind(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val addRes: Int,
    val hintRes: List<Int>,
    val inputTypes: List<Int>,
    val types: List<TypeOption> = emptyList(),
    /** the type value that means "custom label" (0 for most, -1 for IM protocols) */
    val customType: Int = 0,
    val multiple: Boolean = true,
    val isDate: Boolean = false,
    val isMultiline: Boolean = false,
    /** always visible, even when empty */
    val alwaysShown: Boolean = false,
    /** types no longer offered in the picker, but still shown correctly on contacts that use them */
    val legacyTypes: List<TypeOption> = emptyList(),
) {
    PHONE(
        R.string.section_phone, R.drawable.call, R.string.add_phone,
        listOf(R.string.hint_phone_number), listOf(PHONE_INPUT),
        listOf(
            TypeOption(Phone.TYPE_MOBILE, R.string.type_mobile),
            TypeOption(Phone.TYPE_HOME, R.string.type_home),
            TypeOption(Phone.TYPE_WORK, R.string.type_work),
            TypeOption(Phone.TYPE_MAIN, R.string.type_main),
            TypeOption(Phone.TYPE_FAX_WORK, R.string.type_fax_work),
            TypeOption(Phone.TYPE_FAX_HOME, R.string.type_fax_home),
            TypeOption(Phone.TYPE_PAGER, R.string.type_pager),
            TypeOption(Phone.TYPE_OTHER, R.string.type_other),
        ),
        alwaysShown = true,
        legacyTypes = listOf(TypeOption(Phone.TYPE_WORK_MOBILE, R.string.type_work_mobile))
    ),
    EMAIL(
        R.string.section_email, R.drawable.mail, R.string.add_email,
        listOf(R.string.hint_email_address), listOf(EMAIL_INPUT),
        listOf(
            TypeOption(Email.TYPE_HOME, R.string.type_home),
            TypeOption(Email.TYPE_WORK, R.string.type_work),
            TypeOption(Email.TYPE_OTHER, R.string.type_other),
        ),
        alwaysShown = true,
        legacyTypes = listOf(TypeOption(Email.TYPE_MOBILE, R.string.type_mobile))
    ),
    ADDRESS(
        R.string.section_address, R.drawable.location_on, R.string.add_address,
        listOf(
            R.string.hint_street, R.string.hint_city, R.string.hint_region,
            R.string.hint_postcode, R.string.hint_country
        ),
        listOf(ADDRESS_INPUT, WORDS, WORDS, TEXT or TYPE_TEXT_FLAG_CAP_CHARACTERS, WORDS),
        listOf(
            TypeOption(StructuredPostal.TYPE_HOME, R.string.type_home),
            TypeOption(StructuredPostal.TYPE_WORK, R.string.type_work),
            TypeOption(StructuredPostal.TYPE_OTHER, R.string.type_other),
        )
    ),
    ORGANIZATION(
        R.string.section_organization, R.drawable.corporate_fare, R.string.add_organization,
        listOf(R.string.hint_company, R.string.hint_job_title, R.string.hint_department),
        listOf(WORDS, WORDS, WORDS)
    ),
    EVENT(
        R.string.section_event, R.drawable.event, R.string.add_event,
        listOf(R.string.hint_date), listOf(TEXT),
        listOf(
            TypeOption(Event.TYPE_BIRTHDAY, R.string.type_birthday),
            TypeOption(Event.TYPE_ANNIVERSARY, R.string.type_anniversary),
            TypeOption(Event.TYPE_OTHER, R.string.type_other),
        ),
        isDate = true
    ),
    /** the name the contact is sorted and shown under, when different from the usual first + last */
    FILE_AS(
        R.string.section_file_as, R.drawable.person, R.string.add_file_as,
        listOf(R.string.hint_file_as), listOf(WORDS),
        multiple = false
    ),
    WEBSITE(
        R.string.section_website, R.drawable.link, R.string.add_website,
        listOf(R.string.hint_website_url), listOf(URL_INPUT),
        listOf(
            TypeOption(Website.TYPE_HOMEPAGE, R.string.type_homepage),
            TypeOption(Website.TYPE_BLOG, R.string.type_blog),
            TypeOption(Website.TYPE_PROFILE, R.string.type_profile),
            TypeOption(Website.TYPE_HOME, R.string.type_home),
            TypeOption(Website.TYPE_WORK, R.string.type_work),
            TypeOption(Website.TYPE_FTP, R.string.type_ftp),
            TypeOption(Website.TYPE_OTHER, R.string.type_other),
        )
    ),
    IM(
        R.string.section_im, R.drawable.chat, R.string.add_im,
        listOf(R.string.hint_im_handle), listOf(TEXT),
        listOf(
            TypeOption(Im.PROTOCOL_SKYPE, R.string.protocol_skype),
            TypeOption(Im.PROTOCOL_GOOGLE_TALK, R.string.protocol_google_talk),
            TypeOption(Im.PROTOCOL_AIM, R.string.protocol_aim),
            TypeOption(Im.PROTOCOL_MSN, R.string.protocol_msn),
            TypeOption(Im.PROTOCOL_YAHOO, R.string.protocol_yahoo),
            TypeOption(Im.PROTOCOL_ICQ, R.string.protocol_icq),
            TypeOption(Im.PROTOCOL_JABBER, R.string.protocol_jabber),
            TypeOption(Im.PROTOCOL_QQ, R.string.protocol_qq),
            TypeOption(Im.PROTOCOL_NETMEETING, R.string.protocol_netmeeting),
        ),
        customType = Im.PROTOCOL_CUSTOM
    ),
    RELATION(
        R.string.section_relation, R.drawable.group, R.string.add_relation,
        listOf(R.string.hint_relation_name), listOf(WORDS),
        listOf(
            TypeOption(Relation.TYPE_FRIEND, R.string.type_friend),
            TypeOption(Relation.TYPE_SPOUSE, R.string.type_spouse),
            TypeOption(Relation.TYPE_PARTNER, R.string.type_partner),
            TypeOption(Relation.TYPE_DOMESTIC_PARTNER, R.string.type_domestic_partner),
            TypeOption(Relation.TYPE_PARENT, R.string.type_parent),
            TypeOption(Relation.TYPE_MOTHER, R.string.type_mother),
            TypeOption(Relation.TYPE_FATHER, R.string.type_father),
            TypeOption(Relation.TYPE_CHILD, R.string.type_child),
            TypeOption(Relation.TYPE_BROTHER, R.string.type_brother),
            TypeOption(Relation.TYPE_SISTER, R.string.type_sister),
            TypeOption(Relation.TYPE_RELATIVE, R.string.type_relative),
            TypeOption(Relation.TYPE_MANAGER, R.string.type_manager),
            TypeOption(Relation.TYPE_ASSISTANT, R.string.type_assistant),
            TypeOption(Relation.TYPE_REFERRED_BY, R.string.type_referred_by),
        )
    ),
    SIP(
        R.string.section_sip, R.drawable.dialpad, R.string.add_sip,
        listOf(R.string.hint_sip_address), listOf(EMAIL_INPUT),
        listOf(
            TypeOption(SipAddress.TYPE_HOME, R.string.type_home),
            TypeOption(SipAddress.TYPE_WORK, R.string.type_work),
            TypeOption(SipAddress.TYPE_OTHER, R.string.type_other),
        )
    ),
    NOTE(
        R.string.section_note, R.drawable.notes, R.string.add_note,
        listOf(R.string.hint_note), listOf(NOTE_INPUT),
        multiple = false, isMultiline = true
    ),
    CUSTOM(
        R.string.section_custom, R.drawable.tune, R.string.add_custom,
        listOf(R.string.hint_custom_label, R.string.hint_custom_value), listOf(SENTENCES, SENTENCES)
    );

    val hasType get() = types.isNotEmpty()
    val defaultType get() = types.firstOrNull()?.value ?: 0
}

/**
 * One row of a [FieldKind]. [values] holds one entry per input of the kind
 * (e.g. street, city… for an address).
 */
class FieldEntry(
    var values: MutableList<String> = mutableListOf(),
    var type: Int = 0,
    var label: String? = null,
    /** columns the editor doesn't show but must not lose when it rewrites the row */
    var extras: Map<String, String?> = emptyMap()
)

class AccountOption(val name: String?, val type: String?) {
    val isDevice get() = name == null || type == null
    override fun equals(other: Any?) = other is AccountOption && other.name == name && other.type == type
    override fun hashCode() = (name ?: "").hashCode() * 31 + (type ?: "").hashCode()
}

/** What the editor shows and edits. Filled from the contacts provider, written back on save. */
class ContactDraft {
    /** null while creating a new contact */
    var rawContactId: Long? = null
    var account: AccountOption = AccountOption(null, null)

    var prefix = ""
    var firstName = ""
    var middleName = ""
    var lastName = ""
    var suffix = ""
    var nickname = ""
    var phoneticFirst = ""
    var phoneticMiddle = ""
    var phoneticLast = ""

    /** the contact (not raw contact) being edited, null for a new one */
    var contactId: Long? = null
    var starred = false
    var initialStarred = false

    val fields = EnumMap<FieldKind, MutableList<FieldEntry>>(FieldKind::class.java)

    /** photo already stored on the contact (display only) */
    var existingPhotoUri: String? = null
    /** photo picked in this session */
    var newPhoto: Bitmap? = null
    var photoRemoved = false

    val isNew get() = rawContactId == null

    fun entries(kind: FieldKind): MutableList<FieldEntry> = fields.getOrPut(kind) { mutableListOf() }

    fun isEmpty(): Boolean {
        val hasName = listOf(
            prefix, firstName, middleName, lastName, suffix,
            nickname, phoneticFirst, phoneticMiddle, phoneticLast
        ).any { it.isNotBlank() }
        val hasField = fields.values.any { list -> list.any { e -> e.values.any { it.isNotBlank() } } }
        return !hasName && !hasField && newPhoto == null
    }

    /** a comparable summary, used to tell whether the user changed anything */
    fun signature(): String = buildString {
        append(prefix).append('|').append(firstName).append('|').append(middleName).append('|')
            .append(lastName).append('|').append(suffix).append('|').append(nickname).append('|')
            .append(phoneticFirst).append('|').append(phoneticMiddle).append('|')
            .append(phoneticLast).append('|').append(starred).append('|')
            .append(account.name).append(account.type).append('|')
            .append(newPhoto != null).append(photoRemoved).append('|')
        FieldKind.values().forEach { kind ->
            append(kind.name).append(':')
            fields[kind]?.forEach { e ->
                append(e.values.joinToString("\u0001")).append('\u0002').append(e.type)
                    .append('\u0002').append(e.label).append('\u0003')
            }
        }
    }
}
