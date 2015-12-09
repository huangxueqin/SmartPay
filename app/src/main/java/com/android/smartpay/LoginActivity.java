package com.android.smartpay;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.android.smartpay.http.HttpService;
import com.android.smartpay.http.OnRequest;

/**
 * Created by xueqin on 2015/12/1 0001.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "TAG--------->";
    private static final boolean NO_NETWORK_DEBUG = Application.NO_NETWORK_DEBUG;

    private EditText mEtUsername;
    private EditText mEtPassword;
    private CheckBox mCbAccountRemember;
    private Button mLoginButton;

    private HttpService mHttpService;
    private Preferences mPreferences;
    private boolean mIsLogin = false;
    private ProgressDialog mLoginDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mEtUsername = (EditText) findViewById(R.id.username);
        mEtPassword = (EditText) findViewById(R.id.password);
        mCbAccountRemember = (CheckBox) findViewById(R.id.cb_account_remember);
        mLoginButton = (Button) findViewById(R.id.bt_login);
        mLoginButton.setOnClickListener(this);
        initProgressDialog();

        mHttpService = HttpService.get();
        mPreferences = new Preferences(this);
        if (mPreferences.accountRemembered()) {
            mCbAccountRemember.setChecked(true);
            mEtUsername.setText(mPreferences.getAccountName());
            mEtPassword.setText(mPreferences.getAccountPassword());
        }
        if (!HttpService.isConnected()) {
            T("没有网络连接");
            mLoginButton.setEnabled(false);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
    }

    private void T(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private BroadcastReceiver mNetworkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(HttpService.isConnected()) {
                mLoginButton.setEnabled(true);
            } else {
                T("没有网络连接");
                mLoginButton.setEnabled(false);
            }
        }
    };

    OnRequest<Void> mLoginCallback = new OnRequest<Void>() {
        @Override
        public void onNoConnection() {
            T("没有网络连接");
            mLoginButton.setEnabled(false);
            mLoginDialog.dismiss();
            mIsLogin = false;
        }

        @Override
        public void onComplete(Void aVoid) {
            mLoginDialog.dismiss();
            if(mCbAccountRemember.isChecked()) {
                String username = mEtUsername.getText().toString();
                String password = mEtPassword.getText().toString();
                mPreferences.saveAccountInfo(username, password);
            }
            setResult(RESULT_OK);
            finish();
        }

        @Override
        public void onFail(String code, String msg) {
            T("登陆失败，检查用户名或密码是否正确");
            mLoginDialog.dismiss();
            mIsLogin = false;
        }
    };

    private void startLogin(final String username, final String password) {
        mIsLogin = true;
        mLoginDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                mHttpService.userLogin(username, password, mLoginCallback);
            }
        }).start();
    }

    private void initProgressDialog() {
        if(mLoginDialog == null) {
            mLoginDialog = new ProgressDialog(this);
            mLoginDialog.setMessage("正在登陆");
            mLoginDialog.setIndeterminate(false);
            mLoginDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mLoginDialog.setCancelable(false);
        }
    }

    @Override
    protected void onPause() {
        if(!mCbAccountRemember.isChecked()) {
            mPreferences.clearAccountInfo();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mNetworkReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(!mIsLogin) {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_login:
                if(NO_NETWORK_DEBUG) {
                    startLogin("111", "111");
                    return;
                }
                String username = mEtUsername.getText().toString();
                String password = mEtPassword.getText().toString();
                if(TextUtils.isEmpty(username)) {
                    T("请输入用户名");
                } else if(TextUtils.isEmpty(password)) {
                    T("密码不能为空");
                } else {
                    startLogin(username, password);
                }
                break;
        }
    }

}
