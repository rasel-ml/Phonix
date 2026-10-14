package app.mrm.phonix.lib.interactor.prompt

import androidx.fragment.app.FragmentManager
import app.mrm.phonix.lib.interactor.base.BaseInteractorImpl
import app.mrm.phonix.lib.ui.base.BaseFragment
import app.mrm.phonix.lib.ui.base.BottomFragment
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class PromptsInteractorImpl @Inject constructor(
    private val fragmentManager: FragmentManager
) : BaseInteractorImpl<PromptsInteractor.Listener>(),
    PromptsInteractor {

    override fun showFragment(fragment: BaseFragment<*>, tag: String?) {
        BottomFragment(fragment).show(fragmentManager, tag)
    }
}