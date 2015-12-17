package com.android.smartpay.utilities;

/**
 * Created by xueqin on 2015/12/4 0004.
 */
public class Cons {
    public static final int TYPE_DAY = 0xF0-1;
    public static final int TYPE_TODAY = 0xF0;
    public static final int TYPE_WEEK = 0xF1;
    public static final int TYPE_MONTH = 0xF2;
    public static final int TYPE_SEVEN = 0xF3;
    public static final int TYPE_THIRTY = 0xF4;

    // cons for main activity
    public static final int REQUEST_LOGIN = 0xFF0;
    public static final int REQUEST_SCAN = 0xFF1;
    public static final int REQUEST_ORDER_RESULT = 0xFF2;
    public static final int REQUEST_ORDER_LIST = 0xFF3;
    public static final int REQUEST_STATISTICS = 0xFF4;
    public static final int REQUEST_ORDER_SPEC = 0xFF5;

    // actions for capture activity
    public static final int ACTION_WECHAT_PAY = 0xFFF0;
    public static final int ACTION_QQ_PAY = 0xFFF1;
    // actions for pay success activity
    public static final int ACTION_CASHIER = 0xFFF3;
    public static final int ACTION_RECORD = 0xFFF4;
    // actions for record fragment
    public static final int ACTION_STATISTICS = 0xFFF5;
    public static final int ACTION_ORDER_LIST = 0xFFF6;
    public static final int ACTION_MOST_RECENT = 0xFFF7;
    // actions for setting fragment
    public static final int ACTION_LOGOUT = 0xFFF8;

    public static final String ARG_ACTION = "action";
    public static final String ARG_MONEY = "money";
    public static final String ARG_SCAN_RESULT = "scan_result";
    public static final String ARG_PAY_TYPE = "pay_type";
    public static final String ARG_PERM = "permission";
    public static final String ARG_USER = "user";

    // arg for pay success activity
    public static final String ARG_CREATE_TIME = "create_time";
    public static final String ARG_TOTAL_MONEY = "total_money";

    // args for record fragment
    public static final String ARG_LIST_TYPE = "list_type";

    // args for order specific activity
    public static final String ARG_ORDER = "order";

    // args for order list activity
    public static final String ARG_DATE = "date";

    // message magic numbers
    public static final int MSG_ORDER_SUBMIT_SUCCESS = 0xFFFF0;
    public static final int MSG_ORDER_SUBMIT_FAILED = 0xFFFF1;
    public static final int MSG_ORDER_PAY_SUCCESS = 0xFFFF2;
    public static final int MSG_ORDER_PAY_FAILED = 0xFFFF3;
    public static final int MSG_CANCEL_ORDER_PAY = 0xFFFF4;

}
