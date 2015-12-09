package com.android.smartpay.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.smartpay.BusinessRuleActivity;
import com.android.smartpay.ContactActivity;
import com.android.smartpay.R;
import com.android.smartpay.UsingHelpActivity;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.utilities.Cons;

/**
 * Created by xueqin on 2015/11/26 0026.
 */
public class SettingFragment extends BaseFragment implements View.OnClickListener {

    View mContact;
    View mUsingHelp;
    View mBusinessRule;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_setting, null, false);
        mContact = rootView.findViewById(R.id.item_contact);
        mContact.setOnClickListener(this);
        mUsingHelp = rootView.findViewById(R.id.item_using_help);
        mUsingHelp.setOnClickListener(this);
        mBusinessRule = rootView.findViewById(R.id.item_business_rule);
        mBusinessRule.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_setting, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_setting_logout) {
            mListener.onEvent(null, Cons.ACTION_LOGOUT, null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.item_business_rule:
                Intent i = new Intent(getContext(), BusinessRuleActivity.class);
                startActivity(i);
                break;
            case R.id.item_using_help:
                i = new Intent(getContext(), UsingHelpActivity.class);
                startActivity(i);
                break;
            case R.id.item_contact:
                i = new Intent(getContext(), ContactActivity.class);
                startActivity(i);
                break;
        }
    }

    @Override
    public void updateUserInfo(LoginResponse.ShopUser user) {

    }
}
