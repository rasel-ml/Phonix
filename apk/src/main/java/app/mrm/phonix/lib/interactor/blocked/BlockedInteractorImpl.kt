package app.mrm.phonix.lib.interactor.blocked

import android.content.ContentValues
import android.content.Context
import android.provider.BlockedNumberContract
import android.provider.BlockedNumberContract.BlockedNumbers
import app.mrm.phonix.lib.di.module.IoDispatcher
import app.mrm.phonix.lib.interactor.base.BaseInteractorImpl
import app.mrm.phonix.lib.util.annotation.RequiresDefaultDialer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockedInteractorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BaseInteractorImpl<BlockedInteractor.Listener>(), BlockedInteractor {

    @RequiresDefaultDialer
    override suspend fun isNumberBlocked(number: String) = withContext(ioDispatcher) {
        BlockedNumberContract.isBlocked(context, number)
    }

    @RequiresDefaultDialer
    override suspend fun blockNumber(number: String) {
        withContext(ioDispatcher) {
            if (isNumberBlocked(number)) return@withContext
            val contentValues = ContentValues()
            contentValues.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            context.contentResolver.insert(BlockedNumbers.CONTENT_URI, contentValues)
        }
    }

    @RequiresDefaultDialer
    override suspend fun unblockNumber(number: String) {
        withContext(ioDispatcher) {
            if (!isNumberBlocked(number)) return@withContext
            val contentValues = ContentValues()
            contentValues.put(BlockedNumbers.COLUMN_ORIGINAL_NUMBER, number)
            context.contentResolver.insert(BlockedNumbers.CONTENT_URI, contentValues)?.also {
                context.contentResolver.delete(it, null, null)
            }
        }
    }
}