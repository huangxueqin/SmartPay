package com.android.smartpay.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.smartpay.DataLoader;
import com.android.smartpay.R;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.OrderInfo;
import com.android.smartpay.utilities.Cons;
import com.android.smartpay.utilities.DateUtils;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by xueqin on 2015/11/26 0026.
 */
public class RecordFragment extends BaseFragment {
    Button mBtnStatistic;

    TextView mPanelMonth;
    TextView mPanelYear;
    TextView mPanelMonthIncome;
    TextView mPanelMonthOrderCount;
    TextView mPanelShopName;

    View mItemItemMostRecent;
    View mItemItemToday;
    View mItemItemWeek;
    View mItemItemMonth;
    TextView mItemMostRecentDate;
    TextView mItemMostRecentPayType;
    TextView mItemMostRecentMoney;
    TextView mItemTodayDate;
    TextView mItemTodayMoney;
    TextView mItemTodayCount;
    TextView mItemWeekDate;
    TextView mItemWeekCount;
    TextView mItemWeekMoney;
    TextView mItemMonth;
    TextView mItemMonthDate;
    TextView mItemMonthCount;
    TextView mItemMonthMoney;

    private LoginResponse.ShopUser mUser;
    private int mMonthOrderNum;
    private float mMonthOrderMoney;
    private int mTodayOrderNum;
    private float mTodayOrderMoney;
    private int mWeekOrderNum;
    private float mWeekOrderMoney;

