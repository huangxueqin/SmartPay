package com.android.smartpay.scanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

import com.android.smartpay.R;
import com.android.smartpay.scanner.Utils.DisplayUtils;

/**
 * Created by xueqin on 2015/11/27 0027.
 */
public final class ViewFinderView extends ViewGroup {
    private static final String TAG = "TAG------------->";
    private static final int LASER_WIDTH_DP = 3;
    private static final int MONEY_SP = 35;
    private static final int PROMPT_SP = 20;
    private static final int MONEY_MARGIN_TOP_DP = 20;
    private static final int PROMPT_MARGIN_TOP_DP = 20;
    private static final int WINDOW_WIDTH_DP = 300;
    private static final int WINDOW_HEIGHT_DP = 300;
    private static final int CORNER_WIDTH_DP = 5;
    private static final float WINDOW_CENTER_X = 0.5f;
    private static final float WINDOW_CENTER_Y = 0.33f;
    private static final int MASK_ALPHA = 125;

    private static final int FRAME_STROKE_WIDTH = 2;

    private int mMaskColor;
    private int mFrameColor;
    private int mCornerColor;
    private int mLaserWidth;
    private int mMaskAlpha;
    private int mCornerWidth;
    private int mWindowWidth;
    private int mWindowHeight;
    private float mWindowCenterX;
    private float mWindowCenterY;
    private Rect mFrameRect;

    private View mFrameView;
    private View mLaserView;
    private TranslateAnimation mAnimation;

    private int mMoneyColor;
    private int mMoneySize;
    private String mMoney = "￥";
    private int mPromptColor;
    private int mPromptSize;
    private String mPrompt = "扫描消费者的付款码完成交易";
    private Rect mMeasureRect = new Rect();

    public ViewFinderView(Context context) {
        this(context, null);
    }

