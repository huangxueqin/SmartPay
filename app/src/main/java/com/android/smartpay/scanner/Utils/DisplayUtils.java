package com.android.smartpay.scanner.Utils;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Created by xueqin on 2015/11/12 0012.
 */
public class DisplayUtils {

    public static float getScaleDensity(Context context) {
        return context.getResources().getDisplayMetrics().scaledDensity;
    }

    public static float getDensity(Context context) {
        return context.getResources().getDisplayMetrics().density;
    }

    public static float dp2px(Context context, float dpValue) {
        return getDensity(context) * dpValue;
    }

    public static float px2dp(Context context, float pxValue) {
        return pxValue / getDensity(context);
    }

    public static float sp2px(Context context, float spValue) {
        return spValue * getScaleDensity(context);
    }

    public static float px2sp(Context context, float pxValue) {
        return pxValue / getScaleDensity(context);
    }

    public static final int getScreenOrientation(Context context) {
        return context.getResources().getConfiguration().orientation;
    }

    public static Point getScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point scr = new Point();
        display.getSize(scr);
        return scr;
    }

    /**
     * @param context
     * @return height / width
     */
    public static float getDisplayRatio(Context context) {
        Point size = getScreenSize(context);
        int h = size.y;
        int w = size.x;
        return h / (float) w;
    }
}
