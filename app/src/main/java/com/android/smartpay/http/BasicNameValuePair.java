package com.android.smartpay.http;

/**
 * Created by xueqin on 2015/12/15 0015.
 */
public class BasicNameValuePair {
    public String name;
    public String value;

    public BasicNameValuePair(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
