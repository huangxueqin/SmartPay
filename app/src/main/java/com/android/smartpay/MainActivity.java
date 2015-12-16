package com.android.smartpay;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
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
import com.android.smartpay.http.BasicNameValuePair;
import com.android.smartpay.http.HttpService;
import com.android.smartpay.http.OnRequest;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.OrderInfo;
import com.android.smartpay.jsonbeans.OrderSpecResponse;
import com.android.smartpay.utilities.Cons;
import com.android.smartpay.utilities.HttpUtils;
import com.android.smartpay.utilities.Permission;
import com.google.gson.Gson;

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
    private Intent mPayServiceIntent;
    private boolean mLoginSuccess = false;
    private LoginResponse.ShopUser mUser;
    private int mPermission;
    private HttpService mHttpService;
    private Preferences mPreferences;
    private DataLoader mLoader;
    private ProgressDialog mProgressDialog;

    // booleans for pay
    private boolean mCancelPay = false;
    private ProgressDialog mStartPayDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHttpService = HttpService.get();
        mLoader = DataLoader.get();
        mMainHandler = new Handler(getMainLooper());
        mPreferences = new Preferences(this);

        setupToolbar();
        initTabsOnce();
        setupFragmentsOnce();
        initProgressDialog();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);

        mPayServiceIntent = new Intent(this, LocalPayService.class);
        startService(mPayServiceIntent);
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
        transaction.hide(mInputFragment);
        transaction.hide(mRecordFragment);
        transaction.hide(mSettingFragment);
        transaction.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!mLoginSuccess) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    startActivityForResult(new Intent(MainActivity.this, LoginActivity.class), Cons.REQUEST_LOGIN);
                }
            });
        }
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
        L("destory running");
        unregisterReceiver(mNetworkReceiver);
        stopService(mPayServiceIntent);
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
        mLoader.setUser(mUser, Permission.hasPermOrder(mPermission));
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
                if(money >= 0.01f) {
                    Intent intent = new Intent(this, CaptureActivity.class);
                    intent.putExtra(Cons.ARG_ACTION, action);
                    intent.putExtra(Cons.ARG_MONEY, money);
                    intent.putExtra(Cons.ARG_PERM, mPermission);
                    intent.putExtra(Cons.ARG_USER, new Gson().toJson(mUser));
                    startActivityForResult(intent, requestCode);
                }
                else {
                    T("金额至少是0.01元");
                }
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
                if(!Permission.hasPermOrder(mPermission)) {
                    T("没有权限查看订单历史");
                    return;
                }
                Intent i = new Intent(this, OrderListActivity.class);
                i.putExtra(Cons.ARG_LIST_TYPE, data.getIntExtra(Cons.ARG_LIST_TYPE, Cons.TYPE_DAY));
                startActivityForResult(i, Cons.REQUEST_ORDER_LIST);
                break;
            case Cons.ACTION_STATISTICS:
                if(!Permission.hasPermOrder(mPermission)) {
                    T("没有权限查看数据统计");
                    return;
                }
                i = new Intent(this, StatisticActivity.class);
                startActivityForResult(i, Cons.REQUEST_STATISTICS);
                break;
            case Cons.ACTION_MOST_RECENT:
                if(!Permission.hasPermOrder(mPermission)) {
                    T("没有权限查看最近订单");
                    return;
                }
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
                } else if(resultCode == RESULT_OK) {
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
                    int payType = data.getIntExtra(Cons.ARG_PAY_TYPE, 2);
                    String authCode = data.getStringExtra(Cons.ARG_SCAN_RESULT);
                    float money = data.getFloatExtra(Cons.ARG_MONEY, 0);
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
        if (!HttpUtils.isConnected(this)) {
            T("没有数据连接");
            return;
        }
        mCancelPay = false;
        // show dialog
        mStartPayDialog = new ProgressDialog(this);
        mStartPayDialog.setMessage("正在提交...");
        mStartPayDialog.setCancelable(false);
        mStartPayDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mPaySerConn != null) {
                    mPaySerConn.cancelPay();
                }
            }
        });
        mStartPayDialog.show();
        bindPayServiceForPay(money, payType, authCode);
    }

    private void bindPayServiceForPay(float money, int paytype, String authCode) {
        L("bind service");
        Intent ps = new Intent(this.getApplicationContext(), LocalPayService.class);
        mPaySerConn = new PayConnection();
        mPaySerConn.authCode = authCode;
        mPaySerConn.money = money;
        mPaySerConn.payType = paytype;
        bindService(ps, mPaySerConn, BIND_AUTO_CREATE);
    }

    private PayConnection mPaySerConn;
    private class PayConnection implements ServiceConnection {
        public float money;
        public int payType;
        public String authCode;
        public LocalPayService payService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalPayService.LocalBinder binder = (LocalPayService.LocalBinder) service;
            if(binder != null) {
                payService = binder.getService();
                payService.registerClient(mMessenger);
                payService.startPay(mUser, money, payType, authCode);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            L("unbind service");
            if(payService != null) {
                payService.unregisterClient();
            }
        }

        public void cancelPay() {
            if(payService != null) {
                cancelPay();
            }
        }
    };

    private Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            boolean payFailed = false;
            switch(msg.what) {
                case Cons.MSG_ORDER_SUBMIT_SUCCESS:
                    mStartPayDialog.setMessage("订单提交完成，正在支付...");
                    break;
                case Cons.MSG_ORDER_SUBMIT_FAILED:
                    payFailed = true;
                    mStartPayDialog.dismiss();
                    T("订单提交失败");
                    break;
                case Cons.MSG_ORDER_PAY_SUCCESS:
                    mStartPayDialog.dismiss();
                    String orderId = (String) msg.obj;
                    onPaySuccess(orderId);
                    break;
                case Cons.MSG_ORDER_PAY_FAILED:
                    payFailed = true;
                    mStartPayDialog.dismiss();
                    T("订单支付失败");
                    break;
                case Cons.MSG_CANCEL_ORDER_PAY:
                    payFailed = true;
                    mStartPayDialog.dismiss();
                    T("订单取消");
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
            if(payFailed) {
                unbindService(mPaySerConn);
            }
        }
    });

    private void onPaySuccess(final String orderId) {
        // show dialog
        mProgressDialog.setMessage("支付完成,正在获取订单信息...");
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
        mHttpService.executeJsonGetAsync(HttpUtils.buildUrlWithParams(HttpUtils.ORDER_QUERY_SPEC_URL, params), OrderSpecResponse.class, mOrderQueryCallback);
    }

    private OnRequest<OrderSpecResponse> mOrderQueryCallback = new OnRequest<OrderSpecResponse>() {
        @Override
        public void onComplete(OrderSpecResponse orderSpecResponse) {
            // add order info to data loader
            final OrderInfo orderInfo = orderSpecResponse.data.order;
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    unbindService(mPaySerConn);
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
                    unbindService(mPaySerConn);
                    mProgressDialog.dismiss();
                    T("获取订单信息失败，请检查网络连接");
                }
            });
        }
    };

    private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(HttpUtils.isConnected(MainActivity.this) && mLoader.isLoadFail() && mLoginSuccess) {
                mLoader.setUser(mUser, Permission.hasPermOrder(mPermission));
            } else if(!HttpUtils.isConnected(MainActivity.this) && mLoginSuccess){
                T("没有网络连接");
            }
        }
    };
}
