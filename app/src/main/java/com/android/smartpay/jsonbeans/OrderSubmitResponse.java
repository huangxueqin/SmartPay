package com.android.smartpay.jsonbeans;

/**
 * Created by xueqin on 2015/12/2 0002.
 */
public class OrderSubmitResponse {
    public String errcode;
    public String errmsg;
    public Data data;

    public static class Data {
        public OrderInfo order;
        public AdInfo ad;
    }

    public static OrderSubmitResponse sOrderSubmitResponse;

    public static void initTest() {
        sOrderSubmitResponse = new OrderSubmitResponse();
        sOrderSubmitResponse.data = new Data();
        sOrderSubmitResponse.data.order = new OrderInfo();
        sOrderSubmitResponse.data.ad = new AdInfo();
        sOrderSubmitResponse.errcode = "0";
        sOrderSubmitResponse.errmsg = "ok";
        sOrderSubmitResponse.data.order.id = "2";
        sOrderSubmitResponse.data.order.orderno = "2015031813505010334841";
//        sOrderSubmitResponse.data.order.seller_cut = 0;
//        sOrderSubmitResponse.data.order.delivery_fee = 0;
//        sOrderSubmitResponse.data.order.should_pay = 1980;
//        sOrderSubmitResponse.data.order.point_discount = 0;
//        sOrderSubmitResponse.data.order.totalprice = 2000;
        sOrderSubmitResponse.data.order.status = 1;
        sOrderSubmitResponse.data.order.paytype = String.valueOf(2);
        sOrderSubmitResponse.data.order.createtime = "2015-2-05 10:52:09";
        sOrderSubmitResponse.data.order.coupon_name = "满100减20";
//        sOrderSubmitResponse.data.order.discount = 20;

        sOrderSubmitResponse.data.ad.id = "2";
        sOrderSubmitResponse.data.ad.type = 1;
        sOrderSubmitResponse.data.ad.content = "我要赢";
    }



















}
