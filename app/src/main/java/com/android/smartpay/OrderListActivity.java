package com.android.smartpay;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.smartpay.utilities.Cons;

/**
 * Created by xueqin on 2015/12/5 0005.
 */
public class OrderListActivity extends AppCompatActivity {
    public static final String TAG = "TAG--------->";
    private Spinner mSpinner;
    private ListView mList;
    private OrderListAdapter mAdapter;
    private int mCurType;
    DataLoader mLoader;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        setupToolbar();
        mLoader = DataLoader.get();
        mCurType = getIntent().getIntExtra(Cons.ARG_LIST_TYPE, Cons.TYPE_DAY);
        mAdapter = new OrderListAdapter(this, mCurType, mLoader.getTodayOrders(), mLoader.getWeekOrders(), mLoader.getMonthOrders());
        mList = (ListView) findViewById(R.id.order_list);
        mList.setAdapter(mAdapter);

        mSpinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.ola_spinner, R.layout.simple_spinner_title);
        adapter.setDropDownViewResource(R.layout.simple_spinner_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setSelection(mCurType == Cons.TYPE_DAY ? 0 : mCurType == Cons.TYPE_WEEK ? 1 : 2);
        mSpinner.setOnItemSelectedListener(mSpinnerItemSelectedListener);

    }

    private Spinner.OnItemSelectedListener mSpinnerItemSelectedListener = new Spinner.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if(position == 0) {
                L(parent.getItemAtPosition(0).toString());
                // today
                mCurType = Cons.TYPE_DAY;
            }
            else if(position == 1) {
                L(parent.getItemAtPosition(1).toString());
                // this week
                mCurType = Cons.TYPE_WEEK;
            }
            else if(position == 2) {
                L(parent.getItemAtPosition(2).toString());
                // this month
                mCurType = Cons.TYPE_MONTH;
            }
            mAdapter.setType(mCurType);
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // do nothing
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            TextView title = (TextView) findViewById(R.id.appbar_title);
            title.setText(R.string.record_title);
        }
    }

    public void T(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void L(String msg) {
        Log.d(TAG, msg);
    }
}
