package com.android.smartpay;

import com.android.smartpay.http.HttpService;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.OrderSubmitResponse;
import com.android.smartpay.jsonbeans.TokenResponse;

/**
 * Created by xueqin on 2015/12/1 0001.
 */
public class Application extends android.app.Application {
    public static final boolean NO_NETWORK_DEBUG = false;
    public static final boolean TEST_DEBUG = true;
    public static final boolean NORMAL_DEBUG = true;
    HttpService httpService;
    DataLoader loader;
    @Override
    public void onCreate() {
        super.onCreate();
        HttpService.setApplicationContext(this);
        httpService = HttpService.get();
        loader = DataLoader.get();
        if(TEST_DEBUG) {
            TokenResponse.initTest();
            LoginResponse.initTest();
            OrderSubmitResponse.initTest();
        }
    }

    @Override
    public void onTerminate() {
        httpService.close();
        if(!loader.isLoadComplete()) {
            loader.cancelLoad();
        }
        super.onTerminate();
    }
}
