package app.mrm.phonix.lib.ui.permission

import androidx.fragment.app.viewModels
import app.mrm.phonix.databinding.PermissionBinding
import app.mrm.phonix.lib.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionFragment @Inject constructor() : BaseFragment<PermissionViewState>() {
    override val contentView by lazy { _binding.root }
    override val viewState: PermissionViewState by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private val _binding by lazy { PermissionBinding.inflate(layoutInflater) }


    override fun onSetup() {
        viewState.apply {
            textRes.observe(this@PermissionFragment, _binding.noPermissionText::setText)
            imageRes.observe(this@PermissionFragment) {
                _binding.noPermissionImage.setImageResource(it)
            }
        }

        _binding.apply {
            noPermissionButton.setOnClickListener { viewState.onGrantClick() }
        }
    }
}