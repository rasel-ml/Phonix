package app.mrm.phonix.lib.ui.widgets

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * The fast scroller is an overlay on top of the list (both are children of the same
 * FastScrollView) that starts scrolling for a touch anywhere along its full-height strip.
 *
 * This gate lets the scroller receive a gesture only when it starts on the visible thumb (plus a
 * small touch slop). Any other gesture that starts in that strip, like a tap on a row's quick
 * call button or a normal swipe, is forwarded to the list's RecyclerView instead.
 */
class FastScrollerTouchGate(private val scroller: View) : View.OnTouchListener {
    private var isForwarding = false
    private var isDispatching = false
    private val density = scroller.resources.displayMetrics.density
    private val slop = SLOP_DP * density
    private val location = IntArray(2)
    private val listLocation = IntArray(2)

    fun attach() = scroller.setOnTouchListener(this)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        // never re-enter through our own forwarding
        if (isDispatching) return false

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            isForwarding = !isOnThumb(event)
        }
        if (!isForwarding) return false // the scroller handles it as usual

        // the RecyclerView itself, not its parent: the parent also contains this scroller
        val list = findList() ?: return false

        v.getLocationOnScreen(location)
        list.getLocationOnScreen(listLocation)
        val forwarded = MotionEvent.obtain(event)
        forwarded.offsetLocation(
            (location[0] - listLocation[0]).toFloat(),
            (location[1] - listLocation[1]).toFloat()
        )
        isDispatching = true
        try {
            list.dispatchTouchEvent(forwarded)
        } finally {
            isDispatching = false
            forwarded.recycle()
        }
        return true
    }

    private fun isOnThumb(event: MotionEvent): Boolean {
        // if the thumb can't be found, keep the scroller's default behaviour
        val thumb = findHandle() ?: return true
        if (thumb.visibility != View.VISIBLE) return false

        thumb.getLocationOnScreen(location)
        return event.rawX >= location[0] - slop && event.rawX <= location[0] + thumb.width + slop &&
                event.rawY >= location[1] - slop && event.rawY <= location[1] + thumb.height + slop
    }

    private fun findHandle(): View? {
        val id = scroller.resources.getIdentifier(
            "fastscroll_handle", "id", scroller.context.packageName
        )
        return if (id != 0) scroller.findViewById(id) else null
    }

    private fun findList(): RecyclerView? {
        val parent = scroller.parent as? ViewGroup ?: return null
        for (i in 0 until parent.childCount) {
            (parent.getChildAt(i) as? RecyclerView)?.let { return it }
        }
        return null
    }

    companion object {
        private const val SLOP_DP = 8
    }
}
