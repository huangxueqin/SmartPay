package com.android.smartpay;

import android.os.AsyncTask;
import android.telecom.Call;
import android.util.Log;

import com.android.smartpay.http.BasicNameValuePair;
import com.android.smartpay.http.HttpService;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.OrderInfo;
import com.android.smartpay.jsonbeans.OrderListResponse;
import com.android.smartpay.utilities.DateUtils;
import com.android.smartpay.utilities.HttpUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by xueqin on 2015/12/8 0008.
 */
public class DataLoader {
    private static final String TAG = "TAG---------->";
    private static final int PAST_DAY_NUM = 31;

    private List<OrderInfo> mThirtyDayOrders;
    private List<List<OrderInfo>> mDayOrder;

    private List<OrderInfo> mSevenDayOrders = new ArrayList<>();
    private List<OrderInfo> mMonthOrders = new ArrayList<>();
    private List<OrderInfo> mWeekOrders = new ArrayList<>();
    private List<OrderInfo> mTodayOrders = new ArrayList<>();
    private OrderInfo mMostRecentOrder;

    private HashMap<String, Integer> mMap;
    private SimpleDateFormat mMapFormatter = new SimpleDateFormat("yyyy-MM-dd");

    private HttpService mHttpService;
    private LoginResponse.ShopUser mUser;
    private boolean mLoadComplete = true;
    private boolean mCancelLoad = false;
    private boolean mLoadFail = false;
    private List<Callback> mCallbacks;

    private String mThirtyDaysBeforeTime;
    private String mTodayTime;

    private static DataLoader sINSTANCE;
    private DataLoader() {
        mHttpService = HttpService.get();
        mCallbacks = new ArrayList<>();
        Date thirtyDaysBefore = DateUtils.getThirtyDaysBefore();
        mThirtyDaysBeforeTime = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(thirtyDaysBefore);
        mTodayTime = new SimpleDateFormat("yyyy-MM-dd 23:59:59").format(new Date());

        mThirtyDayOrders = new ArrayList<>();
        mDayOrder = new ArrayList<>(PAST_DAY_NUM);
        for(int i = 0; i < PAST_DAY_NUM; i++) {
            mDayOrder.add(new ArrayList<OrderInfo>());
        }
        initMap();
    }

    private void initMap() {
        mMap = new HashMap<>();
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.add(Calendar.DATE, -(PAST_DAY_NUM-1));
        // consider all possible date formats
        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat formatter2 = new SimpleDateFormat("yyyy-M-d");
        SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy-MM-d");
        SimpleDateFormat formatter4 = new SimpleDateFormat("yyyy-M-dd");
        for(int i = 0; i < PAST_DAY_NUM; i++) {
            Date time = ca.getTime();
            mMap.put(formatter1.format(time), i);
            int month = ca.get(Calendar.MONTH);
            int day = ca.get(Calendar.DAY_OF_MONTH);
            if(month < 10) {
                mMap.put(formatter2.format(time), i);
            }
            if(day < 10) {
                mMap.put(formatter3.format(time), i);
            }
            if(day < 10 && month < 10) {
                mMap.put(formatter4.format(time), i);
            }
            ca.add(Calendar.DATE, 1);
        }
    }

    public static DataLoader get() {
        if(sINSTANCE == null) {
            sINSTANCE = new DataLoader();
        }
        return sINSTANCE;
    }

    public interface Callback {
        void onLoadStart();
        void onLoadComplete();
        void onNewOrderAdded(OrderInfo order);
    }

    public void registerCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void unRegisterCallback(Callback callback) {
        mCallbacks.remove(callback);
    }

    public void addNewOrder(OrderInfo order) {
        mThirtyDayOrders.add(order);
        mSevenDayOrders.add(order);
        mMonthOrders.add(order);
        mWeekOrders.add(order);
        mTodayOrders.add(order);
        mMostRecentOrder = order;
        for(Callback callback : mCallbacks) {
            callback.onNewOrderAdded(order);
        }
    }

