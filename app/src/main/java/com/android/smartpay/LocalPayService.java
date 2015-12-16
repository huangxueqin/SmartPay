package com.android.smartpay;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.smartpay.http.BasicNameValuePair;
import com.android.smartpay.http.HttpService;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.OrderPayParam;
import com.android.smartpay.jsonbeans.OrderPayResponse;
import com.android.smartpay.jsonbeans.OrderStatusResponse;
import com.android.smartpay.jsonbeans.OrderSubmitParam;
import com.android.smartpay.jsonbeans.OrderSubmitResponse;
import com.android.smartpay.utilities.Cons;
import com.android.smartpay.utilities.HttpUtils;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xueqin on 2015/12/2 0002.
 * Used as a local service for handling payment process
 */
public class LocalPayService extends Service {
    private HttpService mHttpService = HttpService.get();
    private Messenger mClient;
    private final IBinder mBinder = new LocalBinder();
    private boolean mCancelPay = false;
    private boolean mIsPaying = false;

    static HandlerThread sWorkThread;
    static Handler sWorkHandler;
    static {
        sWorkThread = new HandlerThread("service thread");
        sWorkThread.start();
        sWorkHandler = new Handler(sWorkThread.getLooper());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public class LocalBinder extends Binder {
        public LocalPayService getService() {
            return LocalPayService.this;
        }
    }

    public void registerClient(Messenger client) {
        mClient = client;
    }

    public void unregisterClient() {
        mClient = null;
    }

    public void cancelPay() {
        mCancelPay = true;
    }

    public void startPay(final LoginResponse.ShopUser user, final float money, final int payType, final String authCode) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                mCancelPay = false;
                mIsPaying = true;
                // submit order interface
                OrderSubmitParam submitParam = new OrderSubmitParam();
                submitParam.totalprice = money;
                submitParam.pos_pin_code = HttpService.PIN_CODE;
                submitParam.shop_user_id = user.id;
                OrderSubmitResponse submitResponse = mHttpService.executeJsonPostSync(HttpUtils.ORDER_SUBMIT_URL,
                        new Gson().toJson(submitParam), OrderSubmitResponse.class);
                if(mCancelPay) {
                    sendMessage(Message.obtain(null, Cons.MSG_CANCEL_ORDER_PAY));
                    return;
                }
                if(submitResponse == null) {
                    sendMessage(Message.obtain(null, Cons.MSG_ORDER_SUBMIT_FAILED));
                    return;
                }
                sendMessage(Message.obtain(null, Cons.MSG_ORDER_SUBMIT_SUCCESS));

                // order submit success, now start pay the order
                OrderPayParam payParam = new OrderPayParam();
                OrderPayParam.PayInfoItem item = new OrderPayParam.PayInfoItem();
                item.auth_code = authCode;
                item.type = String.valueOf(payType);
                item.total = submitResponse.data.order.should_pay;
                payParam.payinfo.add(item);
                payParam.order_id = submitResponse.data.order.id;
                payParam.platform = HttpService.PLATFORM;
                payParam.seller_id = user.seller_id;
                payParam.shop_user_id = user.id;
                OrderPayResponse payResponse = mHttpService.executeJsonPostSync(HttpUtils.ORDER_PAY_URL,
                        new Gson().toJson(payParam), OrderPayResponse.class);
                if(mCancelPay) {
                    sendMessage(Message.obtain(null, Cons.MSG_CANCEL_ORDER_PAY));
                    return;
                }
                if(payResponse == null) {
                    sendMessage(Message.obtain(null, Cons.MSG_ORDER_PAY_FAILED));
                    return;
                }

                // order pay return success, now query order state
                String timeStamp = HttpUtils.getTimeStamp();
                String signMethod = HttpService.SIGN_METHOD;
                final String orderId = submitResponse.data.order.id;
                String shopUserId = user.id;
                String signString = HttpService.SKEY + "order_id" + orderId +
                        "shop_user_id" + shopUserId +
                        "sign_method" + signMethod +
                        "timestamp" + timeStamp + HttpService.SKEY;
                String sign = HttpUtils.MD5Hash(signString);
                List<BasicNameValuePair> queryParam = new ArrayList<>();
                queryParam.add(new BasicNameValuePair("sign", sign));
                queryParam.add(new BasicNameValuePair("timestamp", timeStamp));
                queryParam.add(new BasicNameValuePair("sign_method", signMethod));
                queryParam.add(new BasicNameValuePair("order_id", orderId));
                queryParam.add(new BasicNameValuePair("shop_user_id", shopUserId));
                String queryUrl = HttpUtils.buildUrlWithParams(HttpUtils.ORDER_QUERY_STATUS_URL, queryParam);
                while (!mCancelPay) {
                    final OrderStatusResponse statusResponse = mHttpService.executeJsonGetSync(queryUrl, OrderStatusResponse.class);
                    if (!mCancelPay) {
                        if (statusResponse != null) {
                            if (statusResponse.data.order.status != 0) {
                                Message msg = Message.obtain();
                                msg.what = Cons.MSG_ORDER_PAY_SUCCESS;
                                msg.obj = statusResponse.data.order.id;
                                sendMessage(msg);
                                return;
                            }
                        }
                    }
                }
                sendMessage(Message.obtain(null, Cons.MSG_CANCEL_ORDER_PAY));
                mIsPaying = false;
            }
        };
        sWorkHandler.post(r);
    }

    private void sendMessage(Message msg) {
        if(mClient != null) {
            try {
                mClient.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


}
