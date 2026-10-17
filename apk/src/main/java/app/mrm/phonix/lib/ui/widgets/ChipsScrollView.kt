package app.mrm.phonix.lib.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.HorizontalScrollView

/**
 * A horizontal scroll view that keeps its touches away from a parent pager: while a gesture
 * starts inside it, the parents (e.g. the Contacts / Recents ViewPager2) can't steal the
 * horizontal drag, so scrolling the chips never switches the page.
 *
 * It's requested on the way *down* (not only once this view starts scrolling), because the
 * touch usually lands on a chip, which handles it before this view's own touch handling runs.
 */
class ChipsScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.dispatchTouchEvent(ev)
    }
}