    public List<OrderInfo> getThirtyDayOrders() {
        return mThirtyDayOrders;
    }

    public List<OrderInfo> getSevenDayOrders() {
        return mSevenDayOrders;
    }

    public List<OrderInfo> getMonthOrders() {
        return mMonthOrders;
    }

    public int getMonthOrderNum() {
        return mMonthOrders.size();
    }

    public float getMonthOrderMoney() {
        float money = 0;
        for(OrderInfo order : mMonthOrders) {
            money += Float.valueOf(order.should_pay);
        }
        return money;
    }

    public List<OrderInfo> getWeekOrders() {
        return mWeekOrders;
    }

    public int getWeekOrderNum() {
        return mWeekOrders.size();
    }

    public float getWeekOrderMoney() {
        float money = 0;
        for(OrderInfo order : mWeekOrders) {
            money += Float.valueOf(order.should_pay);
        }
        return money;
    }

    public List<OrderInfo> getTodayOrders() {
        return mTodayOrders;
    }

    public int getTodayOrderNum() {
        return mTodayOrders.size();
    }

    public float getTodayOrderMoney() {
        float money = 0;
        for(OrderInfo order : mTodayOrders) {
            money += Float.valueOf(order.should_pay);
        }
        return money;
    }

    public int getOrderNumForDate(Date date) {
        String key = mMapFormatter.format(date);
        if(mMap.get(key) != null) {
            return mDayOrder.get(mMap.get(key)).size();
        }
        return 0;
    }

    public float getOrderMoneyForDate(Date date) {
        float money = 0;
        String key = mMapFormatter.format(date);
        if(mMap.get(key) != null) {
            for(OrderInfo order : mDayOrder.get(mMap.get(key))) {
                money += Float.valueOf(order.should_pay);
            }
        }
        return money;
    }

    public List<Integer> getOrderNumForThirtyDays() {
        ArrayList<Integer> result = new ArrayList<>();
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.add(Calendar.DATE, -29);
        for(int i = 0; i < 30; i++) {
            result.add(getOrderNumForDate(ca.getTime()));
            ca.add(Calendar.DATE, 1);
        }
        return result;
    }

    public List<Float> getOrderMoneyForThirtyDays() {
        ArrayList<Float> result = new ArrayList<>();
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.add(Calendar.DATE, -29);
        for(int i = 0; i < 30; i++) {
            result.add(getOrderMoneyForDate(ca.getTime()));
            ca.add(Calendar.DATE, 1);
        }
        return result;
    }

    public OrderInfo getMostRecentOrder() {
        return mMostRecentOrder;
    }

    public void cancelLoad() {
        mCancelLoad = true;
    }

    public boolean isLoadComplete() {
        return mLoadComplete;
    }

    public boolean isLoadFail() {
        return mLoadFail;
    }

    private void clearData() {
        mThirtyDayOrders.clear();
        mSevenDayOrders.clear();
        mMonthOrders.clear();
        mWeekOrders.clear();
        mTodayOrders.clear();
        for(int i = 0; i < PAST_DAY_NUM; i++) {
            mDayOrder.get(i).clear();
        }
        mMostRecentOrder = null;
    }

    public void setUser(LoginResponse.ShopUser user, boolean load) {
        mUser = user;
        clearData();
        if(load) {
            startLoad();
        }
    }

