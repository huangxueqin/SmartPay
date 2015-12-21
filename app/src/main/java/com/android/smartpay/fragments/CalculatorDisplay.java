package com.android.smartpay.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Interpolator;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.TextView;

import com.android.smartpay.R;

import java.util.Stack;

/**
 * Created by xueqin on 2015/11/26 0026.
 */
public class CalculatorDisplay extends EditText {
    private static final String TAG = "TAG------------>";
    private static final int ACTION_NONE = 0xFF;
    private static final int ACTION_APPEND = 0x100;
    private static final int ACTION_REMOVE = 0x101;
    private static final int DEFAULT_MAX_TEXT_SP = 60;
    private static final int DEFAULT_MIN_TEXT_SP = 30;
    private static final int MIN_SCALE_DOWN_NUM = 3;
    private static final String TEXT_RMB = "￥";
    private static final String[] digits = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final String TEXT_PLUS = "+";
    private static final String TEXT_POINT = ".";

    private float mMaxTextSP = -1;
    private float mMinTextSP = -1;
    private Paint mMeasurePaint;
    private Rect mMeasureBounds;
    private float mCurrentTextSP;
    private float mScaleDownNum;
    private int mTotalTextAreaWidth;
    private int mMaxMinSingleTextWidth;

    private StringBuffer mContent = new StringBuffer(TEXT_RMB);
    private int mAction = ACTION_NONE;
    private TextView mSecondDisplay;

    private Stack<Float> mCachedTextSP = new Stack<>();
    private Stack<Integer> mCachedTextLength = new Stack<>();

    public CalculatorDisplay(Context context) {
        this(context, null);
    }

    public CalculatorDisplay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalculatorDisplay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if(attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CalculatorDisplay);
            mMaxTextSP = ta.getInt(R.styleable.CalculatorDisplay_max_text_size, DEFAULT_MAX_TEXT_SP);
            mMinTextSP = ta.getInt(R.styleable.CalculatorDisplay_min_text_size, DEFAULT_MIN_TEXT_SP);
            ta.recycle();
        }
        else {
            mMaxTextSP = DEFAULT_MAX_TEXT_SP;
            mMinTextSP =  DEFAULT_MIN_TEXT_SP;
        }
        mScaleDownNum = Math.max(MIN_SCALE_DOWN_NUM, (mMaxTextSP - mMinTextSP) / 5);

        mMeasurePaint = new Paint();
        mMeasurePaint.setTypeface(Typeface.DEFAULT);
        mMeasureBounds = new Rect();
        mCurrentTextSP = mMaxTextSP;

        setInputType(InputType.TYPE_NULL);
        setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        addTextChangedListener(mTextWatcher);
        setTextSize(mCurrentTextSP);
