package com.android.smartpay.jsonbeans;

import java.util.ArrayList;

/**
 * Created by xueqin on 2015/12/7 0007.
 */
public class OrderPayParam {
    public static class PayInfoItem {
        public String type;
        public String total;
        public String auth_code;
    }
    public ArrayList<PayInfoItem> payinfo = new ArrayList<>();
    public String shop_user_id;
    public String platform;
    public String seller_id;
    public String shop_id;
    public String order_id;

}
