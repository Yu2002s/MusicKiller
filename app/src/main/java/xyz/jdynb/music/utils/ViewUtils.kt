package xyz.jdynb.music.utils

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun RecyclerView.removeAllItemDecorator(): RecyclerView {
  for (i in itemDecorationCount - 1 downTo 0 ) {
    removeItemDecorationAt(i)
  }
  return this
}

/**
 * 修复嵌套滚动出现的问题
 */
@SuppressLint("ClickableViewAccessibility")
fun ViewPager2.fixNestedScroll(): ViewPager2 {
  (getChildAt(0) as? RecyclerView)?.let { rv ->
    rv.isNestedScrollingEnabled = false
    rv.overScrollMode = View.OVER_SCROLL_NEVER
    var startX = 0f
    var startY = 0f

    rv.setOnTouchListener { v, e ->
      when (e.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          startX = e.x
          startY = e.y
          v.parent.requestDisallowInterceptTouchEvent(false)
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = e.x - startX
          val dy = e.y - startY
          if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
            // Vertical: let BottomSheetBehavior intercept
            v.parent.requestDisallowInterceptTouchEvent(false)
          } else {
            // Horizontal: keep for ViewPager2 to page
            v.parent.requestDisallowInterceptTouchEvent(true)
          }
        }
      }
      false
    }
  }
  return this
}

@SuppressLint("ClickableViewAccessibility")
fun View.fixNestedScroll(): View {
  isNestedScrollingEnabled = false

  var startX = 0f
  var startY = 0f

  setOnTouchListener { v, e ->
    when (e.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        startX = e.x
        startY = e.y
        v.parent.requestDisallowInterceptTouchEvent(false)
      }
      MotionEvent.ACTION_MOVE -> {
        val dx = e.x - startX
        val dy = e.y - startY
        if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) {
          // Vertical: let BottomSheetBehavior intercept
          v.parent.requestDisallowInterceptTouchEvent(false)
        } else {
          // Horizontal: keep for ViewPager2 to page
          v.parent.requestDisallowInterceptTouchEvent(true)
        }
      }
    }
    false
  }

  return this
}