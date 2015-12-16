package com.android.smartpay;

import android.content.Intent;

import com.android.smartpay.http.HttpService;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.OrderSubmitResponse;
import com.android.smartpay.jsonbeans.TokenResponse;

/**
 * Created by xueqin on 2015/12/1 0001.
 */
public class Application extends android.app.Application {
    public static final boolean GSON_ENABLE = true;

    public static final boolean NO_NETWORK_DEBUG = false;
    public static final boolean TEST_DEBUG = false;
    public static final boolean NORMAL_DEBUG = true;
    HttpService httpService;
    DataLoader loader;
    Intent ps;
    @Override
    public void onCreate() {
        super.onCreate();
        httpService = HttpService.get();
        loader = DataLoader.get();

        if(TEST_DEBUG) {
            TokenResponse.initTest();
            LoginResponse.initTest();
            OrderSubmitResponse.initTest();
        }
    }
}
