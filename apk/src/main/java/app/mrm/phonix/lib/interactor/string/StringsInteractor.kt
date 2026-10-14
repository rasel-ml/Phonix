package app.mrm.phonix.lib.interactor.string

import androidx.annotation.StringRes
import app.mrm.phonix.lib.interactor.base.BaseInteractor

interface StringsInteractor : BaseInteractor<StringsInteractor.Listener> {
    interface Listener

    fun getString(@StringRes stringRes: Int): String
}