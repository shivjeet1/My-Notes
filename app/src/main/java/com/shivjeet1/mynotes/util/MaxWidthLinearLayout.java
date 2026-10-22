package com.shivjeet1.mynotes.util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.shivjeet1.mynotes.R;

/**
 * A vertical container that never grows wider than the reading width on
 * tablets, while still filling the screen on phones.
 */
public class MaxWidthLinearLayout extends LinearLayout {

    private int maxWidth;

    public MaxWidthLinearLayout(Context context) {
        this(context, null);
    }

    public MaxWidthLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaxWidthLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        maxWidth = getResources().getDimensionPixelSize(R.dimen.content_max_width);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int size = MeasureSpec.getSize(widthMeasureSpec);
        if (size > maxWidth && (mode == MeasureSpec.EXACTLY || mode == MeasureSpec.AT_MOST)) {
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, mode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