    private String buildUrl(String page) {
        String timestamp = HttpUtils.getTimeStamp();
        String sign_method = HttpService.SIGN_METHOD;
        String shop_user_id = mUser.id;
        String signStr = HttpService.SKEY
                + "endtime" + mTodayTime
                + "page" + page
                + "shop_user_id" + shop_user_id
                + "sign_method" + sign_method
                + "starttime" + mThirtyDaysBeforeTime
                + "timestamp" + timestamp
                + HttpService.SKEY;
        String sign = HttpUtils.MD5Hash(signStr);

        L("signStr = " + signStr);
        L("sign = " + sign);

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("endtime", mTodayTime));
        params.add(new BasicNameValuePair("page", page));
        params.add(new BasicNameValuePair("shop_user_id", shop_user_id));
        params.add(new BasicNameValuePair("sign_method", sign_method));
        params.add(new BasicNameValuePair("starttime", mThirtyDaysBeforeTime));
        params.add(new BasicNameValuePair("timestamp", timestamp));
        params.add(new BasicNameValuePair("sign", sign));
        return HttpUtils.buildUrlWithParams(HttpUtils.ORDER_LIST_URL, params);
    }

    private OrderInfo findMostRecentOrder() {
        OrderInfo recent = null;
        List<OrderInfo> infos = null;
        if(mTodayOrders.size() > 0) {
            infos = mTodayOrders;
        } else if(mWeekOrders.size() > 0) {
            infos = mWeekOrders;
        } else if(mSevenDayOrders.size() > 0) {
            infos = mSevenDayOrders;
        } else if(mMonthOrders.size() > 0) {
            infos = mMonthOrders;
        } else if(mThirtyDayOrders.size() > 0) {
            infos = mThirtyDayOrders;
        }
        if(infos != null && infos.size() > 0) {
            recent = infos.get(0);
            for(OrderInfo order : infos) {
                if(recent.createtime.compareTo(order.createtime) < 0) {
                    recent = order;
                }
            }
        }
        return recent;
    }

    private void startLoad() {
        for(Callback callback : mCallbacks) {
            callback.onLoadStart();
        }

        mLoadComplete = false;
        mLoadFail = false;
        mCancelLoad = false;
        LoadDataTask task = new LoadDataTask();
        task.execute(buildUrl("0"));
    }

    public static void L(String msg) {
        Log.d(TAG, msg);
    }

    private class LoadDataTask extends AsyncTask<String, Void, Boolean> {
        int totalPage;
        int currentPage;

        @Override
        protected Boolean doInBackground(String... params) {
            if(mCancelLoad) {
                return false;
            }
            String url = params[0];
            OrderListResponse list = mHttpService.executeJsonGetSync(url, OrderListResponse.class);
            if(!mCancelLoad && list != null && list.errcode != null && list.errcode.equals("0")) {
                List<OrderInfo> orders = list.data.order;
                mThirtyDayOrders.addAll(orders);
                for(OrderInfo order : orders) {
                    String createTime = order.createtime.substring(0, order.createtime.indexOf(' '));
                    mDayOrder.get(mMap.get(createTime)).add(order);
                }
                totalPage = list.total_page;
                currentPage = list.current_page;

                if(totalPage-1 == currentPage) {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                    Date month = DateUtils.getFirstDayOfMonth();
                    int monthIndex = mMap.get(formatter.format(month));
                    for (int i = monthIndex; i < PAST_DAY_NUM; i++) {
                        mMonthOrders.addAll(mDayOrder.get(i));
                    }

                    Date week = DateUtils.getFirstDayOfWeek();
                    int weekIndex = mMap.get(formatter.format(week));
                    for (int i = weekIndex; i < PAST_DAY_NUM; i++) {
                        mWeekOrders.addAll(mDayOrder.get(i));
                    }

                    Date sevenDay = DateUtils.getSevenDaysBefore();
                    int sevenDayIndex = mMap.get(formatter.format(sevenDay));
                    for (int i = sevenDayIndex; i < PAST_DAY_NUM; i++) {
                        mSevenDayOrders.addAll(mDayOrder.get(i));
                    }

                    mTodayOrders.addAll(mDayOrder.get(PAST_DAY_NUM-1));
                    mMostRecentOrder = findMostRecentOrder();
                }
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if(aBoolean) {
                if(currentPage < totalPage-1) {
                    LoadDataTask task = new LoadDataTask();
                    task.execute(buildUrl(String.valueOf(currentPage+1)));
                }
                else {
                    for(Callback callback : mCallbacks) {
                        mLoadComplete = true;
                        mLoadFail = false;
                        callback.onLoadComplete();
                    }
                }
            }
            else {
                mLoadFail = true;
            }
        }
    }
}
