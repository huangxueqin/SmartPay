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
import com.google.gson.Gson;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by xueqin on 2015/12/8 0008.
 */
public class OrderListAdapter extends BaseAdapter {

    Context context;
    List<OrderInfo> todayOrders;
    List<OrderInfo> weekOrders;
    List<OrderInfo> monthOrders;
    private int type;


    public OrderListAdapter(Context context, int type, List<OrderInfo> todayOrders, List<OrderInfo> weekOrders, List<OrderInfo> monthOrders) {
        this.todayOrders = todayOrders;
        this.weekOrders = weekOrders;
        this.monthOrders = monthOrders;
        this.context = context;
        this.type = type;
    }


    public void setType(int type) {
        if(type != this.type) {
            if(type == Cons.TYPE_DAY) {
                this.type = Cons.TYPE_DAY;
            } else if(type == Cons.TYPE_WEEK) {
                this.type = Cons.TYPE_WEEK;
            } else if(type == Cons.TYPE_MONTH) {
                this.type = Cons.TYPE_MONTH;
            }
            this.notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        if(type == Cons.TYPE_DAY) {
            return todayOrders == null ? 0 : todayOrders.size();
        } else if(type == Cons.TYPE_WEEK) {
            return weekOrders == null ? 0 : weekOrders.size();
        } else if(type == Cons.TYPE_MONTH) {
            return monthOrders == null ? 0 : monthOrders.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        if(type == Cons.TYPE_DAY) {
            return todayOrders.get(position);
        } else if(type == Cons.TYPE_WEEK) {
            return weekOrders.get(position);
        } else if(type == Cons.TYPE_MONTH) {
            return monthOrders.get(position);
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
}
