package com.openlist.app.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

/**
 * A DrawerLayout subclass that, when the drawer is locked open (tablet mode),
 * does NOT intercept touch events on the main content area.
 * This fixes the issue where the locked-open drawer consumes clicks in the content area.
 */
class NonInterceptingDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : DrawerLayout(context, attrs, defStyleAttr) {

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // When the drawer is locked open (tablet side-nav mode), don't intercept —
        // let all touch events fall through to child views normally.
        if (getDrawerLockMode(GravityCompat.START) == LOCK_MODE_LOCKED_OPEN) {
            return false
        }
        return super.onInterceptTouchEvent(ev)
    }
}
