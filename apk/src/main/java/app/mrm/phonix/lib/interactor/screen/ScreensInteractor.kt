package app.mrm.phonix.lib.interactor.screen

import android.view.MotionEvent
import app.mrm.phonix.lib.interactor.base.BaseInteractor

interface ScreensInteractor : BaseInteractor<ScreensInteractor.Listener> {
    interface Listener

    fun showWhenLocked()
    fun disableKeyboard()
    fun ignoreEditTextFocus(event: MotionEvent)
}