package app.mrm.phonix.lib.ui.contacteditor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import app.mrm.phonix.R
import app.mrm.phonix.databinding.CropPhotoBinding
import app.mrm.phonix.lib.ui.base.BaseActivity
import app.mrm.phonix.lib.ui.base.BaseViewState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Lets the user choose which square part of a photo becomes the contact picture. */
@AndroidEntryPoint
class CropPhotoActivity : BaseActivity<BaseViewState>() {
    override val contentView by lazy { binding.root }
    override val viewState: BaseViewState by viewModels()

    private val binding by lazy { CropPhotoBinding.inflate(layoutInflater) }


    override fun onSetup() {
        binding.cropClose.setOnClickListener { finish() }
        binding.cropDone.setOnClickListener { onDoneClick() }
        binding.cropDone.isEnabled = false

        val uri = intent.data
        if (uri == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                ImageUtils.decode(this@CropPhotoActivity, uri, MAX_DECODE_SIDE)
            }
            if (bitmap == null) {
                showError(R.string.editor_error_save)
                finish()
            } else {
                binding.cropView.setBitmap(bitmap)
                binding.cropDone.isEnabled = true
            }
        }
    }

    private fun onDoneClick() {
        val cropped = binding.cropView.getCroppedBitmap(OUTPUT_SIDE) ?: return
        binding.cropDone.isEnabled = false

        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                cacheDir.listFiles { f -> f.name.startsWith(FILE_PREFIX) }?.forEach { it.delete() }
                File(cacheDir, "$FILE_PREFIX${System.currentTimeMillis()}.jpg").also { target ->
                    FileOutputStream(target).use {
                        cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, it)
                    }
                }
            }
            setResult(RESULT_OK, Intent().setData(Uri.fromFile(file)))
            finish()
        }
    }

    /** Starts the cropper for a photo and returns the cropped photo's uri, or null if cancelled. */
    class Contract : ActivityResultContract<Uri, Uri?>() {
        override fun createIntent(context: Context, input: Uri): Intent =
            Intent(context, CropPhotoActivity::class.java)
                .setData(input)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
            if (resultCode == Activity.RESULT_OK) intent?.data else null
    }

    companion object {
        private const val FILE_PREFIX = "contact_crop_"
        private const val MAX_DECODE_SIDE = 2048
        private const val OUTPUT_SIDE = 720
    }
}
