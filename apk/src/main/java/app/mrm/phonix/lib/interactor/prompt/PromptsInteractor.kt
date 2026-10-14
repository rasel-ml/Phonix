package app.mrm.phonix.lib.interactor.prompt

import app.mrm.phonix.lib.interactor.base.BaseInteractor
import app.mrm.phonix.lib.ui.base.BaseFragment

interface PromptsInteractor : BaseInteractor<PromptsInteractor.Listener> {
    interface Listener

    fun showFragment(fragment: BaseFragment<*>, tag: String? = null)
}