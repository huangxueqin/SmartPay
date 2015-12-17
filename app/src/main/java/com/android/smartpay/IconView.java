package com.android.smartpay;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by xueqin on 2015/12/17 0017.
 */
public class IconView extends ImageView {
    private final static int[] STATE_SELECTED = { R.attr.state_selected};
    private boolean mSelected = false;

    public IconView(Context context) {
        this(context, null);
    }

    public IconView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSelect(boolean select) {
        mSelected = select;
        refreshDrawableState();
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (mSelected) {
            final int[] drawableState = super.onCreateDrawableState(extraSpace + STATE_SELECTED.length);
            mergeDrawableStates(drawableState, STATE_SELECTED);
            return drawableState;
        }
        else {
            return super.onCreateDrawableState(extraSpace);
        }
    }
}
