package com.android.smartpay.utilities;

import org.apache.http.message.BasicNameValuePair;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by xueqin on 2015/12/1 0001.
 */
public class HttpUtils {
    public static final String URL = "http://posapitest.vikduo.com";
    public static final String LOGIN_URL = URL + "/v3/api/login";
    public static final String REFRESH_URL = URL + "/v3/api/refresh";
    public static final String ORDER_SUBMIT_URL = URL + "/v3/cashier-order/add";
    public static final String ORDER_PAY_URL = URL + "/v3/order/pay";
    public static final String ORDER_QUERY_STATUS_URL = URL + "/v3/order/status";
    public static final String ORDER_QUERY_SPEC_URL = URL + "/v3/order/get";
    public static final String ORDER_LIST_URL = URL + "/v3/order/list";

    public static String buildUrlWithParams(String url, List<BasicNameValuePair> params) {
        StringBuffer sb = new StringBuffer(url);
        if(params.size() > 0) {
            sb.append("?");
            for (int i = 0; i < params.size(); i++) {
                BasicNameValuePair pair = params.get(i);
                sb.append(pair.getName() + "=" + pair.getValue());
                if(i < params.size()-1) {
                    sb.append("&");
                }
            }
        }
        return sb.toString();
    }

    public static String signGet(List<BasicNameValuePair> params) {
        Comparator<BasicNameValuePair> comp = new Comparator<BasicNameValuePair>() {
            @Override
            public int compare(BasicNameValuePair lhs, BasicNameValuePair rhs) {
                int t = lhs.getName().compareTo(rhs.getName());
                if(t != 0) {
                    return t;
                }
                else {
                    return lhs.getValue().compareTo(rhs.getValue());
                }
            }
        };

        Collections.sort(params, comp);
        StringBuffer sb = new StringBuffer();
        for(BasicNameValuePair pair : params) {
            sb.append(pair.getName() + pair.getValue());
        }
        return MD5Hash(sb.toString());
    }

    public static String getTimeStamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    public static String MD5Hash(String msg) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(msg.getBytes());
            byte[] digestBytes = md.digest();
            StringBuilder digest = new StringBuilder();
            for(int i = 0; i < digestBytes.length; i++) {
                String hexString = Integer.toHexString(digestBytes[i] & 0xFF);
                if(hexString.length() < 2) {
                    digest.append('0');
                }
                digest.append(hexString);
            }
            return digest.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

}
