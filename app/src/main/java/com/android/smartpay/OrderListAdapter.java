package com.android.smartpay;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.smartpay.jsonbeans.OrderInfo;
import com.android.smartpay.utilities.Cons;
import com.android.smartpay.utilities.DateUtils;
import com.android.smartpay.utilities.OrderUtils;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by xueqin on 2015/12/8 0008.
 */
public class OrderListAdapter extends BaseAdapter {

    Context context;
    List<OrderInfo> orders;

    public OrderListAdapter(Context context, List<OrderInfo> orders) {
        this.context = context;
        this.orders = orders;
        if(orders != null) {
            Collections.sort(orders, orderCompar);
        }
    }

    @Override
    public int getCount() {
        if(orders != null) {
            return orders.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        position = getCount() - 1 - position;
        if(orders != null) {
            return orders.get(position);
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
            convertView = LayoutInflater.from(context).inflate(R.layout.order_list_item, null, false);
            convertView.setTag(new Holder(convertView));
        }
        final OrderInfo order = (OrderInfo) getItem(position);
        holder = (Holder) convertView.getTag();
        holder.price.setText(order.should_pay);
        holder.icon.setImageResource(order.status != 0 ? R.drawable.weipay_success : R.drawable.weipay_failed);
        holder.info.setText(OrderUtils.getOrderSpec(order));
        Date orderDate = OrderUtils.getOrderDate(order);
        if(orderDate != null) {
            SimpleDateFormat format = new SimpleDateFormat("MM-dd");
            holder.date.setText(format.format(orderDate));
            holder.week.setText(DateUtils.getWeekOfDate(orderDate));
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(context, OrderSpecificActivity.class);
                i.putExtra(Cons.ARG_ORDER, new Gson().toJson(order));
                context.startActivity(i);
            }
        });
        return convertView;
    }

    private class Holder {
        public Holder(View root) {
            week = (TextView) root.findViewById(R.id.week);
            date = (TextView) root.findViewById(R.id.date);
            icon = (ImageView) root.findViewById(R.id.icon);
            price = (TextView) root.findViewById(R.id.price);
            info = (TextView) root.findViewById(R.id.info);
        }

        private TextView week;
        private TextView date;
        private ImageView icon;
        private TextView price;
        private TextView info;
    }

    // order list should be sorted with earliest order display firstly. However if we
    // sort the list descendingly, when add a new order, we must add the order at the
    // beginning of the array list. This may result to a huge performance degradation
    // when the list is large. So, here instead, we sort it ascendingly, and do some
    // change in method {@link getItem} to let the displaying reverse.
    private Comparator<OrderInfo> orderCompar = new Comparator<OrderInfo>() {
        @Override
        public int compare(OrderInfo lhs, OrderInfo rhs) {
            return lhs.createtime.compareTo(rhs.createtime);
        }
    };
}