    public ViewFinderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewFinderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ViewFinderView);
        mMaskColor = ta.getColor(R.styleable.ViewFinderView_mask_color, context.getResources().getColor(R.color.viewfinder_mask));
        mFrameColor = ta.getColor(R.styleable.ViewFinderView_frame_color, context.getResources().getColor(R.color.viewfinder_frame));
        mCornerColor = ta.getColor(R.styleable.ViewFinderView_corner_color, context.getResources().getColor(R.color.viewfinder_frame));
        mMaskAlpha = ta.getIndex(R.styleable.ViewFinderView_mask_alpha);
        if(mMaskAlpha <= 0 || mMaskAlpha >= 225) mMaskAlpha = MASK_ALPHA;
        mWindowWidth = ta.getDimensionPixelSize(R.styleable.ViewFinderView_window_width, (int) DisplayUtils.dp2px(context, WINDOW_WIDTH_DP));
        mWindowHeight = ta.getDimensionPixelSize(R.styleable.ViewFinderView_window_height, (int) DisplayUtils.dp2px(context, WINDOW_HEIGHT_DP));
        mWindowCenterX = ta.getFloat(R.styleable.ViewFinderView_window_centerX, WINDOW_CENTER_X);
        mWindowCenterY = ta.getFloat(R.styleable.ViewFinderView_window_centerY, WINDOW_CENTER_Y);
        mCornerWidth = ta.getDimensionPixelSize(R.styleable.ViewFinderView_corner_width, (int) DisplayUtils.dp2px(context, CORNER_WIDTH_DP));
        mMoneySize = ta.getDimensionPixelSize(R.styleable.ViewFinderView_money_size, (int) DisplayUtils.sp2px(context, MONEY_SP));
        mMoneyColor = ta.getColor(R.styleable.ViewFinderView_money_color, context.getResources().getColor(R.color.viewfinder_frame));
        mPromptSize = ta.getDimensionPixelSize(R.styleable.ViewFinderView_prompt_size, (int) DisplayUtils.sp2px(context, PROMPT_SP));
        mPromptColor = ta.getColor(R.styleable.ViewFinderView_prompt_color, Color.WHITE);
        ta.recycle();
        mFrameRect = new Rect();
        mLaserWidth = (int) DisplayUtils.dp2px(context, LASER_WIDTH_DP);

        mFrameView = new FrameView(context);
        addView(mFrameView);

        mLaserView = new View(context);
        mLaserView.setBackgroundResource(R.drawable.laser);
        addView(mLaserView);

        mAnimation = new TranslateAnimation(0, 0, 0, mWindowHeight - mLaserWidth);
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.REVERSE);
        mAnimation.setDuration(2000);
        mAnimation.setInterpolator(new DecelerateInterpolator());

    }

    public void setMoney(String m) {
        mMoney += m;
    }

    // must called after ondraw
    public Rect getWindowRect() {
        return mFrameRect;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        for(int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if(child == mFrameView) {
                child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            }
            else if(child == mLaserView) {
                child.measure(MeasureSpec.makeMeasureSpec(mWindowWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mLaserWidth, MeasureSpec.EXACTLY));
            }
        }
        setMeasuredDimension(width, height);
        int frameLeft = (int) ((width - mWindowWidth) * mWindowCenterX);
        int frameTop = (int) ((height - mWindowHeight) * mWindowCenterY);
        mFrameRect.set(frameLeft, frameTop, frameLeft + mWindowWidth, frameTop + mWindowHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for(int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if(child == mFrameView) {
                child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
            }
            else if(child == mLaserView) {
                int left = mFrameRect.left;
                int top = mFrameRect.top;
                child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
            }
        }
    }

    public void startScanAnimation() {
        mLaserView.setVisibility(VISIBLE);
        mLaserView.startAnimation(mAnimation);
    }

    public void stopScanAnimation() {
        mLaserView.clearAnimation();
        mLaserView.setVisibility(GONE);
    }

    private class FrameView extends View {
        private Paint mPaint;

        public FrameView(Context context) {
            super(context);
            mPaint = new Paint();
        }

        @Override
        protected void onDraw(Canvas canvas) {
//            Log.d(TAG, "frame view onDraw running");
            int width = getWidth();
            int height = getHeight();
            // draw mask
            mPaint.setColor(mMaskColor);
            if(Color.alpha(mMaskColor) == 225) {
                mPaint.setAlpha(mMaskAlpha);
            }
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, width, mFrameRect.top, mPaint);
            canvas.drawRect(0, mFrameRect.bottom, width, height, mPaint);
            canvas.drawRect(0, mFrameRect.top, mFrameRect.left, mFrameRect.bottom, mPaint);
            canvas.drawRect(mFrameRect.right, mFrameRect.top, width, mFrameRect.bottom, mPaint);

            // draw frame
            mPaint.setColor(mFrameColor);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(FRAME_STROKE_WIDTH);
            mPaint.setAlpha(225);
            canvas.drawRect(mFrameRect, mPaint);

            // draw for corner
            mPaint.setColor(mCornerColor);
            mPaint.setStyle(Paint.Style.FILL);
            // top-left corner
            canvas.drawRect(mFrameRect.left, mFrameRect.top,
                    mFrameRect.left + mCornerWidth, mFrameRect.top + FRAME_STROKE_WIDTH * 2, mPaint);
            canvas.drawRect(mFrameRect.left, mFrameRect.top + FRAME_STROKE_WIDTH * 2,
                    mFrameRect.left + FRAME_STROKE_WIDTH * 2, mFrameRect.top + mCornerWidth, mPaint);
            // top-right corner
            canvas.drawRect(mFrameRect.right - mCornerWidth, mFrameRect.top,
                    mFrameRect.right, mFrameRect.top + FRAME_STROKE_WIDTH*2, mPaint);
            canvas.drawRect(mFrameRect.right - FRAME_STROKE_WIDTH * 2, mFrameRect.top+FRAME_STROKE_WIDTH * 2,
                    mFrameRect.right, mFrameRect.top + mCornerWidth, mPaint);
            // bottom-left corner
            canvas.drawRect(mFrameRect.left, mFrameRect.bottom - mCornerWidth,
                    mFrameRect.left + FRAME_STROKE_WIDTH*2, mFrameRect.bottom, mPaint);
            canvas.drawRect(mFrameRect.left + FRAME_STROKE_WIDTH * 2, mFrameRect.bottom - FRAME_STROKE_WIDTH * 2,
                    mFrameRect.left + mCornerWidth, mFrameRect.bottom, mPaint);
            // bottom-right corner
            canvas.drawRect(mFrameRect.right - mCornerWidth, mFrameRect.bottom - FRAME_STROKE_WIDTH*2,
                    mFrameRect.right, mFrameRect.bottom, mPaint);
            canvas.drawRect(mFrameRect.right - FRAME_STROKE_WIDTH * 2, mFrameRect.bottom - mCornerWidth,
                    mFrameRect.right, mFrameRect.bottom - FRAME_STROKE_WIDTH * 2, mPaint);

            mPaint.setAntiAlias(true);
            // draw money and prompt text
            int marginTop = (int) DisplayUtils.dp2px(getContext(), MONEY_MARGIN_TOP_DP);
            mPaint.setTextSize(mMoneySize);
            mPaint.setColor(mMoneyColor);
            mPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mPaint.getTextBounds(mMoney, 0, mMoney.length(), mMeasureRect);
            int moneyOrigin = (width - mMeasureRect.width()) / 2;
            int moneyBaseline = mFrameRect.bottom + marginTop + mMeasureRect.height();
            canvas.drawText(mMoney, moneyOrigin, moneyBaseline, mPaint);

            marginTop = (int) DisplayUtils.dp2px(getContext(), PROMPT_MARGIN_TOP_DP);
            mPaint.setTextSize(mPromptSize);
            mPaint.setColor(mPromptColor);
            mPaint.setAlpha(120);
            mPaint.setTypeface(Typeface.DEFAULT);
            mPaint.getTextBounds(mPrompt, 0, mPrompt.length(), mMeasureRect);
            int promptOrigin = (width - mMeasureRect.width()) / 2;
            int promptBaseline = moneyBaseline + marginTop + mMeasureRect.height();
            canvas.drawText(mPrompt, promptOrigin, promptBaseline, mPaint);

        }


    }

//    private static class LaserInterpolator {
//        private int k;
//        private int[] offset;
//
//        public LaserInterpolator(int l, int t) {
//            offset = new int[t];
//            float a = l / ((float) t * t);
//            for(int i = 0; i < t; i++) {
//                offset[i] = l - (int )(a * (t-i) * (t-i));
//            }
//            k = 0;
//        }
//
//        public int getOffset() {
//            int r = offset[k++];
//            if(k >= offset.length) k = 0;
//            return r;
//        }
//
//        public void reset() {
//            k = 0;
//        }
//    }
}
