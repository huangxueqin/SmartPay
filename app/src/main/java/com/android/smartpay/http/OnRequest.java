package com.android.smartpay.http;

import android.os.AsyncTask;

/**
 * Created by xueqin on 2015/11/29 0029.
 * All callback functions will be called in the executing thread.
 * so be careful If you want to do things that must within the UI thread
 */
public interface OnRequest<T> {
    void onComplete(T t);
    void onFail(String code, String msg);
}
