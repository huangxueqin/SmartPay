package com.android.smartpay.jsonbeans;

import com.google.gson.Gson;

/**
 * Created by xueqin on 2015/11/30 0030.
 */
public class ErrorResponse {
    public String errcode;
    public String errmsg;

    public static ErrorResponse obtainInstance(String json) {
        ErrorResponse err = new ErrorResponse();

        return err;
    }
}
