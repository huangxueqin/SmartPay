package com.android.smartpay.jsonbeans;

import com.google.gson.Gson;

/**
 * Created by xueqin on 2015/12/1 0001.
 */
public class LoginResponse {
    public String errcode;
    public String errmsg;
    public Data data;

    public static class Data {
        public Token token;
        public ShopUser shop_user;
        public Abilities abilities;
    }

    public static class Token {
        public String access_token;
        public String refresh_token;
        public long expires_in;
    }

    public static class Abilities {
        public int user;
        public int card;
        public int order;
        public int product;
        public int refund;
        public int cashier;
        public int pick;
        public int cash_pay;
        public int wechat_pay;
        public int qq_pay;
    }
    public static class ShopUser {
        public String id;
        public String real_name;
        public String mobile;
        public String shop_id;
        public String shop_name;
        public String seller_id;
        public String seller_name;
        public String address;
        public String phone;
        public String qrcode_img;
    }

    static LoginResponse createInstance(String json) throws ParseBeanException {
        LoginResponse response = new Gson().fromJson(json, LoginResponse.class);
        if(response.errcode != null && !response.errcode.equals("0")) {
            throw new ParseBeanException();
        }
        return response;
    }

    public static LoginResponse sTestLoginResponse;
    public static LoginResponse.ShopUser sTestUser;
    public static void initTest() {
        sTestLoginResponse = new LoginResponse();
        sTestLoginResponse.errcode = "0";
        sTestLoginResponse.errmsg = "ok";
        sTestLoginResponse.data = new Data();
        sTestLoginResponse.data.shop_user = new ShopUser();
        sTestLoginResponse.data.abilities = new Abilities();
        sTestLoginResponse.data.token = new Token();

        sTestLoginResponse.data.token.access_token = "0ccb9dbeb7bb5351f7b181a04868587c";
        sTestLoginResponse.data.token.refresh_token = "2d6a9058d54beed9b436261fbb87cce9";
        sTestLoginResponse.data.token.expires_in = 7200;

        sTestLoginResponse.data.shop_user.id = "100";
        sTestLoginResponse.data.shop_user.real_name = "test";
        sTestLoginResponse.data.shop_user.mobile = "18694045201";
        sTestLoginResponse.data.shop_user.shop_id = "100";
        sTestLoginResponse.data.shop_user.shop_name = "深圳分店";
        sTestLoginResponse.data.shop_user.seller_id = "12";
        sTestLoginResponse.data.shop_user.seller_name = "美宜佳";
        sTestLoginResponse.data.shop_user.address = "腾讯大厦";
        sTestLoginResponse.data.shop_user.phone = "0755-2656748";
        sTestLoginResponse.data.shop_user.qrcode_img = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=gQE48DoAAAAAAAAAASxodHRwOi8vd2VpeGluLnFxLmNvbS9xL2dYUUhUM0xtcW9iVE53alFBMWg3AAIEYvTtUwMEAAAAAA==";

        sTestLoginResponse.data.abilities.user = 0;
        sTestLoginResponse.data.abilities.card = 1;
        sTestLoginResponse.data.abilities.order = 1;
        sTestLoginResponse.data.abilities.product = 1;
        sTestLoginResponse.data.abilities.refund = 1;
        sTestLoginResponse.data.abilities.cashier = 0;
        sTestLoginResponse.data.abilities.pick = 1;
        sTestLoginResponse.data.abilities.cash_pay = 0;
        sTestLoginResponse.data.abilities.wechat_pay = 1;
        sTestLoginResponse.data.abilities.qq_pay = 0;

        sTestUser = new LoginResponse.ShopUser();
        sTestUser.id = "100";
        sTestUser.real_name = "小张";
        sTestUser.mobile = "18694045201";
        sTestUser.shop_id = "100";
        sTestUser.shop_name = "深圳分店";
        sTestUser.seller_id = "12";
        sTestUser.seller_name = "美宜佳";
        sTestUser.address = "腾讯大厦";
        sTestUser.phone = "0755-2656748";
        sTestUser.qrcode_img = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=gQE48DoAAAAAAAAAASxodHRwOi8vd2VpeGluLnFxLmNvbS9xL2dYUUhUM0xtcW9iVE53alFBMWg3AAIEYvTtUwMEAAAAAA==";
    }
}

