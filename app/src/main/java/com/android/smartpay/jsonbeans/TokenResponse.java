package com.android.smartpay.jsonbeans;

/**
 * Created by xueqin on 2015/12/1 0001.
 */
public class TokenResponse {
    public String errcode;
    public String errmsg;
    public Data data;
    public static class Data {
        public String access_token;
        public String refresh_token;
        public long expires_in;
    }
    public static TokenResponse sTestTokenResponse;

    public static void initTest() {
        sTestTokenResponse = new TokenResponse();
        sTestTokenResponse.data = new Data();
        sTestTokenResponse.data.access_token = "14ea441da7bfbb1487a7cefc0110abe5";
        sTestTokenResponse.data.refresh_token = "28ea441da7bfbb1487a7cefc0110abe5";
        sTestTokenResponse.data.expires_in = 7200;
    }
}
