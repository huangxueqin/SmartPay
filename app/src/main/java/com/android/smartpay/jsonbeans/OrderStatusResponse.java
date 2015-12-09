package com.android.smartpay.jsonbeans;

/**
 * Created by xueqin on 2015/12/7 0007.
 */
public class OrderStatusResponse {
    public String errcode;
    public String errmsg;
    public Data data;

    public static class Data {
        public OrderInfo order;
    }
}
