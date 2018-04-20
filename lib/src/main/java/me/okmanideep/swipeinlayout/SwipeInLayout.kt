package me.okmanideep.swipeinlayout

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.support.v4.view.ViewCompat
import android.support.v4.widget.ViewDragHelper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout


class SwipeInLayout
@JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attributeSet, defStyleAttr) {
    private var isExpanded: Boolean = false
    private val dragCallback = SwipeInLayoutDragCallback(this)
    private val dragHelper: ViewDragHelper
    private var slideEdge = SlideEdge.LEFT
    private var collapsedWidth: Int = 0

    init {
        dragHelper = ViewDragHelper.create(this, dragCallback)
        if (attributeSet != null) {
            val attrs = context.theme.obtainStyledAttributes(attributeSet, R.styleable.SwipeInLayout, 0, 0)
            try {
                val slideEdgeAttr = attrs.getInt(R.styleable.SwipeInLayout_slideEdge, 0)
                slideEdge = if (slideEdgeAttr == SlideEdge.LEFT.ordinal) SlideEdge.LEFT else SlideEdge.RIGHT
                isExpanded = attrs.getBoolean(R.styleable.SwipeInLayout_expanded, false)
                collapsedWidth = attrs.getDimensionPixelSize(R.styleable.SwipeInLayout_collapsedWidth, 0)
            } finally {
                attrs.recycle()
            }
        }
    }

    fun expand(animate: Boolean = true) {
        isExpanded = true
        val child = getChild() ?: return
        if (animate) {
            val finalLeft = if (slideEdge.isLeft()) maxLeft() else minLeft()
            dragHelper.smoothSlideViewTo(child, finalLeft, child.top)
        } else {
            requestLayout()
        }
    }

    fun collapse(animate: Boolean = true) {
        isExpanded = false
        val child = getChild() ?: return
        if (animate) {
            val finalLeft = if (slideEdge.isLeft()) minLeft() else maxLeft()
            dragHelper.smoothSlideViewTo(child, finalLeft, child.top)
        } else {
            requestLayout()
        }
    }

    internal fun getChild(): View? {
        return getChildAt(0)
    }

    internal fun onViewReleased(child: View, xvel: Float, yvel: Float) {
        val currentLeft = child.left
        val minLeft = minLeft()
        val maxLeft = maxLeft()
        val mid = (minLeft + maxLeft) / 2
        val finalLeft = if (currentLeft < mid) {
            val minVel = (maxLeft - currentLeft) / 0.25f
            if (xvel > minVel) maxLeft else minLeft
        } else {
            // negative velocities
            val minVel = (minLeft - currentLeft) / 0.25f
            if (xvel < minVel) minLeft else maxLeft
        }
        isExpanded = (finalLeft == maxLeft && slideEdge.isLeft()) ||
                (finalLeft == minLeft && slideEdge.isRight())
        if (dragHelper.settleCapturedViewAt(finalLeft, child.top)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return dragHelper.shouldInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        super.onTouchEvent(ev)
        dragHelper.processTouchEvent(ev)
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val child = getChild() ?: return
        if (isExpanded) {
            child.left = paddingLeft + (child.layoutParams as FrameLayout.LayoutParams).leftMargin
        } else {
            if (slideEdge.isLeft()) {
                child.offsetLeftAndRight(collapsedWidth - child.width)
            } else {
                child.offsetLeftAndRight(child.width - collapsedWidth)
            }
        }
    }

    override fun addView(child: View?) {
        super.addView(child)
        onChildrenChanged()
    }

    override fun addView(child: View?, index: Int) {
        super.addView(child, index)
        onChildrenChanged()
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        super.addView(child, params)
        onChildrenChanged()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        onChildrenChanged()
    }

    override fun addView(child: View?, width: Int, height: Int) {
        super.addView(child, width, height)
        onChildrenChanged()
    }

    override fun addViewInLayout(child: View?, index: Int, params: ViewGroup.LayoutParams?): Boolean {
        val ret = super.addViewInLayout(child, index, params)
        onChildrenChanged()
        return ret
    }

    override fun addViewInLayout(child: View?, index: Int, params: ViewGroup.LayoutParams?, preventRequestLayout: Boolean): Boolean {
        val ret = super.addViewInLayout(child, index, params, preventRequestLayout)
        onChildrenChanged()
        return ret
    }

    override fun computeScroll() {
        super.computeScroll()
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private fun onChildrenChanged() {
        if (childCount > 1) {
            throw IllegalStateException("SwipeInLayout cannot have more than one child")
        }
    }

    internal fun minLeft(): Int {
        val child = getChild() ?: return 0

        return if (slideEdge.isLeft()) {
            collapsedWidth - child.width
        } else {
            paddingLeft + (child.layoutParams as FrameLayout.LayoutParams).leftMargin
        }
    }

    internal fun maxLeft(): Int {
        return if (slideEdge.isLeft()) {
            paddingLeft
        } else {
            width - collapsedWidth
        }
    }

    internal fun childTop(): Int {
        val child = getChild() ?: return paddingTop
        return paddingTop + (child.layoutParams as FrameLayout.LayoutParams).topMargin
    }
}

internal enum class SlideEdge {
    LEFT, RIGHT;

    fun isLeft() = this == LEFT

    fun isRight() = this == RIGHT
}
