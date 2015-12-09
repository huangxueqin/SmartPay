package com.android.smartpay;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by xueqin on 2015/12/5 0005.
 */
public class StatisticActivity extends AppCompatActivity {
    private static final int TIME_TYPE_7 = 0x100;
    private static final int TIME_TYPE_30 = 0x101;
    private static final int CATEGORY_TYPE_NUM = 0x102;
    private static final int CATEGORY_TYPE_MONEY = 0x103;

    private Spinner mSpinnerCategory;
    private Spinner mSpinnerTime;
    private LineChartView mChart;
    private ListView mList;
    private SimpleOrderListAdapter mAdapter;
    private TextView mChartTitle;
    private TextView mInfoTitle;

    private List<Integer> mThirtyDaysOrderNum;
    private List<Float> mThirtyDaysOrderMoney;
    private List<AxisValue> mAxisValues30;
    private List<AxisValue> mAxisValues7;
    private Line mLine30Num;
    private Line mLine7Num;
    private Line mLine30Money;
    private Line mLine7Money;
    private int mTimeType = TIME_TYPE_7;
    private int mCategoryType = CATEGORY_TYPE_NUM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistic);
        setupToolbar();
        mSpinnerCategory = (Spinner) findViewById(R.id.spinner_category);
        ArrayAdapter<CharSequence> adapterCategory = ArrayAdapter.createFromResource(this, R.array.sa_spinner_1, R.layout.toolbar_spinner_title);
        adapterCategory.setDropDownViewResource(R.layout.simple_spinner_item);
        mSpinnerCategory.setAdapter(adapterCategory);
        mSpinnerCategory.setOnItemSelectedListener(mCategorySpinnerItemSelectedListener);
        mSpinnerTime = (Spinner) findViewById(R.id.spinner_time);
        ArrayAdapter<CharSequence> adapterTime = ArrayAdapter.createFromResource(this, R.array.sa_spinner_2, R.layout.toolbar_spinner_title);
        adapterTime.setDropDownViewResource(R.layout.simple_spinner_item);
        mSpinnerTime.setAdapter(adapterTime);
        mSpinnerTime.setOnItemSelectedListener(mTimeSpinnerItemSelectedListener);
        mChart = (LineChartView) findViewById(R.id.chart);
        mList = (ListView) findViewById(R.id.list);
        mChartTitle = (TextView) findViewById(R.id.chart_title);
        mInfoTitle = (TextView) findViewById(R.id.info_title);
        initDataOnce();
        updateViewByType();
    }

    private void initDataOnce() {
        // init core data
        mThirtyDaysOrderMoney = DataLoader.get().getOrderMoneyForThirtyDays();
        mThirtyDaysOrderNum = DataLoader.get().getOrderNumForThirtyDays();

        // init chart horizontal axis values
        mAxisValues30 = new ArrayList<>();
        mAxisValues7 = new ArrayList<>();
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.add(Calendar.DATE, -30);
        for(int i = 0; i < 30; i++) {
            ca.add(Calendar.DATE, 1);
            mAxisValues30.add(new AxisValue(i, formatter.format(ca.getTime()).toCharArray()));
        }
        ca.setTime(new Date());
        ca.add(Calendar.DATE, -7);
        for(int i = 0; i < 7; i++) {
            ca.add(Calendar.DATE, 1);
            mAxisValues7.add(new AxisValue(i, formatter.format(ca.getTime()).toCharArray()));
        }

        // init points of chart
        List<PointValue> numValues30 = new ArrayList<>();
        List<PointValue> moneyValues30 = new ArrayList<>();
        List<PointValue> numValues7 = new ArrayList<>();
        List<PointValue> moneyValues7 = new ArrayList<>();
        for(int i = 0; i < 30; i++) {
            numValues30.add(new PointValue(i, mThirtyDaysOrderNum.get(i)));
            moneyValues30.add(new PointValue(i, mThirtyDaysOrderMoney.get(i)));
        }
        mLine30Num = new Line(numValues30);
        mLine30Money = new Line(moneyValues30);
        for(int i = 0; i < 7; i++) {
            numValues7.add(new PointValue(i, mThirtyDaysOrderNum.get(i+23)));
            moneyValues7.add(new PointValue(i, mThirtyDaysOrderMoney.get(i+23)));
        }
        mLine7Num = new Line(numValues7);
        mLine7Money = new Line(moneyValues7);
        // init list adapter
        mAdapter = new SimpleOrderListAdapter();
        mList.setAdapter(mAdapter);
    }

    private void updateTitlesByType() {
        if(mTimeType == TIME_TYPE_30) {
            if(mCategoryType == CATEGORY_TYPE_MONEY) {
                mChartTitle.setText(R.string.sa_chart_title_month_money);
            } else if(mCategoryType == CATEGORY_TYPE_NUM) {
                mChartTitle.setText(R.string.sa_chart_title_month_num);
            }
        } else if(mTimeType == TIME_TYPE_7) {
            if(mCategoryType == CATEGORY_TYPE_MONEY) {
                mChartTitle.setText(R.string.sa_chart_title_week_money);
            } else if(mCategoryType == CATEGORY_TYPE_NUM) {
                mChartTitle.setText(R.string.sa_chart_title_week_num);
            }
        }

        if(mCategoryType == CATEGORY_TYPE_MONEY) {
            mInfoTitle.setText(R.string.sa_info_title_money);
        } else if(mCategoryType == CATEGORY_TYPE_NUM) {
            mInfoTitle.setText(R.string.sa_info_title_num);
        }
    }

    private void updateViewByType() {
        LineChartData data = new LineChartData();
        Axis axisX = new Axis().setHasLines(true);
        Axis axisY = new Axis().setHasLines(true);
        axisX.setTextColor(Color.WHITE);
        axisY.setTextColor(Color.WHITE);
        List<Line> lines = new ArrayList<>();
        if(mTimeType == TIME_TYPE_7) {
            axisX.setValues(mAxisValues7);
            if(mCategoryType == CATEGORY_TYPE_NUM) {
                lines.add(mLine7Num);
            } else if(mCategoryType == CATEGORY_TYPE_MONEY) {
                lines.add(mLine7Money);
            }
        } else if(mTimeType == TIME_TYPE_30) {
            axisX.setValues(mAxisValues30);
            if(mCategoryType == CATEGORY_TYPE_NUM) {
                lines.add(mLine30Num);
            } else if(mCategoryType == CATEGORY_TYPE_MONEY) {
                lines.add(mLine30Money);
            }
        }
        data.setLines(lines);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        mChart.setLineChartData(data);
        mChart.setZoomEnabled(false);
        Viewport v = new Viewport(mChart.getMaximumViewport());
        v.left = 0;
        v.right = 7;
        mChart.setCurrentViewport(v);
        mChart.setContainerScrollEnabled(false, ContainerScrollType.VERTICAL);
        mChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        updateTitlesByType();
    }

    private void testChart() {
        int arr[] = {1, 3, 5, 1000, 4, 8, 30};

        List<PointValue> values = new ArrayList<>();
        for(int i = 0; i < 30; i++) {
            values.add(new PointValue(i, arr[i%7]));
        }
        Line line = new Line(values);
        List<Line> lines = new ArrayList<>();
        lines.add(line);
        LineChartData data = new LineChartData();
        data.setLines(lines);
        Axis axisX = new Axis().setHasLines(true);
        Axis axisY = new Axis().setHasLines(true);
        axisX.setTextColor(Color.WHITE);
        List<AxisValue> axisValues = new ArrayList<>();
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.add(Calendar.DATE, -30);
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd");
        for(int i = 0; i < 30; i++) {
            ca.add(Calendar.DATE, 1);
            axisValues.add(new AxisValue(i, formatter.format(ca.getTime()).toCharArray()));
        }
        axisX.setValues(axisValues);
        axisY.setTextColor(Color.WHITE);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        mChart.setLineChartData(data);
        mChart.setZoomEnabled(false);
        Viewport v = new Viewport(mChart.getMaximumViewport());
        v.left = 0;
        v.right = 7;
        mChart.setCurrentViewport(v);
        mChart.setContainerScrollEnabled(false, ContainerScrollType.VERTICAL);
        mChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
    }

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.record_title);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Spinner.OnItemSelectedListener mCategorySpinnerItemSelectedListener = new Spinner.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // fix me: hard coded
            int categoryType = mCategoryType;
            if(position == 0) {
                categoryType = CATEGORY_TYPE_NUM;
            } else if(position == 1) {
                categoryType = CATEGORY_TYPE_MONEY;
            }
            if(categoryType != mCategoryType) {
                mCategoryType = categoryType;
                updateViewByType();
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private Spinner.OnItemSelectedListener mTimeSpinnerItemSelectedListener = new Spinner.OnItemSelectedListener() {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // fix me: hard coded
            int timeType = mTimeType;
            if(position == 0) {
                timeType = TIME_TYPE_7;
            } else if(position == 1) {
                timeType = TIME_TYPE_30;
            }
            if(timeType != mTimeType) {
                mTimeType = timeType;
                updateViewByType();
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private class SimpleOrderListAdapter extends BaseAdapter {
        List<Date> dates;
        SimpleDateFormat formatter;

        public SimpleOrderListAdapter() {
            dates = new ArrayList<>();
            Calendar ca = Calendar.getInstance();
            ca.setTime(new Date());
            ca.add(Calendar.DATE, -29);
            for(int i = 0; i < 30; i++) {
                dates.add(ca.getTime());
                ca.add(Calendar.DATE, 1);
            }
            formatter = new SimpleDateFormat("yyyy-MM-dd");
        }

        @Override
        public int getCount() {
            if(mTimeType == TIME_TYPE_30) {
                return 30;
            } else if(mTimeType == TIME_TYPE_7) {
                return 7;
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if(mTimeType == TIME_TYPE_30) {
                if(mCategoryType == CATEGORY_TYPE_NUM) {
                    return mThirtyDaysOrderNum.get(position);
                } else if(mCategoryType == CATEGORY_TYPE_MONEY) {
                    return mThirtyDaysOrderMoney.get(position);
                }
            } else if(mTimeType == TIME_TYPE_7) {
                if(mCategoryType == CATEGORY_TYPE_NUM) {
                    return mThirtyDaysOrderNum.get(23+position);
                } else if(mCategoryType == CATEGORY_TYPE_MONEY) {
                    return mThirtyDaysOrderMoney.get(23+position);
                }
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder = null;
            if(convertView == null) {
                convertView = StatisticActivity.this.getLayoutInflater().inflate(R.layout.list_item_statistic, parent, false);
                convertView.setTag(new Holder(convertView));
            }
            holder = (Holder) convertView.getTag();
            Object item = getItem(position);
            if(mTimeType == TIME_TYPE_30) {
                holder.date.setText(formatter.format(dates.get(position)));
            } else if(mTimeType == TIME_TYPE_7) {
                holder.date.setText(formatter.format(dates.get(23+position)));
            }
            if(mCategoryType == CATEGORY_TYPE_MONEY) {
                holder.info.setText("￥ " + String.format("%.2f", (Float) item));
            } else if(mCategoryType == CATEGORY_TYPE_NUM) {
                holder.info.setText(String.valueOf((Integer) item) + "笔");
            }
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            return convertView;
        }

        private class Holder {
            TextView date;
            TextView info;

            public Holder(View rootView) {
                date = (TextView) rootView.findViewById(R.id.date);
                info = (TextView) rootView.findViewById(R.id.info);
            }
        }
    }

}
