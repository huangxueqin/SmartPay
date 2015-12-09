package com.android.smartpay;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.smartpay.utilities.Cons;

/**
 * Created by xueqin on 2015/12/2 0002.
 */
public class TransactionResultActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView mTotalPrice;
    private TextView mCreateTime;
    private TextView mLookup;
    private Button mButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_result);
        setupToolbar();
        mTotalPrice = (TextView) findViewById(R.id.money);
        mCreateTime = (TextView) findViewById(R.id.date);
        mLookup = (TextView) findViewById(R.id.lookup);
        mButton = (Button) findViewById(R.id.btn_continue);
        mLookup.setOnClickListener(this);
        mButton.setOnClickListener(this);
        mTotalPrice.setText("￥"+ String.format("%.2f", getIntent().getFloatExtra(Cons.ARG_TOTAL_MONEY, 0)));
        mCreateTime.setText("收钱时间:" + getIntent().getStringExtra(Cons.ARG_CREATE_TIME));
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            TextView title = (TextView) findViewById(R.id.appbar_title);
            title.setText(R.string.ps_title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        Intent data = new Intent();
        switch (v.getId()) {
            case R.id.btn_continue:
                data.putExtra(Cons.ARG_ACTION, Cons.ACTION_CASHIER);
                setResult(RESULT_OK, data);
                finish();
                break;
            case R.id.lookup:
                data.putExtra(Cons.ARG_ACTION, Cons.ACTION_RECORD);
                setResult(RESULT_OK, data);
                finish();
                break;
        }
    }
}
