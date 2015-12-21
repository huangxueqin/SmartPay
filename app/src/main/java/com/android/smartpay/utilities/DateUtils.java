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
    private static final SimpleDateFormat sStandardFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static int getMonth() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        // january is 0
        return ca.get(Calendar.MONTH) + 1;
    }

    public static int getYear() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        return ca.get(Calendar.YEAR);
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

    public static Date getFirstDayOfCurrentMonth() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.set(Calendar.DAY_OF_MONTH, 1);
        return ca.getTime();
    }

    public static Date getLastDayOfCurrentMonth() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        return ca.getTime();
    }

    /**
     *
     * @param month 0 - 11
     * @return
     */
    public static Date getFirstDayOfMonth(int month) {
        if(month < 0 ) month = 0;
        if(month > 11) month = 11;
        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.MONTH, month);
        ca.set(Calendar.DAY_OF_MONTH, 1);
        return ca.getTime();
    }

    /**
     *
     * @param month 0 - 11
     * @return
     */
    public static Date getLastDayOfMonth(int month) {
        if(month < 0 ) month = 0;
        if(month > 11) month = 11;

        Calendar ca = Calendar.getInstance();
        ca.set(Calendar.MONTH, month);
        ca.set(Calendar.DAY_OF_MONTH, ca.getActualMaximum(Calendar.DAY_OF_MONTH));
        return ca.getTime();
    }

    /**
     * monday as the first day
     * sunday as the last day
     * @return
     */
    public static Date getFirstDayOfWeek() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        int w = ca.get(Calendar.DAY_OF_WEEK);
        if(w == 1) {
            // sunday now
            ca.add(Calendar.DATE, -6);
        } else {
            ca.add(Calendar.DATE, w-2);
        }
        return ca.getTime();
    }

    public static Date getLastDayOfWeek() {
        Calendar ca = Calendar.getInstance();
        ca.setTime(new Date());
        int w = ca.get(Calendar.DAY_OF_WEEK);
        if(w == 1) {
            // sunday
            return ca.getTime();
        } else {
            ca.add(Calendar.DATE, 8-w);
            return ca.getTime();
        }
    }

    public static String formatStandard(Date date) {
        return sStandardFormatter.format(date);
    }

    public static Date parseStandard(String date) {
        try {
            return sStandardFormatter.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

}
