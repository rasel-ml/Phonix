package app.mrm.phonix.lib.ui.contacteditor

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.*
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts
import app.mrm.phonix.lib.di.module.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Reads a contact into a [ContactDraft] and writes a [ContactDraft] back to the contacts provider.
 *
 * A contact shown in the contacts list can be made of several raw contacts (one per account that
 * holds it). The editor works on a single, writable raw contact; everything it doesn't manage
 * (group membership, other apps' data rows…) is left untouched.
 */
@Singleton
class ContactEditorRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val resolver get() = context.contentResolver

    private val managedMimeTypes = listOf(
        StructuredName.CONTENT_ITEM_TYPE,
        Phone.CONTENT_ITEM_TYPE,
        Email.CONTENT_ITEM_TYPE,
        StructuredPostal.CONTENT_ITEM_TYPE,
        Organization.CONTENT_ITEM_TYPE,
        Event.CONTENT_ITEM_TYPE,
        Nickname.CONTENT_ITEM_TYPE,
        Website.CONTENT_ITEM_TYPE,
        Im.CONTENT_ITEM_TYPE,
        Relation.CONTENT_ITEM_TYPE,
        SipAddress.CONTENT_ITEM_TYPE,
        Note.CONTENT_ITEM_TYPE,
        MIME_CUSTOM_FIELD
    )

    // region loading

    suspend fun loadDraft(contactId: Long): ContactDraft? = withContext(ioDispatcher) {
        val raw = pickRawContact(contactId) ?: return@withContext null
        val draft = ContactDraft().apply {
            rawContactId = raw.id
            account = AccountOption(raw.accountName, raw.accountType)
            this.contactId = contactId
        }
        queryContactInfo(draft, contactId)

        resolver.query(
            Data.CONTENT_URI,
            arrayOf(Data.MIMETYPE) + DATA_COLUMNS,
            "${Data.RAW_CONTACT_ID}=?",
            arrayOf(raw.id.toString()),
            "${Data._ID} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val mime = cursor.getString(0) ?: continue
                val d = Array(DATA_COLUMNS.size) { cursor.safeString(it + 1) }
                readRow(draft, mime, d)
            }
        }
        draft
    }

    private fun readRow(draft: ContactDraft, mime: String, d: Array<String?>) {
        fun typeOf(i: Int, default: Int) = d[i]?.toIntOrNull() ?: default
        fun add(kind: FieldKind, entry: FieldEntry) {
            if (entry.values.any { it.isNotBlank() }) draft.entries(kind).add(entry)
        }

        when (mime) {
            StructuredName.CONTENT_ITEM_TYPE -> {
                draft.firstName = d[1].orEmpty()
                draft.lastName = d[2].orEmpty()
                draft.prefix = d[3].orEmpty()
                draft.middleName = d[4].orEmpty()
                draft.suffix = d[5].orEmpty()
                draft.phoneticFirst = d[6].orEmpty()
                draft.phoneticMiddle = d[7].orEmpty()
                draft.phoneticLast = d[8].orEmpty()

                val display = d[0]
                val core = nameTokens(d[1], d[4], d[2])
                if (!display.isNullOrBlank()) {
                    if (core.isEmpty()) {
                        // the name was only stored as a whole
                        draft.firstName = display
                    } else {
                        // a display name that isn't just the parts put together was set as "file as"
                        val shown = nameTokens(display)
                        val full = nameTokens(d[3], d[1], d[4], d[2], d[5])
                        if (shown != core && shown != full) {
                            add(FieldKind.FILE_AS, FieldEntry(mutableListOf(display)))
                        }
                    }
                }
            }
            Phone.CONTENT_ITEM_TYPE -> add(
                FieldKind.PHONE,
                FieldEntry(mutableListOf(d[0].orEmpty()), typeOf(1, Phone.TYPE_MOBILE), d[2])
            )
            Email.CONTENT_ITEM_TYPE -> add(
                FieldKind.EMAIL,
                FieldEntry(mutableListOf(d[0].orEmpty()), typeOf(1, Email.TYPE_HOME), d[2])
            )
            StructuredPostal.CONTENT_ITEM_TYPE -> {
                val street = d[3].orEmpty()
                val city = d[6].orEmpty()
                val region = d[7].orEmpty()
                val postcode = d[8].orEmpty()
                val country = d[9].orEmpty()
                val values = mutableListOf(street, city, region, postcode, country)
                if (values.all { it.isBlank() }) {
                    // some accounts only store the formatted address
                    values[0] = d[0].orEmpty()
                }
                add(
                    FieldKind.ADDRESS,
                    FieldEntry(
                        values, typeOf(1, StructuredPostal.TYPE_HOME), d[2],
                        extras = mapOf(
                            StructuredPostal.POBOX to d[4],
                            StructuredPostal.NEIGHBORHOOD to d[5]
                        )
                    )
                )
            }
            Organization.CONTENT_ITEM_TYPE -> add(
                FieldKind.ORGANIZATION,
                FieldEntry(mutableListOf(d[0].orEmpty(), d[3].orEmpty(), d[4].orEmpty()))
            )
            Event.CONTENT_ITEM_TYPE -> add(
                FieldKind.EVENT,
                FieldEntry(mutableListOf(d[0].orEmpty()), typeOf(1, Event.TYPE_BIRTHDAY), d[2])
            )
            Nickname.CONTENT_ITEM_TYPE -> if (draft.nickname.isEmpty()) {
                draft.nickname = d[0].orEmpty()
            }
            Website.CONTENT_ITEM_TYPE -> add(
                FieldKind.WEBSITE,
                FieldEntry(mutableListOf(d[0].orEmpty()), typeOf(1, Website.TYPE_HOMEPAGE), d[2])
            )
            Im.CONTENT_ITEM_TYPE -> add(
                FieldKind.IM,
                FieldEntry(mutableListOf(d[0].orEmpty()), typeOf(4, Im.PROTOCOL_SKYPE), d[5])
            )
            Relation.CONTENT_ITEM_TYPE -> add(
                FieldKind.RELATION,
                FieldEntry(mutableListOf(d[0].orEmpty()), typeOf(1, Relation.TYPE_FRIEND), d[2])
            )
            SipAddress.CONTENT_ITEM_TYPE -> add(
                FieldKind.SIP,
                FieldEntry(mutableListOf(d[0].orEmpty()), typeOf(1, SipAddress.TYPE_HOME), d[2])
            )
            Note.CONTENT_ITEM_TYPE -> if (draft.entries(FieldKind.NOTE).isEmpty()) add(
                FieldKind.NOTE,
                FieldEntry(mutableListOf(d[0].orEmpty()))
            )
            MIME_CUSTOM_FIELD -> add(
                FieldKind.CUSTOM,
                FieldEntry(mutableListOf(d[0].orEmpty(), d[1].orEmpty()))
            )
        }
    }

    private fun nameTokens(vararg parts: String?): List<String> =
        parts.filterNotNull().joinToString(" ").lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotEmpty() }
            .sorted()

    private class RawContactInfo(val id: Long, val accountName: String?, val accountType: String?)

    /** the raw contact that should be edited: device-only first, then Google, then the rest */
    private fun pickRawContact(contactId: Long): RawContactInfo? {
        val candidates = mutableListOf<RawContactInfo>()
        resolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID, RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE),
            "${RawContacts.CONTACT_ID}=? AND ${RawContacts.DELETED}=0",
            arrayOf(contactId.toString()),
            "${RawContacts._ID} ASC"
        )?.use {
            while (it.moveToNext()) {
                candidates += RawContactInfo(it.getLong(0), it.getString(1), it.getString(2))
            }
        }
        val usable = candidates.filterNot { isReadOnlyAccount(it.accountType) }
        return usable.firstOrNull { it.accountType == null }
            ?: usable.firstOrNull { it.accountType == "com.google" }
            ?: usable.firstOrNull()
    }

    private fun isReadOnlyAccount(type: String?): Boolean {
        val t = type?.lowercase() ?: return false
        return t.endsWith("sim") || READ_ONLY_ACCOUNT_HINTS.any { t.contains(it) }
    }

    private fun queryContactInfo(draft: ContactDraft, contactId: Long) {
        resolver.query(
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
            arrayOf(ContactsContract.Contacts.PHOTO_URI, ContactsContract.Contacts.STARRED),
            null, null, null
        )?.use {
            if (it.moveToFirst()) {
                draft.existingPhotoUri = it.getString(0)
                draft.starred = it.getInt(1) == 1
                draft.initialStarred = draft.starred
            }
        }
    }

    suspend fun deleteContact(contactId: Long) = withContext(ioDispatcher) {
        resolver.delete(
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId),
            null, null
        )
    }

    suspend fun getAccounts(): List<AccountOption> = withContext(ioDispatcher) {
        val result = mutableListOf(AccountOption(null, null))
        try {
            resolver.query(
                ContactsContract.Settings.CONTENT_URI,
                arrayOf(ContactsContract.Settings.ACCOUNT_NAME, ContactsContract.Settings.ACCOUNT_TYPE),
                null, null, null
            )?.use {
                while (it.moveToNext()) {
                    val name = it.getString(0)
                    val type = it.getString(1)
                    if (name == null || type == null || isReadOnlyAccount(type)) continue
                    val option = AccountOption(name, type)
                    if (option !in result) result += option
                }
            }
        } catch (e: Exception) {
        }
        result
    }

    // endregion

    // region photo

    suspend fun decodePhoto(uri: Uri): Bitmap? = withContext(ioDispatcher) {
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            var sample = 1
            while (bounds.outWidth / sample > PHOTO_DECODE_LIMIT || bounds.outHeight / sample > PHOTO_DECODE_LIMIT) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext null

            // formats without EXIF data (PNG, WebP…) simply aren't rotated
            val rotation = try {
                resolver.openInputStream(uri)?.use { stream ->
                    when (ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                } ?: 0f
            } catch (e: Exception) {
                0f
            }

            val side = min(decoded.width, decoded.height)
            val square = Bitmap.createBitmap(
                decoded,
                (decoded.width - side) / 2,
                (decoded.height - side) / 2,
                side, side,
                Matrix().apply { if (rotation != 0f) postRotate(rotation) },
                true
            )
            if (square.width > PHOTO_SIZE) {
                Bitmap.createScaledBitmap(square, PHOTO_SIZE, PHOTO_SIZE, true)
            } else {
                square
            }
        } catch (e: Exception) {
            null
        }
    }

    // endregion

    // region saving

    /** @return the id of the raw contact that was written */
    suspend fun save(draft: ContactDraft): Long = withContext(ioDispatcher) {
        val existingId = draft.rawContactId
        val ops = ArrayList<ContentProviderOperation>()

        if (existingId == null) {
            ops += ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                .withValue(RawContacts.ACCOUNT_TYPE, if (draft.account.isDevice) null else draft.account.type)
                .withValue(RawContacts.ACCOUNT_NAME, if (draft.account.isDevice) null else draft.account.name)
                .withValue(RawContacts.STARRED, if (draft.starred) 1 else 0)
                .build()
        } else {
            val mimes = managedMimeTypes.toMutableList()
            if (draft.newPhoto != null || draft.photoRemoved) mimes += Photo.CONTENT_ITEM_TYPE
            ops += ContentProviderOperation.newDelete(Data.CONTENT_URI)
                .withSelection(
                    "${Data.RAW_CONTACT_ID}=? AND ${Data.MIMETYPE} IN (${mimes.joinToString(",") { "?" }})",
                    arrayOf(existingId.toString()) + mimes.toTypedArray()
                )
                .build()
        }

        val contactId = draft.contactId
        if (existingId != null && contactId != null && draft.starred != draft.initialStarred) {
            ops += ContentProviderOperation.newUpdate(
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            ).withValue(ContactsContract.Contacts.STARRED, if (draft.starred) 1 else 0).build()
        }

        fun insert(mime: String): ContentProviderOperation.Builder {
            val builder = ContentProviderOperation.newInsert(Data.CONTENT_URI)
            if (existingId == null) {
                builder.withValueBackReference(Data.RAW_CONTACT_ID, 0)
            } else {
                builder.withValue(Data.RAW_CONTACT_ID, existingId)
            }
            return builder.withValue(Data.MIMETYPE, mime)
        }

        val fileAs = draft.fields[FieldKind.FILE_AS]?.firstOrNull()?.values?.firstOrNull()?.trim().orEmpty()
        if (listOf(
                draft.prefix, draft.firstName, draft.middleName, draft.lastName, draft.suffix,
                draft.phoneticFirst, draft.phoneticMiddle, draft.phoneticLast, fileAs
            ).any { it.isNotBlank() }
        ) {
            ops += insert(StructuredName.CONTENT_ITEM_TYPE)
                .withValue(StructuredName.GIVEN_NAME, draft.firstName.trim())
                .withValue(StructuredName.FAMILY_NAME, draft.lastName.trim())
                .withValue(StructuredName.PREFIX, draft.prefix.trim())
                .withValue(StructuredName.MIDDLE_NAME, draft.middleName.trim())
                .withValue(StructuredName.SUFFIX, draft.suffix.trim())
                .withValue(StructuredName.PHONETIC_GIVEN_NAME, draft.phoneticFirst.trim())
                .withValue(StructuredName.PHONETIC_MIDDLE_NAME, draft.phoneticMiddle.trim())
                .withValue(StructuredName.PHONETIC_FAMILY_NAME, draft.phoneticLast.trim())
                .apply { if (fileAs.isNotEmpty()) withValue(StructuredName.DISPLAY_NAME, fileAs) }
                .build()
        }

        if (draft.nickname.isNotBlank()) {
            ops += insert(Nickname.CONTENT_ITEM_TYPE)
                .withValue(Nickname.NAME, draft.nickname.trim())
                .withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT)
                .build()
        }

        FieldKind.values().forEach { kind ->
            draft.fields[kind].orEmpty().forEach { entry ->
                val v = entry.values.map { it.trim() }
                if (v.all { it.isEmpty() }) return@forEach
                val label = entry.label?.trim()?.takeIf { it.isNotEmpty() }
                val isCustom = entry.type == kind.customType && label != null

                when (kind) {
                    FieldKind.PHONE -> ops += insert(Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, v[0])
                        .withValue(Phone.TYPE, entry.type)
                        .withValue(Phone.LABEL, if (isCustom) label else null)
                        .build()
                    FieldKind.EMAIL -> ops += insert(Email.CONTENT_ITEM_TYPE)
                        .withValue(Email.ADDRESS, v[0])
                        .withValue(Email.TYPE, entry.type)
                        .withValue(Email.LABEL, if (isCustom) label else null)
                        .build()
                    FieldKind.ADDRESS -> {
                        val pobox = entry.extras[StructuredPostal.POBOX]
                        val neighborhood = entry.extras[StructuredPostal.NEIGHBORHOOD]
                        val formatted = listOf(
                            v[0], pobox.orEmpty(), neighborhood.orEmpty(),
                            listOf(v[1], v[2], v[3]).filter { it.isNotEmpty() }.joinToString(" "),
                            v[4]
                        ).filter { it.isNotEmpty() }.joinToString("\n")
                        ops += insert(StructuredPostal.CONTENT_ITEM_TYPE)
                            .withValue(StructuredPostal.FORMATTED_ADDRESS, formatted)
                            .withValue(StructuredPostal.STREET, v[0])
                            .withValue(StructuredPostal.POBOX, pobox)
                            .withValue(StructuredPostal.NEIGHBORHOOD, neighborhood)
                            .withValue(StructuredPostal.CITY, v[1])
                            .withValue(StructuredPostal.REGION, v[2])
                            .withValue(StructuredPostal.POSTCODE, v[3])
                            .withValue(StructuredPostal.COUNTRY, v[4])
                            .withValue(StructuredPostal.TYPE, entry.type)
                            .withValue(StructuredPostal.LABEL, if (isCustom) label else null)
                            .build()
                    }
                    FieldKind.ORGANIZATION -> ops += insert(Organization.CONTENT_ITEM_TYPE)
                        .withValue(Organization.COMPANY, v[0])
                        .withValue(Organization.TITLE, v[1])
                        .withValue(Organization.DEPARTMENT, v[2])
                        .withValue(Organization.TYPE, Organization.TYPE_WORK)
                        .build()
                    FieldKind.EVENT -> ops += insert(Event.CONTENT_ITEM_TYPE)
                        .withValue(Event.START_DATE, v[0])
                        .withValue(Event.TYPE, entry.type)
                        .withValue(Event.LABEL, if (isCustom) label else null)
                        .build()
                    FieldKind.FILE_AS -> Unit // stored as the display name of the name row
                    FieldKind.WEBSITE -> ops += insert(Website.CONTENT_ITEM_TYPE)
                        .withValue(Website.URL, v[0])
                        .withValue(Website.TYPE, entry.type)
                        .withValue(Website.LABEL, if (isCustom) label else null)
                        .build()
                    FieldKind.IM -> ops += insert(Im.CONTENT_ITEM_TYPE)
                        .withValue(Im.DATA, v[0])
                        .withValue(Im.TYPE, Im.TYPE_OTHER)
                        .withValue(Im.PROTOCOL, entry.type)
                        .withValue(Im.CUSTOM_PROTOCOL, if (isCustom) label else null)
                        .build()
                    FieldKind.RELATION -> ops += insert(Relation.CONTENT_ITEM_TYPE)
                        .withValue(Relation.NAME, v[0])
                        .withValue(Relation.TYPE, entry.type)
                        .withValue(Relation.LABEL, if (isCustom) label else null)
                        .build()
                    FieldKind.SIP -> ops += insert(SipAddress.CONTENT_ITEM_TYPE)
                        .withValue(SipAddress.SIP_ADDRESS, v[0])
                        .withValue(SipAddress.TYPE, entry.type)
                        .withValue(SipAddress.LABEL, if (isCustom) label else null)
                        .build()
                    FieldKind.NOTE -> ops += insert(Note.CONTENT_ITEM_TYPE)
                        .withValue(Note.NOTE, v[0])
                        .build()
                    FieldKind.CUSTOM -> ops += insert(MIME_CUSTOM_FIELD)
                        .withValue(Data.DATA1, v[0])
                        .withValue(Data.DATA2, v[1])
                        .build()
                }
            }
        }

        draft.newPhoto?.let { bitmap ->
            val bytes = ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it)
            }.toByteArray()
            ops += insert(Photo.CONTENT_ITEM_TYPE)
                .withValue(Photo.PHOTO, bytes)
                .withValue(Data.IS_SUPER_PRIMARY, 1)
                .build()
        }

        val results = resolver.applyBatch(ContactsContract.AUTHORITY, ops)
        existingId ?: ContentUris.parseId(results[0].uri!!)
    }

    // endregion

    private fun Cursor.safeString(index: Int): String? = try {
        getString(index)
    } catch (e: Exception) {
        null
    }

    companion object {
        /** custom fields have no counterpart in other contacts apps, so they get a mimetype of their own */
        const val MIME_CUSTOM_FIELD = "vnd.android.cursor.item/vnd.app.mrm.phonix.custom_field"

        private const val PHOTO_SIZE = 720
        private const val PHOTO_DECODE_LIMIT = 1440

        /** data1 … data14 (data15 is a blob for photos) */
        private val DATA_COLUMNS = Array(14) { "data${it + 1}" }

        /** accounts that expose their contacts read-only (messengers, SIM cards…) */
        private val READ_ONLY_ACCOUNT_HINTS = listOf(
            "whatsapp", ".sim", "tachyon", "thoughtcrime", "telegram", "viber", "signal"
        )
    }
}