//        setSingleLine(false);
//        setHorizontallyScrolling(false);
        initMaxMinSingleTextWidth();
        setText(mContent);
    }

    private void initMaxMinSingleTextWidth() {
        mMaxMinSingleTextWidth = measureTextLength(TEXT_PLUS, 0, 1, spToPixels(getContext(), mMinTextSP));
        mMaxMinSingleTextWidth = Math.max(mMaxMinSingleTextWidth, measureTextLength(TEXT_POINT, 0, 1, spToPixels(getContext(), mMinTextSP)));
        for(int i = 0; i < 10; i++) {
            mMaxMinSingleTextWidth = Math.max(mMaxMinSingleTextWidth, measureTextLength(digits[i], 0, 1, spToPixels(getContext(), mMinTextSP)));
        }
    }

    public void setContent(String s) {
        Editable content = getText();
        if(s.length() < content.length()-1) {
            mAction = ACTION_REMOVE;
        }
        else if(s.length() > content.length()-1) {
            mAction = ACTION_APPEND;
        }
        mContent.setLength(1);
        mContent.append(s);
        setText(mContent);
    }

    public void clearAll() {
        setContent("");
        if(mSecondDisplay != null) {
            mSecondDisplay.setText("");
        }
    }

    public boolean maxTextReached() {
        String content = getText().toString();
        return mTotalTextAreaWidth - measureTextLength(content, 0, content.length(), spToPixels(getContext(), mMinTextSP))
                < mMaxMinSingleTextWidth;
//        return false;
    }

    private int measureTextLength(String content) {
        return measureTextLength(content, 0, content.length(), spToPixels(getContext(), mCurrentTextSP));
    }

    private int measureTextLength(String content, int start, int end, float textSize) {
        mMeasurePaint.setTextSize(textSize);
        mMeasurePaint.getTextBounds(content, start, end, mMeasureBounds);
        return mMeasureBounds.width();
    }

    private void setTextSizeForAction(String content) {
        float fromSp = mCurrentTextSP;
        if(mAction == ACTION_APPEND) {
            float length = measureTextLength(content);
            if (mTotalTextAreaWidth - length < spToPixels(getContext(), mCurrentTextSP) / 3) {
                if (mCurrentTextSP > mMinTextSP) {
                    mCachedTextSP.push(mCurrentTextSP);
                    mCachedTextLength.push(content.length() - 1);
                    mCurrentTextSP = Math.max(mMinTextSP, mCurrentTextSP - mScaleDownNum);
//                    CalculatorDisplay.this.setTextSize(mCurrentTextSP);
                    changeTextSizeAnimatedly(fromSp, mCurrentTextSP);
                }
                else {

                }
            }
            CalculatorDisplay.this.setSelection(getText().length());
        } else if(mAction == ACTION_REMOVE) {
            if(!mCachedTextLength.empty() && content.length() <= mCachedTextLength.peek()) {
                while(!mCachedTextLength.empty() && content.length() < mCachedTextLength.peek()) {
                    mCachedTextLength.pop();
                    mCachedTextSP.pop();
                }
                mCurrentTextSP = mCachedTextSP.empty() ? mMaxTextSP : mCachedTextSP.peek();
                if(!mCachedTextSP.empty()) {
                    mCachedTextSP.pop();
                    mCachedTextLength.pop();
                }
//                CalculatorDisplay.this.setTextSize(mCurrentTextSP);
                changeTextSizeAnimatedly(fromSp, mCurrentTextSP);
            }
            CalculatorDisplay.this.setSelection(getText().length());
        }
        mAction = ACTION_NONE;
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
        @Override
        public void afterTextChanged(Editable s) {
            setTextSizeForAction(s.toString());
        }
    };

    public void setSecondDisplay(TextView secondDisp) {
        mSecondDisplay = secondDisp;
        if(mSecondDisplay != null) {
            mSecondDisplay.setTextSize(mMinTextSP);
            mSecondDisplay.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            mSecondDisplay.setSingleLine(false);
            mSecondDisplay.setTypeface(getTypeface());
        }
    }

    private void changeTextSizeAnimatedly(float fromSize, float toSize) {
        final int origLayerType = getLayerType();
        final Paint origPaint = getPaint();
        setLayerType(View.LAYER_TYPE_HARDWARE, origPaint);
        Animator an = ObjectAnimator.ofFloat(this, "textSize", fromSize, toSize);
        an.setDuration(200);
        an.setInterpolator(new DecelerateInterpolator());
        an.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                setLayerType(origLayerType, origPaint);
            }
        });
        AnimatorSet as = new AnimatorSet();
        as.play(an);
        as.start();
    }

    public void setResultWithAnim(final String content) {
        final int origLayerType = getLayerType();
        final Paint origPaint = getPaint();
        setLayerType(View.LAYER_TYPE_HARDWARE, origPaint);
        Animator animUp = ObjectAnimator.ofFloat(this, "translationY", getHeight(), 0f);
        animUp.setDuration(300);
        animUp.setInterpolator(new DecelerateInterpolator());
        animUp.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                setLayerType(origLayerType, origPaint);
            }
        });
        AnimatorSet as = new AnimatorSet();

        if(mSecondDisplay != null) {
            final int secondLayerType = mSecondDisplay.getLayerType();
            final Paint secondPaint = mSecondDisplay.getPaint();
            setLayerType(View.LAYER_TYPE_HARDWARE, secondPaint);
            mSecondDisplay.setText("");
            final Animator sdAnimUp = ObjectAnimator.ofFloat(mSecondDisplay, "translationY", (mSecondDisplay.getHeight() + getHeight())/2.0f, 0f);
            sdAnimUp.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    mSecondDisplay.setText(mContent.substring(1) + "=");
                    CalculatorDisplay.this.setContent(content);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    setLayerType(secondLayerType, secondPaint);
                }
            });
            sdAnimUp.setDuration(300);
            sdAnimUp.setInterpolator(new DecelerateInterpolator());
            Animator sdAnimTextSize = ObjectAnimator.ofFloat(mSecondDisplay, "textSize", mCurrentTextSP, mMinTextSP);
            sdAnimTextSize.setDuration(300);
            Animator sdAnimTextColor = ObjectAnimator.ofInt(mSecondDisplay, "textColor", Color.rgb(0x88, 0x88, 0x88), Color.rgb(0xCB, 0xCB, 0xCB));
            sdAnimTextColor.setDuration(300);
            as.playTogether(animUp, sdAnimTextColor, sdAnimTextSize, sdAnimUp);
        } else {
            as.playTogether(animUp);
        }
        as.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int padding = getPaddingLeft() + getPaddingRight();
        mTotalTextAreaWidth = width - padding;
    }

    private static float pixelsToSp(Context context, float px) {
        float scaleDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return px / scaleDensity;
    }

    private static float spToPixels(Context context, float sp) {
        float scaleDensity = context.getResources().getDisplayMetrics().scaledDensity;
        return sp * scaleDensity;
    }

    private static void L(String msg) {
        Log.d("calculator disp--->", msg);
    }
}