    private DataLoader mLoader;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mLoader = DataLoader.get();
        mLoader.registerCallback(mLoadCallback);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoader.unRegisterCallback(mLoadCallback);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_record, container, false);

        mPanelMonth = (TextView) rootView.findViewById(R.id.panel_month);
        mPanelYear = (TextView) rootView.findViewById(R.id.panel_year);
        mPanelMonthIncome = (TextView) rootView.findViewById(R.id.panel_month_income);
        mPanelMonthOrderCount = (TextView) rootView.findViewById(R.id.panel_month_orders);
        mPanelShopName = (TextView) rootView.findViewById(R.id.panel_shop_name);

        mBtnStatistic = (Button) rootView.findViewById(R.id.button_statistics);
        mBtnStatistic.setOnClickListener(mClickListener);
        mItemItemMostRecent = rootView.findViewById(R.id.item_item_most_recent);
        mItemItemMostRecent.setOnClickListener(mClickListener);
        mItemItemToday = rootView.findViewById(R.id.item_item_today);
        mItemItemToday.setOnClickListener(mClickListener);
        mItemItemWeek = rootView.findViewById(R.id.item_item_week);
        mItemItemWeek.setOnClickListener(mClickListener);
        mItemItemMonth = rootView.findViewById(R.id.item_item_month);
        mItemItemMonth.setOnClickListener(mClickListener);

        mItemMostRecentDate = (TextView) rootView.findViewById(R.id.most_recent_date);
        mItemMostRecentMoney = (TextView) rootView.findViewById(R.id.most_recent_money);
        mItemMostRecentPayType = (TextView) rootView.findViewById(R.id.most_recent_paytype);
        mItemTodayDate = (TextView) rootView.findViewById(R.id.today_date);
        mItemTodayCount = (TextView) rootView.findViewById(R.id.today_order_count);
        mItemTodayMoney = (TextView) rootView.findViewById(R.id.today_money);
        mItemWeekDate = (TextView) rootView.findViewById(R.id.week_date);
        mItemWeekCount = (TextView) rootView.findViewById(R.id.week_order_count);
        mItemWeekMoney = (TextView) rootView.findViewById(R.id.week_money);
        mItemMonth = (TextView) rootView.findViewById(R.id.item_month);
        mItemMonthDate = (TextView) rootView.findViewById(R.id.month_date);
        mItemMonthCount = (TextView) rootView.findViewById(R.id.month_order_count);
        mItemMonthMoney = (TextView) rootView.findViewById(R.id.month_money);
        setupViewsByDate();
        return rootView;
    }

    private void setupViewsByDate() {
        int month = DateUtils.getMonth();
        int year = DateUtils.getYear();
        mPanelMonth.setText(String.valueOf(month));
        mPanelYear.setText(String.valueOf(year));
        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy年MM月dd日");
        mItemTodayDate.setText(formatter1.format(new Date()));

        SimpleDateFormat formatter2 = new SimpleDateFormat("MM月dd日");
        Date monday = DateUtils.getFirstDayOfWeek();
        Date sunday = DateUtils.getLastDayOfWeek();
        mItemWeekDate.setText(formatter2.format(monday) + " - " + formatter2.format(sunday));

        Date firstDayOfMonth = DateUtils.getFirstDayOfMonth();
        Date lastDayOfMonth = DateUtils.getLastDayOfMonth();
        mItemMonthDate.setText(formatter2.format(firstDayOfMonth) + " - " + formatter2.format(lastDayOfMonth));
        mItemMonth.setText("" + month + "月");
    }

    private void T(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mLoader.isLoadFail()) {
                T("加载数据失败，请检查网络设置");
                return;
            } else if(!mLoader.isLoadComplete()) {
                T("正在加载数据，请稍等");
                return;
            }
            Intent data = new Intent();
            switch (v.getId()) {
                case R.id.button_statistics:
                    mListener.onEvent(null, Cons.ACTION_STATISTICS, null);
                    break;
                case R.id.item_item_most_recent:
                    mListener.onEvent(null, Cons.ACTION_MOST_RECENT, null);
                    break;
                case R.id.item_item_today:
                    data.putExtra(Cons.ARG_LIST_TYPE, Cons.TYPE_DAY);
                    mListener.onEvent(null, Cons.ACTION_ORDER_LIST, data);
                    break;
                case R.id.item_item_week:
                    data.putExtra(Cons.ARG_LIST_TYPE, Cons.TYPE_WEEK);
                    mListener.onEvent(null, Cons.ACTION_ORDER_LIST, data);
                    break;
                case R.id.item_item_month:
                    data.putExtra(Cons.ARG_LIST_TYPE, Cons.TYPE_MONTH);
                    mListener.onEvent(null, Cons.ACTION_ORDER_LIST, data);
                    break;
            }
        }
    };

    private void setViewByMostRecentOrder() {
        OrderInfo order = mLoader.getMostRecentOrder();
        if(order == null) {
            T("没有历史订单");
            return;
        }
        mItemMostRecentMoney.setText(order.should_pay);
        mItemMostRecentPayType.setText(order.paytype);
        mItemMostRecentDate.setText(DateUtils.changeDateFormat(order.createtime, "yyyy-MM-dd hh:mm:ss", "yyyy年MM月dd日"));
    }

    @Override
    public void updateUserInfo(LoginResponse.ShopUser user) {
        mUser = user;
        mPanelShopName.setText(user.shop_name);
    }

    DataLoader.Callback mLoadCallback = new DataLoader.Callback() {
        @Override
        public void onLoadStart() {
        }

        @Override
        public void onLoadComplete() {
            setViewByMostRecentOrder();
            mMonthOrderNum = mLoader.getMonthOrderNum();
            mMonthOrderMoney = mLoader.getMonthOrderMoney();
            mPanelMonthIncome.setText("￥ " + String.format("%.2f", mMonthOrderMoney));
            mPanelMonthOrderCount.setText(String.valueOf(mMonthOrderNum) + " 笔");
            mItemMonthMoney.setText(String.format("%.2f", mMonthOrderMoney));
            mItemMonthCount.setText(String.valueOf(mMonthOrderNum) + " 笔");
            mTodayOrderNum = mLoader.getTodayOrderNum();
            mTodayOrderMoney = mLoader.getTodayOrderMoney();
            mItemTodayMoney.setText(String.format("%.2f", mTodayOrderMoney));
            mItemTodayCount.setText(String.valueOf(mTodayOrderNum) + " 笔");
            mWeekOrderMoney = mLoader.getWeekOrderMoney();
            mWeekOrderNum = mLoader.getWeekOrderNum();
            mItemWeekMoney.setText(String.format("%.2f", mWeekOrderMoney));
            mItemWeekCount.setText(String.valueOf(mWeekOrderNum) + " 笔");
        }

        @Override
        public void onNewOrderAdded(OrderInfo order) {
            setViewByMostRecentOrder();
            float money = Float.valueOf(order.should_pay);
            mTodayOrderMoney += money;
            mWeekOrderMoney += money;
            mMonthOrderMoney += money;
            mPanelMonthIncome.setText("￥ " + String.format("%.2f", mMonthOrderMoney));
            mItemMonthMoney.setText(String.format("%.2f", mMonthOrderMoney));
            mItemTodayMoney.setText(String.format("%.2f", mTodayOrderMoney));
            mItemWeekMoney.setText(String.format("%.2f", mWeekOrderMoney));
            mTodayOrderNum += 1;
            mWeekOrderNum += 1;
            mMonthOrderNum += 1;
            mPanelMonthOrderCount.setText(String.valueOf(mMonthOrderNum) + " 笔");
            mItemMonthCount.setText(String.valueOf(mMonthOrderNum) + " 笔");
            mItemTodayCount.setText(String.valueOf(mTodayOrderNum) + " 笔");
            mItemWeekCount.setText(String.valueOf(mWeekOrderNum) + " 笔");
        }
    };
}
