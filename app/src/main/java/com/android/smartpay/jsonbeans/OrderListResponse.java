package com.android.smartpay.jsonbeans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xueqin on 2015/12/8 0008.
 */
public class OrderListResponse {
    public String errcode;
    public String errmsg;
    public Data data;
    public int total_count;
    public int per_page;
    public int current_page;
    public int total_page;

    public static class Data {
        public ArrayList<OrderInfo> order;
    }
}
