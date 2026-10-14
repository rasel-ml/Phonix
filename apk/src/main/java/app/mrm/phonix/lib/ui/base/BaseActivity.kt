package app.mrm.phonix.lib.ui.base

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import app.mrm.phonix.lib.interactor.color.ColorsInteractor
import app.mrm.phonix.lib.interactor.preferences.PreferencesInteractor
import app.mrm.phonix.lib.interactor.string.StringsInteractor
import app.mrm.phonix.lib.interactor.theme.ThemesInteractor
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

abstract class BaseActivity<VM : BaseViewState> : AppCompatActivity(), BaseView<VM> {
    @Inject lateinit var themes: ThemesInteractor
    @Inject lateinit var colors: ColorsInteractor
    @Inject lateinit var strings: StringsInteractor
    @Inject lateinit var disposables: CompositeDisposable
    @Inject lateinit var preferences: PreferencesInteractor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        themes.applyThemeMode(preferences.themeMode)
        contentView?.let { setContentView(it) }

        onSetup()

        viewState.apply {
            attach()

            errorEvent.observe(this@BaseActivity) {
                it.ifNew?.let(this@BaseActivity::showError)
            }

            finishEvent.observe(this@BaseActivity) {
                it.ifNew?.let { finish() }
            }

            messageEvent.observe(this@BaseActivity) {
                it.ifNew?.let(this@BaseActivity::showMessage)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewState.detach()
        disposables.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun finish() {
        finishAndRemoveTask()
    }

    override fun showError(stringResId: Int) {
        Toast.makeText(
            applicationContext,
            strings.getString(stringResId),
            Toast.LENGTH_LONG
        ).show()
    }

    override fun showMessage(stringResId: Int) {
        Toast.makeText(
            this,
            strings.getString(stringResId),
            Toast.LENGTH_SHORT
        ).show()
    }

    open fun <VS : BaseViewState> onFragmentSetup(fragment: BaseFragment<VS>) {}

    abstract val contentView: View?
    abstract override val viewState: VM
}