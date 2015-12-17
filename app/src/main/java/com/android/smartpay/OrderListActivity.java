package com.android.smartpay;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
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

import com.android.smartpay.http.BasicNameValuePair;
import com.android.smartpay.http.HttpService;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.OrderInfo;
import com.android.smartpay.jsonbeans.OrderListResponse;
import com.android.smartpay.utilities.Cons;
import com.android.smartpay.utilities.DateUtils;
import com.android.smartpay.utilities.HttpUtils;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xueqin on 2015/12/5 0005.
 */
public class OrderListActivity extends AppCompatActivity {
    public static final String TAG = "TAG--------->";
    public static final String[] sMonthTitles = { "一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月" };

    private LoginResponse.ShopUser mUser;
    private Spinner mSpinner;
    private TextView mTitle;
    private ListView mList;
    private OrderListAdapter mAdapter;
    private int mListType;
    private DataLoader mLoader;
    private String[] mMonthTitles;
    private int[] mMonthNumForIndex;
    private int mCurMonthIndex;
    private HashMap<Integer, OrderListAdapter> mMonthOrderCache = new HashMap<>();
    private ProgressDialog mLoadingDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        setupToolbar();
        // loading user
        mUser = new Gson().fromJson(new Preferences(this).getUserBasic(), LoginResponse.ShopUser.class);

        mLoader = DataLoader.get();
        mListType = getIntent().getIntExtra(Cons.ARG_LIST_TYPE, Cons.TYPE_TODAY);
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mTitle = (TextView) findViewById(R.id.list_title);

        if(mListType != Cons.TYPE_MONTH) {
            mTitle.setVisibility(View.VISIBLE);
            mSpinner.setVisibility(View.GONE);
            if(mListType == Cons.TYPE_DAY) {
                String dateStr = getIntent().getStringExtra(Cons.ARG_DATE);
                Date date = DateUtils.parseStandard(dateStr);
                mAdapter = new OrderListAdapter(this, mLoader.getOrderForDate(date));
                mTitle.setText(new SimpleDateFormat("yyyy年MM月dd日").format(date));
            } else if(mListType == Cons.TYPE_TODAY) {
                mTitle.setText("今日");
                mAdapter = new OrderListAdapter(this, mLoader.getTodayOrders());
            } else if(mListType == Cons.TYPE_WEEK) {
                mTitle.setText("本周");
                mAdapter = new OrderListAdapter(this, mLoader.getWeekOrders());
            }
        } else {
            mTitle.setVisibility(View.GONE);
            mSpinner.setVisibility(View.VISIBLE);
            int curMonth = DateUtils.getMonth();
            mMonthTitles = new String[curMonth];
            mMonthNumForIndex = new int[curMonth];
            for(int i = 1; i <= curMonth; i++) {
                mMonthTitles[curMonth-i] = sMonthTitles[i-1];
                mMonthNumForIndex[i-1] = curMonth - i;
            }
            mCurMonthIndex = 0;
            mAdapter = new OrderListAdapter(this, mLoader.getMonthOrders());
            mMonthOrderCache.put(mCurMonthIndex, mAdapter);

            ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<CharSequence>(this, R.layout.simple_spinner_title, mMonthTitles);
            spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_item);
            mSpinner.setAdapter(spinnerAdapter);
            mSpinner.setSelection(mCurMonthIndex);
            mSpinner.setOnItemSelectedListener(mSpinnerItemSelectedListener);
        }

        mList = (ListView) findViewById(R.id.order_list);
        if(mAdapter != null) {
            mList.setAdapter(mAdapter);
        }
    }

    private Spinner.OnItemSelectedListener mSpinnerItemSelectedListener = new Spinner.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mCurMonthIndex = position;
            OrderListAdapter adapter = mMonthOrderCache.get(mCurMonthIndex);
            mList.setAdapter(adapter);
            if(mMonthOrderCache.get(mCurMonthIndex) == null) {
                loadingOrdersForMonth();
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // do nothing
        }
    };


    private void loadingOrdersForMonth() {
        int month = mMonthNumForIndex[mCurMonthIndex];
        String url = buildLoadingUrl("0", month);
        new LoadingTask(new ArrayList<OrderInfo>()).execute(url);
    }

    private class LoadingTask extends AsyncTask<String, Void, Boolean> {
        int totalPage = -1;
        int currentPage = -1;
        List<OrderInfo> orders;

        public LoadingTask(List<OrderInfo> orders) {
            this.orders = orders;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(mLoadingDialog == null) {
                mLoadingDialog = new ProgressDialog(OrderListActivity.this);
                mLoadingDialog.setMessage("加载数据...");
                mLoadingDialog.setIndeterminate(false);
                mLoadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                mLoadingDialog.setCancelable(false);
            }
            mLoadingDialog.show();
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if(b) {
                if(currentPage >= totalPage - 1) {
                    mAdapter = new OrderListAdapter(OrderListActivity.this, orders);
                    mMonthOrderCache.put(mCurMonthIndex, mAdapter);
                    mList.setAdapter(mAdapter);
                    mLoadingDialog.dismiss();
                } else {
                    new LoadingTask(orders).execute(buildLoadingUrl(String.valueOf(currentPage+1), mMonthNumForIndex[mCurMonthIndex]));
                }
            } else {
                if(orders.isEmpty()) {
                    mLoadingDialog.dismiss();
                    T("没有数据");
                } else {
                    mAdapter = new OrderListAdapter(OrderListActivity.this, orders);
                    mList.setAdapter(mAdapter);
                    mLoadingDialog.dismiss();
                }
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String url = params[0];
            OrderListResponse list = HttpService.get().executeJsonGetSync(url, OrderListResponse.class);
            if(list == null) {
                return false;
            }
            else {
                totalPage = list.total_page;
                currentPage = list.current_page;
                orders.addAll(list.data.order);
                return true;
            }
        }
    }

    private String buildLoadingUrl(String page, int month) {
        Date firstDay = DateUtils.getFirstDayOfMonth(month);
        Date lastDay = DateUtils.getLastDayOfMonth(month);
        SimpleDateFormat startFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
        String starttime = startFormat.format(firstDay);
        SimpleDateFormat endFormat = new SimpleDateFormat("yyyy-MM-dd 23:59:59");
        String endtime = endFormat.format(lastDay);
        String timestamp = HttpUtils.getTimeStamp();
        String sign_method = HttpService.SIGN_METHOD;
        String shop_user_id = mUser.id;
        String signStr = HttpService.SKEY
                + "endtime" + endtime
                + "page" + page
                + "shop_user_id" + shop_user_id
                + "sign_method" + sign_method
                + "starttime" + starttime
                + "timestamp" + timestamp
                + HttpService.SKEY;
        String sign = HttpUtils.MD5Hash(signStr);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("endtime", endtime));
        params.add(new BasicNameValuePair("page", page));
        params.add(new BasicNameValuePair("shop_user_id", shop_user_id));
        params.add(new BasicNameValuePair("sign_method", sign_method));
        params.add(new BasicNameValuePair("starttime", starttime));
        params.add(new BasicNameValuePair("timestamp", timestamp));
        params.add(new BasicNameValuePair("sign", sign));
        return HttpUtils.buildUrlWithParams(HttpUtils.ORDER_LIST_URL, params);
    }

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
