package app.mrm.phonix.lib.interactor.color

import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import app.mrm.phonix.lib.interactor.base.BaseInteractor

interface ColorsInteractor : BaseInteractor<ColorsInteractor.Listener> {
    interface Listener

    fun getColor(@ColorRes colorRes: Int): Int
    fun getAttrColor(@AttrRes colorRes: Int): Int
}