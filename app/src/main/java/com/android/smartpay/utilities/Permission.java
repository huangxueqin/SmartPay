package com.android.smartpay.utilities;

import com.android.smartpay.jsonbeans.LoginResponse;

/**
 * Created by xueqin on 2015/12/2 0002.
 */
public class Permission {
//    permissions;
//    public int user;
//    public int card;
//    public int order;
//    public int product;
//    public int refund;
//    public int cashier;
//    public int pick;
//    public int cash_pay;
//    public int wechat_pay;
//    public int qq_pay;
    private static final int user = 0x1;
    private static final int card = 0x1 << 1;
    private static final int order = 0x1 << 2;
    private static final int product = 0x1 << 3;
    private static final int refund = 0x1 << 4;
    private static final int cashier = 0x1 << 5;
    private static final int pick = 0x1 << 6;
    private static final int cash_pay = 0x1 << 7;
    private static final int wechat_pay = 0x1 << 8;
    private static final int qq_pay = 0x1 << 9;

    public static int buildPermission(LoginResponse.Abilities abilities) {
        int perm = 0;
        if(abilities.user != 0) perm |= user;
        if(abilities.card != 0) perm |= card;
        if(abilities.order != 0) perm |= order;
        if(abilities.product != 0) perm |= product;
        if(abilities.refund != 0) perm |= refund;
        if(abilities.cashier != 0) perm |= cashier;
        if(abilities.pick != 0) perm |= pick;
        if(abilities.cash_pay != 0) perm |= cash_pay;
        if(abilities.wechat_pay != 0) perm |= wechat_pay;
        if(abilities.qq_pay != 0) perm |= qq_pay;
        return perm;
    }

    public static boolean hasPermUser(final int perm) {
        return (perm & user) != 0;
    }

    public static boolean hasPermCard(final int perm) {
        return (perm & card) != 0;
    }

    public static boolean hasPermOrder(final int perm) {
        return (perm & order) != 0;
    }

    public static boolean hasPermProduct(final int perm) {
        return (perm & product) != 0;
    }

    public static boolean hasPermRefund(final int perm) {
        return (perm & refund) != 0;
    }

    public static boolean hasPermCashier(final int perm) {
        return (perm & cashier) != 0;
    }

    public static boolean hasPermPick(final int perm) {
        return (perm & pick) != 0;
    }

    public static boolean hasPermCashPay(final int perm) {
        return (perm & cash_pay) != 0;
    }

    public static boolean hasPermWechatPay(final int perm) {
        return (perm & wechat_pay) != 0;
    }

    public static boolean hasPermQQPay(final int perm) {
        return (perm & qq_pay) != 0;
    }
}
