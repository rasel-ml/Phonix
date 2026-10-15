package app.mrm.phonix.lib.ui.recent

import android.content.res.ColorStateList
import android.os.Bundle
import androidx.core.content.ContextCompat
import app.mrm.phonix.R
import app.mrm.phonix.lib.util.getAttrColor
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import app.mrm.phonix.databinding.RecentBinding
import app.mrm.phonix.lib.interactor.prompt.PromptsInteractor
import app.mrm.phonix.lib.interactor.telecom.CallStarter
import app.mrm.phonix.lib.interactor.telecom.TelecomInteractor
import app.mrm.phonix.lib.ui.base.BaseFragment
import app.mrm.phonix.lib.ui.recent.menu.RecentMenuViewState
import app.mrm.phonix.lib.ui.recents.RecentsViewState
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecentFragment @Inject constructor() : BaseFragment<RecentViewState>() {
    override val contentView by lazy { binding.root }
    override val viewState: RecentViewState by viewModels()

    private val historyViewState: RecentsViewState by viewModels()
    private val menuViewState: RecentMenuViewState by activityViewModels()
    private val binding by lazy { RecentBinding.inflate(layoutInflater) }

    @Inject lateinit var prompts: PromptsInteractor
    @Inject lateinit var telecomInteractor: TelecomInteractor
    @Inject lateinit var callStarter: CallStarter


    override fun onSetup() {
        showPlaceholderImage()

        binding.recentMainActions.apply {
            recentButtonSms.setOnClickListener { viewState.onSms() }
            recentButtonCall.setOnClickListener { viewState.onCall() }
            recentButtonMore.setOnClickListener { viewState.onMoreClick() }
            recentButtonContact.setOnClickListener { viewState.onOpenContact() }
            recentButtonAddContact.setOnClickListener { viewState.onAddContact() }
        }

        viewState.apply {
            onRecentId(args.getLong(ARG_RECENT_ID))
            isBlocked.observe(this@RecentFragment, menuViewState::onIsBlocked)
            recentNumber.observe(this@RecentFragment, menuViewState::onRecentNumber)
            typeImage.observe(this@RecentFragment, binding.recentTypeImage::setImageResource)
            typeColor.observe(this@RecentFragment) { res ->
                binding.recentTypeImage.imageTintList = res?.let {
                    ColorStateList.valueOf(ContextCompat.getColor(baseActivity, it))
                } ?: ColorStateList.valueOf(baseActivity.getAttrColor(R.attr.colorPrimary))
            }

            imageUri.observe(this@RecentFragment) {
                val image = binding.recentContactImage
                Picasso.with(baseActivity).load(it).into(image, object : Callback {
                    override fun onSuccess() {
                        // the placeholder is tinted and padded; a real photo must not be
                        image.imageTintList = null
                        image.setPadding(0, 0, 0, 0)
                    }

                    override fun onError() {}
                })
            }

            name.observe(this@RecentFragment) {
                binding.recentTextName.text = it
            }

            caption.observe(this@RecentFragment) {
                binding.recentCaption.text = it
            }

            isContactVisible.observe(this@RecentFragment) {
                binding.recentMainActions.recentButtonContact.isVisible = it
            }

            isAddContactVisible.observe(this@RecentFragment) {
                binding.recentMainActions.recentButtonAddContact.isVisible = it
            }

            callEvent.observe(this@RecentFragment) { ev ->
                ev.ifNew?.let(callStarter::call)
            }

            showMoreEvent.observe(this@RecentFragment) {
                it.ifNew?.let {
                    prompts.showFragment(fragmentFactory.getRecentMenuFragment())
                }
            }

            showContactEvent.observe(this@RecentFragment) { ev ->
                ev.ifNew?.let {
                    prompts.showFragment(fragmentFactory.getBriefContactFragment(it))
                }
            }

            showRecentEvent.observe(this@RecentFragment) { ev ->
                ev.ifNew?.let { prompts.showFragment(fragmentFactory.getRecentFragment(it)) }
            }
        }

        historyViewState.itemClickedEvent.observe(this) {
            it.ifNew?.let { recent -> viewState.onRecentClick(recent.id) }
        }

        menuViewState.apply {
            finishEvent.observe(this@RecentFragment) {
                viewState.onFinish()
            }

            onRecentId(args.getLong(ARG_RECENT_ID))
        }
    }

    /** contact photo slot before (or without) a photo: a tinted person icon on a soft circle */
    private fun showPlaceholderImage() = binding.recentContactImage.apply {
        isVisible = true
        val pad = (resources.displayMetrics.density * 12).toInt()
        setPadding(pad, pad, pad, pad)
        imageTintList = ColorStateList.valueOf(baseActivity.getAttrColor(R.attr.colorOnSurfaceVariant))
    }

    companion object {
        const val ARG_RECENT_ID = "recent_id"

        fun newInstance(recentId: Long) = RecentFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_RECENT_ID, recentId)
            }
        }
    }
}