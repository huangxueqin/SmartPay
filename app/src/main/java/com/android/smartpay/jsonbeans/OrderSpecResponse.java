package com.android.smartpay.jsonbeans;

import java.util.List;

/**
 * Created by xueqin on 2015/12/7 0007.
 */
public class OrderSpecResponse {
    public String errcode;
    public String errmsg;
    public Data data;

    public static class Data {
        public OrderInfo order;
        public List<ProductInfo> product;
        public AdInfo ad;
    }
}
