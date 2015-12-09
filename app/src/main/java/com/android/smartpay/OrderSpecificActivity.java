package com.android.smartpay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.android.smartpay.jsonbeans.OrderInfo;
import com.android.smartpay.utilities.Cons;
import com.google.gson.Gson;

/**
 * Created by xueqin on 2015/12/2 0002.
 */
public class OrderSpecificActivity extends AppCompatActivity {
    OrderInfo mOrder;
    TextView mMoney;
    TextView mStatusTop;
    TextView mStatus2;
    TextView mItemInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orderspecific);
        setupToolbar();
        String orderStr = getIntent().getStringExtra(Cons.ARG_ORDER);
        mOrder = new Gson().fromJson(orderStr, OrderInfo.class);

    }

    private void setupViews() {

    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            TextView title = (TextView) findViewById(R.id.appbar_title);
            title.setText(R.string.os_title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
