package app.mrm.phonix.lib.ui.call

import android.annotation.SuppressLint
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import app.mrm.phonix.R
import app.mrm.phonix.databinding.CallBinding
import app.mrm.phonix.lib.di.factory.fragment.FragmentFactory
import app.mrm.phonix.lib.interactor.animation.AnimationsInteractor
import app.mrm.phonix.lib.interactor.dialog.DialogsInteractor
import app.mrm.phonix.lib.interactor.prompt.PromptsInteractor
import app.mrm.phonix.lib.interactor.screen.ScreensInteractor
import app.mrm.phonix.lib.ui.base.BaseActivity
import app.mrm.phonix.lib.ui.call.CallViewState.UIState
import app.mrm.phonix.lib.ui.dialpad.DialpadViewState
import app.mrm.phonix.lib.util.getAttrColor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("ClickableViewAccessibility")
class CallActivity : BaseActivity<CallViewState>() {
    override val contentView by lazy { binding.root }
    override val viewState: CallViewState by viewModels()

    private val dialpadViewState: DialpadViewState by viewModels()
    private val binding by lazy { CallBinding.inflate(layoutInflater) }

    @Inject lateinit var screens: ScreensInteractor
    @Inject lateinit var dialogs: DialogsInteractor
    @Inject lateinit var prompts: PromptsInteractor
    @Inject lateinit var fragmentFactory: FragmentFactory
    @Inject lateinit var animations: AnimationsInteractor

    private var callName: String? = null
    private var callNumber: String? = null


    override fun onSetup() {
        screens.showWhenLocked()

        viewState.apply {
            imageRes.observe(this@CallActivity, binding.callImage::setImageResource)
            stateTextColor.observe(this@CallActivity, binding.callStateText::setTextColor)

            name.observe(this@CallActivity) {
                callName = it
                renderCallerName()
            }

            number.observe(this@CallActivity) {
                callNumber = it
                renderCallerName()
            }

            imageURI.observe(this@CallActivity) {
                binding.callImage.setImageURI(it)
                binding.callImage.isVisible = it != null
            }

            elapsedTime.observe(this@CallActivity) {
                it?.let {
                    animations.show(binding.callTimeText, true)
                    binding.callTimeText.text = DateUtils.formatElapsedTime(it / 1000)
                } ?: run {
                    animations.hide(binding.callTimeText, ifVisible = true, goneOrInvisible = false)
                }
            }

            bannerText.observe(this@CallActivity) {
                it?.let {
                    binding.callBanner.text = it
                    if (binding.callBanner.visibility != View.VISIBLE) {
                        binding.callBanner.visibility = View.VISIBLE
                        animations.show(binding.callBanner, true)
                        animations.focus(binding.callBanner)
                    }
                } ?: run {
                    animations.hide(
                        binding.callBanner,
                        ifVisible = true,
                        goneOrInvisible = false
                    )
                }
            }

            stateText.observe(this@CallActivity) {
                val old = binding.callStateText.text.toString()
                binding.callStateText.text = it
                if (old != it) {
                    animations.focus(binding.callStateText)
                }
            }

            uiState.observe(this@CallActivity) {
                when (it) {
                    UIState.MULTI -> {
                        showActiveLayout()
                        binding.callActions.showMultiCallUI()
                    }

                    UIState.ACTIVE -> {
                        showActiveLayout()
                        binding.callActions.showSingleCallUI()
                    }

                    UIState.INCOMING -> {
                        transitionLayoutTo(R.id.constraint_set_incoming_call)
                    }

                    else -> {
                    }
                }
            }

            isHoldEnabled.observe(this@CallActivity) {
                binding.callActions.isHoldEnabled = it
            }

            isMuteEnabled.observe(this@CallActivity) {
                binding.callActions.isMuteEnabled = it
            }

            isSwapEnabled.observe(this@CallActivity) {
                binding.callActions.isSwapEnabled = it
            }

            isMergeEnabled.observe(this@CallActivity) {
                binding.callActions.isMergeEnabled = it
            }

            isManageEnabled.observe(this@CallActivity) {
                binding.callManageButton.isVisible = it
            }

            isSpeakerEnabled.observe(this@CallActivity) {
                binding.callActions.isSpeakerEnabled = it
            }

            isMuteActivated.observe(this@CallActivity) {
                binding.callActions.isMuteActivated = it
            }

            isHoldActivated.observe(this@CallActivity) {
                binding.callActions.isHoldActivated = it
            }

            isSpeakerActivated.observe(this@CallActivity) {
                binding.callActions.isSpeakerActivated = it
            }

            isBluetoothActivated.observe(this@CallActivity) {
                binding.callActions.isBluetoothActivated = it
            }

            askForRouteEvent.observe(this@CallActivity) {
                it.ifNew?.let {
                    dialogs.askForRoute {
                        viewState.onAudioRoutePicked(it)
                        true
                    }
                }
            }

            showDialerEvent.observe(this@CallActivity) {
                it.ifNew?.let { prompts.showFragment(fragmentFactory.getDialerFragment()) }
            }

            showDialpadEvent.observe(this@CallActivity) {
                it.ifNew?.let { prompts.showFragment(fragmentFactory.getDialpadFragment()) }
            }

            showCallManagerEvent.observe(this@CallActivity) {
                it.ifNew?.let { prompts.showFragment(fragmentFactory.getCallItemsFragment()) }
            }

            selectPhoneHandleEvent.observe(this@CallActivity) {
                it.ifNew?.let {
                    dialogs.askForPhoneAccountHandle(it) {
                        viewState.onPhoneAccountHandleSelected(it)
                        true
                    }
                }
            }

            selectPhoneSuggestionEvent.observe(this@CallActivity) {
                it.ifNew?.let {
                    dialogs.askForPhoneAccountSuggestion(it) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            viewState.onPhoneAccountHandleSelected(it.phoneAccountHandle)
                            return@askForPhoneAccountSuggestion true
                        }
                        false
                    }
                }
            }
        }


        binding.apply {
            callActions.setCallActionsListener(viewState)

            callAnswerButton.setOnClickListener {
                viewState.onAnswerClick()
            }

            callRejectButton.setOnClickListener {
                viewState.onRejectClick()
            }

            callManageButton.setOnClickListener {
                viewState.onManageClick()
            }
        }

        dialpadViewState.char.observe(this@CallActivity, viewState::onCharKey)
    }

    /** the caller's name, with their number in smaller text on the line below */
    private fun renderCallerName() {
        val name = callName.orEmpty()
        val number = callNumber?.takeIf { it.isNotBlank() && it != name }

        binding.callNameText.gravity = Gravity.CENTER_HORIZONTAL
        binding.callNameText.text = if (number == null) {
            name
        } else {
            SpannableStringBuilder(name).apply {
                append('\n')
                val start = length
                append(number)
                setSpan(RelativeSizeSpan(0.55f), start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(
                    ForegroundColorSpan(getAttrColor(R.attr.colorOnSurfaceVariant)),
                    start, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun showActiveLayout() {
        transitionLayoutTo(R.id.constraint_set_active_call)
        if (binding.callActions.visibility != View.VISIBLE) {
            animations.show(binding.callActions, true)
        }
    }

    private fun transitionLayoutTo(constraintRes: Int) {
        if (binding.root.currentState != constraintRes) {
            binding.root.setTransition(binding.root.currentState, constraintRes)
            binding.root.transitionToEnd()
        }
    }
}
