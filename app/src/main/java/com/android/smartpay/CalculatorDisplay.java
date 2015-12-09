package com.android.smartpay;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;

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
    private static final String TEXT_RMB = "￥";

    private float mMaxTextSP = -1;
    private float mMinTextSP = -1;
    private Paint mMeasurePaint;
    private Rect mMeasureBounds;
    private float mCurrentTextSP;
    private float mScaleDownNum;
    private int mTotalTextAreaWidth;
    private StringBuffer mContent;
    private int mAction = ACTION_NONE;

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
        mScaleDownNum = (mMaxTextSP - mMinTextSP) / 5;

        mMeasurePaint = new Paint();
        mMeasurePaint.setTypeface(Typeface.DEFAULT);
        mMeasureBounds = new Rect();
        mContent = new StringBuffer();
        mCurrentTextSP = mMaxTextSP;

        setInputType(InputType.TYPE_NULL);
        setGravity(Gravity.RIGHT | Gravity.BOTTOM);
        addTextChangedListener(mTextWatcher);
        setTextSize(mCurrentTextSP);

        appendTail(TEXT_RMB);
    }

    public void appendTail(String c) {
        mContent.append(c);
        mAction = ACTION_APPEND;
        setText(mContent);
    }

    public void removeTail() {
        mContent.setLength(mContent.length() - 1);
        mAction = ACTION_REMOVE;
        setText(mContent);
    }

    public void replaceTail(String c) {
        mContent.replace(mContent.length()-1, mContent.length(), c);
        setText(mContent);
    }



    public void setContent(String s) {
        if(s.length() < mContent.length()-1) {
            mAction = ACTION_REMOVE;
        }
        else if(s.length() > mContent.length()-1) {
            mAction = ACTION_APPEND;
        }
        mContent.replace(1, mContent.length(), s);
        setText(mContent);
    }

    public String getContent() {
        return mContent.substring(1);
    }

    public int getContentLength() {
        return mContent.length()-1;
    }

    public void clearContent() {
        setContent("");
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
            String content = s.toString();
            if(mAction == ACTION_APPEND) {
                mMeasurePaint.setTextSize(spToPixels(getContext(), mCurrentTextSP));
                mMeasurePaint.getTextBounds(content, 0, content.length(), mMeasureBounds);
                float length = mMeasureBounds.width();
                if (mTotalTextAreaWidth - length < spToPixels(getContext(), mCurrentTextSP) / 3) {
                    if (mCurrentTextSP - mScaleDownNum >= mMinTextSP) {
                        mCachedTextSP.push(mCurrentTextSP);
                        mCachedTextLength.push(content.length()-1);
                        mCurrentTextSP -= mScaleDownNum;
                        CalculatorDisplay.this.setTextSize(mCurrentTextSP);
                    }
                    CalculatorDisplay.this.setSelection(mContent.length());
                }
            }
            else if(mAction == ACTION_REMOVE && !mCachedTextSP.empty()) {
                if(content.length() <= mCachedTextLength.peek()) {
                    while(!mCachedTextLength.empty() && content.length() <= mCachedTextLength.peek()) {
                        mCachedTextLength.pop();
                        mCachedTextSP.pop();
                    }
                    mCurrentTextSP = mCachedTextSP.empty() ? mMaxTextSP : mCachedTextSP.peek();
                    CalculatorDisplay.this.setTextSize(mCurrentTextSP);
                }
                CalculatorDisplay.this.setSelection(mContent.length());
            }
            mAction = ACTION_NONE;
        }
    };

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
}
