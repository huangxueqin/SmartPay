package com.android.smartpay.utilities;

import com.android.smartpay.jsonbeans.OrderInfo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by xueqin on 2015/12/9 0009.
 */
public class OrderUtils {
    public static String getExactStatusString(int status) {
        switch (status) {
            case 0:
                return "未付款";
            case 1:
                return "已付款等待发货";
            case 2:
                return "已发货等待确认收货";
            case 3:
                return "已收货待评价";
            case 4:
                return "订单关闭";
            case 5:
                return "交易完成";
            case 6:
                return "订单已取消";
            case 7:
                return "维权订单";
            default:
                return "未知订单";
        }
    }

    public static boolean isOrderPaid(OrderInfo order) {
        int status = order.status;
        return status == 1 || status == 2 || status == 3 || status == 5 || status == 7;
    }

    public static String getOrderSpec(OrderInfo order) {
        return order.paytype + "-" + order.orderno;
    }

    public static Date getOrderDate(OrderInfo info) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            Date date = formatter.parse(info.createtime);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
