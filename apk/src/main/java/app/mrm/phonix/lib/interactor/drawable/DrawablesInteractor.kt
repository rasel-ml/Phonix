package app.mrm.phonix.lib.interactor.drawable

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes

interface DrawablesInteractor {
    interface Listener

    fun getDrawable(@DrawableRes res: Int): Drawable?
}