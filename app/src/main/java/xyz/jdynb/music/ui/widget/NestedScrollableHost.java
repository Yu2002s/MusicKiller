package xyz.jdynb.music.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

public class NestedScrollableHost extends FrameLayout {
    private int touchSlop = 0;
    private float initialX = 0f;
    private float initialY = 0f;

    public NestedScrollableHost(@NonNull Context context) {
        super(context, null);
    }

    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("NewApi")
    public NestedScrollableHost(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

    }

    public final ViewPager2 getParentViewPager() {
        ViewParent parent = getParent();
        if (!(parent instanceof View)) {
            parent = null;
        }

        View v;
        for (v = (View) parent; v != null && !(v instanceof ViewPager2); v = (View) parent) {
            parent = v.getParent();
            if (!(parent instanceof View)) {
                parent = null;
            }
        }

        View viewPager = v;
        return (ViewPager2) viewPager;

    }

    public View getChildView() {
        return this.getChildCount() > 0 ? this.getChildAt(0) : null;
    }

    @SuppressLint("NewApi")
    private boolean canChildScroll(int orientation, Float delta) {
        View child = getChildView();
        if (child != null) {
            int direction = -(int) Math.signum(delta);
            switch (orientation) {
                case 0:
                    return child.canScrollHorizontally(direction);
                case 1:
                    return child.canScrollVertically(direction);
            }
        }
        return false;

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        handleInterceptTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }


    private void handleInterceptTouchEvent(MotionEvent e) {
        ViewPager2 parentViewPager = getParentViewPager();
        if (parentViewPager != null) {
            int orientation = parentViewPager.getOrientation();
            if (canChildScroll(orientation, -1.0f) || canChildScroll(orientation, 1.0f)) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    initialX = e.getX();
                    initialY = e.getY();
                    getParent().requestDisallowInterceptTouchEvent(true);
                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    float dx = e.getX() - initialX;
                    float dy = e.getY() - initialY;
                    boolean isVpHorizontal = orientation == ViewPager2.ORIENTATION_HORIZONTAL;

                    // assuming ViewPager2 touch-slop is 2x touch-slop of child
                    float scaledDx = Math.abs(dx) * (isVpHorizontal ? 0.5f : 1f);
                    float scaledDy = Math.abs(dy) * (isVpHorizontal ? 1f : 0.5f);

                    if (scaledDx > touchSlop || scaledDy > touchSlop) {
                        if (isVpHorizontal == scaledDy > scaledDx) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        } else getParent().requestDisallowInterceptTouchEvent(canChildScroll(orientation, isVpHorizontal ? dx : dy));
                    }
                }

            }
        }
    }

}