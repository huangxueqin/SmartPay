package com.android.smartpay;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.smartpay.fragments.BaseFragment;
import com.android.smartpay.fragments.InputFragment;
import com.android.smartpay.fragments.RecordFragment;
import com.android.smartpay.fragments.SettingFragment;
import com.android.smartpay.http.HttpService;
import com.android.smartpay.http.OnRequest;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.OrderInfo;
import com.android.smartpay.jsonbeans.OrderPayParam;
import com.android.smartpay.jsonbeans.OrderPayResponse;
import com.android.smartpay.jsonbeans.OrderSpecResponse;
import com.android.smartpay.jsonbeans.OrderStatusResponse;
import com.android.smartpay.jsonbeans.OrderSubmitParam;
import com.android.smartpay.jsonbeans.OrderSubmitResponse;
import com.android.smartpay.utilities.Cons;
import com.android.smartpay.utilities.HttpUtils;
import com.android.smartpay.utilities.Permission;
import com.google.gson.Gson;

import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements FragmentListener {
    private static final String TAG = "TAG-------->";
    private static final boolean TEST_DEBUG = Application.TEST_DEBUG;

    private Toolbar toolbar;
    private final BaseFragment mInputFragment = new InputFragment();
    private final BaseFragment mRecordFragment = new RecordFragment();
    private final BaseFragment mSettingFragment = new SettingFragment();
    private TabIndicator mTabInput, mTabRecord, mTabSetting;
    private BaseFragment mFrontFragment;
    private BaseFragment mTargetFragmentOnActivityResult;

    private Handler mMainHandler;
    private Handler mWorkHandler;
    private boolean mLoginSuccess = false;
    private LoginResponse.ShopUser mUser;
    private int mPermission;
    private HttpService mHttpService;
    private Preferences mPreferences;
    private DataLoader mLoader;
    private ProgressDialog mProgressDialog;

    // booleans for pay
    private boolean mCancelPay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHttpService = HttpService.get();
        mLoader = DataLoader.get();
        mWorkHandler = HttpService.getWorkHandler();
        mMainHandler = mHttpService.getMainHandler();
        mPreferences = new Preferences(this);

        setupToolbar();
        initTabsOnce();
        setupFragmentsOnce();
        initProgressDialog();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void T(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    private static void L(String msg) {
        Log.d(TAG, msg);
    }

    private void initTabsOnce() {
        mTabInput = (TabIndicator) findViewById(R.id.tab_input);
        mTabInput.setOnClickListener(mTabClickListener);
        mTabInput.setFragment(mInputFragment);
        mTabRecord = (TabIndicator) findViewById(R.id.tab_record);
        mTabRecord.setOnClickListener(mTabClickListener);
        mTabRecord.setFragment(mRecordFragment);
        mTabSetting = (TabIndicator) findViewById(R.id.tab_setting);
        mTabSetting.setOnClickListener(mTabClickListener);
        mTabSetting.setFragment(mSettingFragment);
    }

    private void setupFragmentsOnce() {
        mInputFragment.setTab(mTabInput);
        mRecordFragment.setTab(mTabRecord);
        mSettingFragment.setTab(mTabSetting);
        mInputFragment.setFragmentListener(this);
        mRecordFragment.setFragmentListener(this);
        mSettingFragment.setFragmentListener(this);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.container, mInputFragment);
        transaction.add(R.id.container, mRecordFragment);
        transaction.add(R.id.container, mSettingFragment);
//        mFrontFragment = mInputFragment;
//        mFrontFragment.getAttachedTab().setSelect(true);
        transaction.hide(mInputFragment);
        transaction.hide(mRecordFragment);
        transaction.hide(mSettingFragment);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mFrontFragment == null && mTargetFragmentOnActivityResult == null) {
            mFrontFragment = mInputFragment;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.hide(mInputFragment).hide(mRecordFragment).hide(mSettingFragment)
                    .show(mFrontFragment);
            transaction.commit();
            mFrontFragment.getAttachedTab().setSelect(true);
            getSupportActionBar().hide();
        } else if(mTargetFragmentOnActivityResult != null) {
            if(mFrontFragment == null) {
                mFrontFragment = mInputFragment;
            }
            if(mFrontFragment != mTargetFragmentOnActivityResult) {
                switchToFragment(mTargetFragmentOnActivityResult);
            }
            mTargetFragmentOnActivityResult = null;
        }

        if(!mLoginSuccess) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), Cons.REQUEST_LOGIN);
                }
            });
        }
    }

    private void initProgressDialog() {
        if(mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setCancelable(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mNetworkReceiver);
        super.onDestroy();
    }

    private void onFragmentSwitched() {
        if(mFrontFragment == mInputFragment) {
            getSupportActionBar().hide();
        }
        else if(mFrontFragment == mRecordFragment) {
            getSupportActionBar().show();
            TextView title = (TextView) toolbar.findViewById(R.id.appbar_title);
            title.setText(R.string.record_title);
        }
        else if(mFrontFragment == mSettingFragment) {
            getSupportActionBar().show();
            TextView title = (TextView) toolbar.findViewById(R.id.appbar_title);
            title.setText(R.string.setting_title);
        }
    }

    private View.OnClickListener mTabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(!(v instanceof TabIndicator)) {
                throw new RuntimeException("not a tab");
            }
            TabIndicator tab = (TabIndicator) v;
            BaseFragment attachedFragment = tab.getAttachedFragment();
            if(mFrontFragment != attachedFragment) {
                switchToFragment(attachedFragment);
            }
        }
    };

    private void switchToFragment(BaseFragment target) {
        mFrontFragment.getAttachedTab().setSelect(false);
        target.getAttachedTab().setSelect(true);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(mFrontFragment).show(target).commit();
        mFrontFragment = target;
        onFragmentSwitched();
    }

    private void afterUserLogin() {
        mRecordFragment.updateUserInfo(mUser);
        mInputFragment.updateUserInfo(mUser);
        mSettingFragment.updateUserInfo(mUser);
        mLoader.setUser(mUser);
    }

    @Override
    public void onEvent(FragmentCallback callback, int action, Intent data) {
        switch(action) {
            case Cons.ACTION_QQ_PAY:
            case Cons.ACTION_WECHAT_PAY:
                if((action == Cons.ACTION_QQ_PAY && !Permission.hasPermQQPay(mPermission)) ||
                        (action == Cons.ACTION_WECHAT_PAY && !Permission.hasPermWechatPay(mPermission))) {
                    T("没有权限");
                    return;
                }
                float money = data.getFloatExtra(Cons.ARG_MONEY, -1);
                int requestCode = Cons.REQUEST_SCAN;
                L("money is: " + money);
                Intent intent = new Intent(this, CaptureActivity.class);
                intent.putExtra(Cons.ARG_ACTION, action);
                intent.putExtra(Cons.ARG_MONEY, money);
                intent.putExtra(Cons.ARG_PERM, mPermission);
                intent.putExtra(Cons.ARG_USER, new Gson().toJson(mUser));
                startActivityForResult(intent, requestCode);
                break;
            case Cons.ACTION_LOGOUT:
                mHttpService.cancelAllRunningTasks();
                mPreferences.clearLoginInfo();
                mUser = null;
                mPermission = 0;
                mLoginSuccess = false;
                startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), Cons.REQUEST_LOGIN);
                break;
            case Cons.ACTION_ORDER_LIST:
                Intent i = new Intent(this, OrderListActivity.class);
                i.putExtra(Cons.ARG_LIST_TYPE, data.getIntExtra(Cons.ARG_LIST_TYPE, Cons.TYPE_DAY));
                startActivityForResult(i, Cons.REQUEST_ORDER_LIST);
                break;
            case Cons.ACTION_STATISTICS:
                i = new Intent(this, StatisticActivity.class);
                startActivityForResult(i, Cons.REQUEST_STATISTICS);
                break;
            case Cons.ACTION_MOST_RECENT:
                if(mLoader.getMostRecentOrder() == null) {
                    T("最近没有订单");
                } else {
                    i = new Intent(this, OrderSpecificActivity.class);
                    i.putExtra(Cons.ARG_ORDER, new Gson().toJson(mLoader.getMostRecentOrder()));
                    startActivityForResult(i, Cons.REQUEST_ORDER_SPEC);
                }
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Cons.REQUEST_LOGIN:
                if(resultCode == RESULT_CANCELED) {
                    finish();
                } else {
                    mTargetFragmentOnActivityResult = mInputFragment;
                    Preferences preferences = new Preferences(this);
                    L(preferences.getUserAbilities());
                    L(preferences.getUserBasic());
                    mUser = new Gson().fromJson(preferences.getUserBasic(), LoginResponse.ShopUser.class);
                    mPermission = Permission.buildPermission(new Gson().fromJson(preferences.getUserAbilities(), LoginResponse.Abilities.class));
                    afterUserLogin();
                    mLoginSuccess = true;
                }
                break;
            case Cons.REQUEST_SCAN:
                if(resultCode == RESULT_OK) {
                    int payType = 0; //data.getIntExtra(ARG_PAY_TYPE, 2);
                    String authCode = null; //data.getStringExtra(ARG_SCAN_RESULT);
                    float money = 1000; //data.getFloatExtra(ARG_MONEY, 0);
                    if(money > 0 && authCode != null) {
                        startPay(money, payType, authCode);
                    }
                }
                break;
            case Cons.REQUEST_ORDER_RESULT:
                // switch to mRecord fragment
                if(resultCode == RESULT_OK) {
                    int action = data.getIntExtra(Cons.ARG_ACTION, Cons.ACTION_CASHIER);
                    mTargetFragmentOnActivityResult = action == Cons.ACTION_CASHIER ? mInputFragment : mRecordFragment;
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void startPay(final float money, final int payType, final String authCode) {
        if (!mHttpService.isConnected()) {
            T("没有数据连接");
            return;
        }
        mCancelPay = false;

        // show dialog
        final ProgressDialog cancelDialog = new ProgressDialog(this);
        cancelDialog.setMessage("正在支付");
        cancelDialog.setCancelable(false);
        cancelDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCancelPay = true;
            }
        });
        cancelDialog.show();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                // submit order interface
                OrderSubmitParam submitParam = new OrderSubmitParam();
                submitParam.totalprice = money;
                submitParam.pos_pin_code = HttpService.PIN_CODE;
                submitParam.shop_user_id = mUser.id;
                OrderSubmitResponse submitResponse = mHttpService.executeJsonPostSync(HttpUtils.ORDER_SUBMIT_URL,
                        new Gson().toJson(submitParam), OrderSubmitResponse.class);
                if (submitResponse == null || submitResponse.errcode == null || !submitResponse.errcode.equals("0")) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            cancelDialog.dismiss();
                            if (mCancelPay) {
                                T("支付取消");
                            } else {
                                T("支付失败");
                            }
                        }
                    });
                    return;
                }
                if(mCancelPay) {
                    T("支付取消");
                    return;
                }
                // pay order interface
                OrderPayParam payParam = new OrderPayParam();
                OrderPayParam.PayInfoItem item = new OrderPayParam.PayInfoItem();
                item.auth_code = authCode;
                item.type = String.valueOf(payType);
                item.total = submitResponse.data.order.should_pay;
                payParam.payinfo.add(item);
                payParam.order_id = submitResponse.data.order.id;
                payParam.platform = HttpService.PLATFORM;
                payParam.seller_id = mUser.seller_id;
                payParam.shop_user_id = mUser.id;
                OrderPayResponse payResponse = mHttpService.executeJsonPostSync(HttpUtils.ORDER_PAY_URL,
                        new Gson().toJson(payParam), OrderPayResponse.class);
                if (payResponse == null || payResponse.errcode == null || !payResponse.errcode.equals("0")) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            cancelDialog.dismiss();
                            if(mCancelPay) {
                                T("支付取消");
                            } else {
                                T("支付失败");
                            }
                        }
                    });
                    return;
                }

                // query order status interface
                String timeStamp = HttpUtils.getTimeStamp();
                String signMethod = HttpService.SIGN_METHOD;
                final String orderId = submitResponse.data.order.id;
                String shopUserId = mUser.id;
                String signString = HttpService.SKEY + "order_id" + orderId +
                        "shop_user_id" + shopUserId +
                        "sign_method" + signMethod +
                        "timeStamp" + timeStamp + HttpService.SKEY;
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
                        if (statusResponse != null && statusResponse.errcode != null && !statusResponse.errcode.equals("0")) {
                            if (statusResponse.data.order.status == 1) {
                                mMainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        cancelDialog.dismiss();
                                        onPaySuccess(statusResponse.data.order.id);
                                    }
                                });
                                return;
                            }
                        }
                    }
                }
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        T("支付已取消");
                    }
                });
            }
        };
        mWorkHandler.post(r);
    }

    private void onPaySuccess(final String orderId) {
        OnRequest<OrderSpecResponse> callback = new OnRequest<OrderSpecResponse>() {
            @Override
            public void onNoConnection() {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                        T("没有连接");
                    }
                });
            }
            @Override
            public void onComplete(OrderSpecResponse orderSpecResponse) {
                // add order info to data loader
                final OrderInfo orderInfo = orderSpecResponse.data.order;
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        DataLoader.get().addNewOrder(orderInfo);
                        // dismiss dialog and start order specific activity
                        mProgressDialog.dismiss();
                        Intent i = new Intent(MainActivity.this, TransactionResultActivity.class);
                        i.putExtra(Cons.ARG_TOTAL_MONEY, orderInfo.should_pay);
                        i.putExtra(Cons.ARG_CREATE_TIME, orderInfo.createtime);
                        startActivityForResult(i, Cons.REQUEST_ORDER_RESULT);
                    }
                });
            }

            @Override
            public void onFail(String code, String msg) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.dismiss();
                        T("获取订单信息失败，请检查网络连接");
                    }
                });
            }
        };


        // show dialog
        mProgressDialog.setMessage("支付完成,正在获取订单信息");
        mProgressDialog.show();

        // query order info
        String timestamp = HttpUtils.getTimeStamp();
        String sign_method = HttpService.SIGN_METHOD;
        String shop_user_id = mUser.id;
        String order_id = orderId;
        String sign = HttpUtils.MD5Hash(HttpService.SKEY + "order_id" + order_id
                +"shop_user_id" + shop_user_id
                + "sign_method" + sign_method
                + "timestamp" + timestamp + HttpService.SKEY);
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("timestamp", timestamp));
        params.add(new BasicNameValuePair("sign_method", sign_method));
        params.add(new BasicNameValuePair("shop_user_id", shop_user_id));
        params.add(new BasicNameValuePair("order_id", order_id));
        params.add(new BasicNameValuePair("sign", sign));
        mHttpService.executeJsonGetAsync(HttpUtils.buildUrlWithParams(HttpUtils.ORDER_QUERY_SPEC_URL, params), callback, OrderSpecResponse.class);
    }

    private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(HttpService.isConnected() && mLoader.isLoadFail() && mLoginSuccess) {
                mLoader.setUser(mUser);
            } else if(!HttpService.isConnected() && mLoginSuccess){
                T("没有网络连接");
            }
        }
    };
}
