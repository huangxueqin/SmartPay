package com.android.smartpay;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import com.android.smartpay.fragments.BaseFragment;


/**
 * Created by xueqin on 2015/11/26 0026.
 */
public class TabIndicator extends LinearLayout {
    public static final String TAG = "TAG---------->";
    private boolean mSelected = false;
    private BaseFragment mAttachedFragment;

    private Icon mIcon;
    private int mIconDrawableId = -1;

    public TabIndicator(Context context) {
        this(context, null);
    }

    public TabIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if(attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TabIndicator);
            mIconDrawableId = ta.getResourceId(R.styleable.TabIndicator_icon_drawable, -1);
            ta.recycle();
        }
        setGravity(Gravity.CENTER);
        if(mIconDrawableId != -1) {
            mIcon = new Icon(context);
            mIcon.setFocusable(false);
            mIcon.setImageResource(mIconDrawableId);
            LayoutParams lp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            addView(mIcon, lp);
        }
    }

    public void setIcon(int resId) {
        mIconDrawableId = resId;
        if(mIcon != null) {
            removeView(mIcon);
        }
        mIcon = new Icon(getContext());
        mIcon.setFocusable(false);
        mIcon.setImageResource(mIconDrawableId);
        LayoutParams lp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addView(mIcon, lp);
    }

    public void setFragment(BaseFragment fragment) {
        mAttachedFragment = fragment;
    }

    public BaseFragment getAttachedFragment() {
        return mAttachedFragment;
    }

    public void setSelect(boolean select) {
        if(mSelected != select) {
            mSelected = select;
            if(mIcon != null) {
                mIcon.setSelect(mSelected);
            }
        }
    }

    private static class Icon extends ImageView {
        private final static int[] STATE_SELECTED = { R.attr.state_selected};
        private boolean mSelected = false;

        public Icon(Context context) {
            this(context, null);
        }

        public Icon(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public Icon(Context context, AttributeSet attrs, int defStyleAttr) {
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
}
