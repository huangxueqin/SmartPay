package com.android.smartpay.fragments;

import android.content.Intent;
import android.support.v4.app.Fragment;

import com.android.smartpay.FragmentCallback;
import com.android.smartpay.FragmentListener;
import com.android.smartpay.TabIndicator;

/**
 * Created by xueqin on 2015/11/26 0026.
 */
public abstract class BaseFragment extends Fragment implements FragmentCallback {
    protected FragmentListener mListener;
    protected TabIndicator mAttachedTab;

    public void setTab(TabIndicator tab) {
        mAttachedTab = tab;
    }
    public TabIndicator getAttachedTab() {
        return mAttachedTab;
    }

    public void setFragmentListener(FragmentListener listener) {
        mListener = listener;
    }
}
