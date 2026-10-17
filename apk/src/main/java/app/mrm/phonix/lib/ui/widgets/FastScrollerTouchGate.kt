package app.mrm.phonix.lib.ui.widgets

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

/**
 * The fast scroller is an overlay on top of the list that reacts to a touch anywhere along its
 * (full height) right edge. This gate lets it receive a gesture only when it starts on the
 * visible thumb (plus a small touch slop). Any other gesture that starts in that strip, like a
 * tap on a row's quick call button or a normal swipe, is forwarded to the list instead.
 */
class FastScrollerTouchGate(
    private val scroller: View,
    private val list: View
) : View.OnTouchListener {
    private var isForwarding = false
    private val density = scroller.resources.displayMetrics.density
    private val slopX = SLOP_DP * density
    private val slopY = SLOP_DP * density
    private val origin = IntArray(2)
    private val listOrigin = IntArray(2)

    // looked up on the first touch, once the scroller has been inflated and laid out
    private val handle: View? by lazy { findHandle() }

    fun attach() = scroller.setOnTouchListener(this)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            isForwarding = !isOnThumb(event)
        }
        if (!isForwarding) return false // the scroller handles it as usual

        v.getLocationOnScreen(origin)
        list.getLocationOnScreen(listOrigin)
        val forwarded = MotionEvent.obtain(event)
        forwarded.offsetLocation(
            (origin[0] - listOrigin[0]).toFloat(),
            (origin[1] - listOrigin[1]).toFloat()
        )
        list.dispatchTouchEvent(forwarded)
        forwarded.recycle()
        return true
    }

    private fun isOnThumb(event: MotionEvent): Boolean {
        // if the thumb can't be found, keep the scroller's default behaviour
        val thumb = handle ?: return true
        if (thumb.visibility != View.VISIBLE) return false

        thumb.getLocationOnScreen(origin)
        return event.rawX >= origin[0] - slopX && event.rawX <= origin[0] + thumb.width + slopX &&
                event.rawY >= origin[1] - slopY && event.rawY <= origin[1] + thumb.height + slopY
    }

    private fun findHandle(): View? {
        val id = scroller.resources.getIdentifier(
            "fastscroll__handle", "id", scroller.context.packageName
        )
        if (id != 0) scroller.findViewById<View>(id)?.let { return it }
        return firstImage(scroller)
    }

    private fun firstImage(view: View): View? {
        if (view is ImageView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) firstImage(view.getChildAt(i))?.let { return it }
        }
        return null
    }

    companion object {
        private const val SLOP_DP = 8
    }
}
