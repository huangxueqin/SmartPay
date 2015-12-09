package com.android.smartpay.jsonbeans;

/**
 * Created by xueqin on 2015/11/29 0029.
 */
public class ParseBeanException extends Exception{
    public ParseBeanException() {
        super("json string is an error response");
    }
}
