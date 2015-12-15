package com.android.smartpay;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.smartpay.jsonbeans.LoginResponse;
import com.google.gson.Gson;

/**
 * Created by xueqin on 2015/12/1 0001.
 */
public class Preferences {
    private static final boolean TEST_DEBUG = Application.TEST_DEBUG;

    // preferences to store user info and user abilities
    private static final String KEY_PREFS = "com.android.smartpay.prefs";
    private static final String KEY_USER_BASIC = "sp_user_basic";
    private static final String KEY_USER_ABILITIES = "sp_user_abilities";

    // preferences to store login info
    private static final String KEY_ACCOUNT_REMEMBER = "sp_remember_account";
    private static final String KEY_ACCOUNT_NAME = "sp_key_account_name";
    private static final String KEY_ACCOUNT_PASSWORD = "sp_key_account_password";

    private SharedPreferences sp;

    public Preferences(Context context) {
        sp = context.getSharedPreferences(KEY_PREFS, Context.MODE_PRIVATE);
    }

    public void saveAccountInfo(String username, String password) {
        sp.edit()
                .putBoolean(KEY_ACCOUNT_REMEMBER, true)
                .putString(KEY_ACCOUNT_NAME, username)
                .putString(KEY_ACCOUNT_PASSWORD, password)
                .commit();
    }

    public void clearAccountInfo() {
        sp.edit()
                .remove(KEY_ACCOUNT_REMEMBER)
                .remove(KEY_ACCOUNT_NAME)
                .remove(KEY_ACCOUNT_PASSWORD)
                .commit();
    }

    public boolean accountRemembered() {
        return sp.getBoolean(KEY_ACCOUNT_REMEMBER, false);
    }

    public String getAccountName() {
        return sp.getString(KEY_ACCOUNT_NAME, "");
    }

    public String getAccountPassword() {
        return sp.getString(KEY_ACCOUNT_PASSWORD, "");
    }

    public void clearLoginInfo() {
        sp.edit()
                .remove(KEY_USER_BASIC)
                .remove(KEY_USER_ABILITIES)
                .commit();
    }

    public void setLoginInfo(LoginResponse loginResponse) {
        String userBasic = new Gson().toJson(loginResponse.data.shop_user);
        String userAbi = new Gson().toJson(loginResponse.data.abilities);
        sp.edit()
                .putString(KEY_USER_BASIC, userBasic)
                .putString(KEY_USER_ABILITIES, userAbi)
                .commit();
    }

    public String getUserBasic() {
        return sp.getString(KEY_USER_BASIC, null);
    }

    public String getUserAbilities() {
        return sp.getString(KEY_USER_ABILITIES, null);
    }
}
