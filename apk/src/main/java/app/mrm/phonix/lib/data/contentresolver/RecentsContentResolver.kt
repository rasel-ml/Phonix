package app.mrm.phonix.lib.data.contentresolver

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import app.mrm.phonix.lib.di.module.IoDispatcher
import app.mrm.phonix.lib.data.model.RecentAccount
import app.mrm.phonix.lib.util.SelectionBuilder
import kotlinx.coroutines.CoroutineDispatcher
import java.util.*

class RecentsContentResolver(
    private val recentId: Long? = null,
    contentResolver: ContentResolver,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) :
    BaseContentResolver<RecentAccount>(contentResolver, ioDispatcher) {

    override val uri: Uri = CallLog.Calls.CONTENT_URI
    override val filterUri: Uri? = null
    override val sortOrder: String = "${CallLog.Calls.DATE} DESC"
    override val selectionArgs: Array<String>? = null
    override val projection: Array<String> = arrayOf(
        CallLog.Calls._ID,
        CallLog.Calls.NUMBER,
        CallLog.Calls.NUMBER_PRESENTATION,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.TYPE,
        CallLog.Calls.PHONE_ACCOUNT_ID,
        CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME
    )

    override val selection: String
        get() {
            val selection = SelectionBuilder().addSelection(CallLog.Calls._ID, recentId)
            filter?.let { selection.addString("(${CallLog.Calls.CACHED_NAME} LIKE '%$filter%' OR ${CallLog.Calls.NUMBER} LIKE '%$filter%')") }
            return selection.build()
        }


    @SuppressLint("Range")
    override fun convertCursorToItem(cursor: Cursor): RecentAccount {
        return RecentAccount(
            id = cursor.getLong(cursor.getColumnIndex(CallLog.Calls._ID)),
            type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE)),
            date = Date(cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE))),
            number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER)) ?: "",
            duration = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DURATION)),
            cachedName = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)),
            phoneAccountId = cursor.getString(cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)),
            phoneAccountComponent = cursor.getString(
                cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)
            )
        )
    }
}