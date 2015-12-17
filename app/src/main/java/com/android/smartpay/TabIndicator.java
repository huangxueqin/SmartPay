package com.android.smartpay;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.android.smartpay.fragments.BaseFragment;


/**
 * Created by xueqin on 2015/11/26 0026.
 */
public class TabIndicator extends LinearLayout {
    public static final String TAG = "TAG---------->";
    private boolean mSelected = false;
    private BaseFragment mAttachedFragment;
    private int mIconDrawableId = -1;
    private IconView mIcon;

    public TabIndicator(Context context) {
        this(context, null);
    }

    public TabIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TabIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setGravity(Gravity.CENTER);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TabIndicator);
        mIconDrawableId = ta.getResourceId(R.styleable.TabIndicator_icon_drawable, -1);
        ta.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if(getChildCount() > 0) {
            mIcon = (IconView) getChildAt(0);
            mIcon.setFocusable(false);
            if(mIconDrawableId != -1) {
                mIcon.setImageResource(mIconDrawableId);
            }
        }
    }

    public void setIcon(int resId) {
        mIconDrawableId = resId;
        if(mIcon != null) {
            mIcon.setImageResource(mIconDrawableId);
        }
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
}
