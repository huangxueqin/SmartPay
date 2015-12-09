package com.android.smartpay;

import android.content.Intent;

import com.android.smartpay.jsonbeans.LoginResponse;


/**
 * Created by xueqin on 2015/11/26 0026.
 */
public interface FragmentListener {
    void onEvent(FragmentCallback callback, int action, Intent data);
}
