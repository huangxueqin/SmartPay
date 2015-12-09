package com.android.smartpay.http;

import android.os.AsyncTask;

/**
 * Created by xueqin on 2015/11/29 0029.
 */
public interface OnRequest<T> {
    void onNoConnection();
    void onComplete(T t);
    void onFail(String code, String msg);
}
