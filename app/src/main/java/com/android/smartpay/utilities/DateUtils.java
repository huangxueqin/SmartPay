package com.android.smartpay.utilities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by ni on 2015/12/7.
 */
public class DateUtils {

    public static int getMonth() {
        return Calendar.getInstance().get(Calendar.MONTH);
    }

    public static int getYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static String getWeekOfDate(Date dt) {
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0)
            w = 0;
        return weekDays[w];
    }

    public static String changeDateFormat(String source, String fmt1, String fmt2) {
        DateFormat df = new SimpleDateFormat(fmt1);
        try {
            Date d = df.parse(source);
            return new SimpleDateFormat(fmt2).format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static Date getSevenDaysBefore() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.add(Calendar.DATE, -7);
        return ca.getTime();
    }

    public static Date getThirtyDaysBefore() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.add(Calendar.DATE, -30);
        return ca.getTime();
    }

    public static Date getFirstDayOfMonth() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.set(Calendar.DAY_OF_MONTH, 1);
        return ca.getTime();
    }

    public static Date getLastDayOfMonth() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        return ca.getTime();
    }

    public static Date getFirstDayOfWeek() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.set(Calendar.DAY_OF_WEEK, 1);
        if (ca.getFirstDayOfWeek() == Calendar.SUNDAY) {
            ca.add(Calendar.DAY_OF_MONTH, +1);
        }
        return ca.getTime();
    }

    public static Date getLastDayOfWeek() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.set(Calendar.DAY_OF_WEEK, ca.getActualMaximum(Calendar.DAY_OF_WEEK));
        if (ca.getFirstDayOfWeek() == Calendar.SUNDAY) {
            ca.add(Calendar.DAY_OF_MONTH, +1);
        }
        return ca.getTime();
    }

}
