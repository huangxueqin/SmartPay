package com.android.smartpay.fragments;


import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.smartpay.CalculatorDisplay;
import com.android.smartpay.FragmentListener;
import com.android.smartpay.R;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.utilities.Cons;

import java.util.Stack;

/**
 * Created by xueqin on 2015/11/26 0026.
 */
public class InputFragment extends BaseFragment implements View.OnClickListener{
    private static final String TAG = "TAG--------->";

    private static final int TYPE_ZERO = 0x1 << 6;
    private static final int TYPE_DIGIT = 0x1;
    private static final int TYPE_PLUS = 0x1 << 1;
    private static final int TYPE_POINT = 0x1 << 2;
    private static final int TYPE_EQUAL = 0x1 << 3;
    private static final int TYPE_BACK = 0x1 << 4;
    private static final int TYPE_PAY = 0x1 << 5;

    private static SparseArray<String> sButtonContent;
    private static SparseArray<Integer> sButtonType;
    static {
        sButtonContent = new SparseArray<>(12);
        sButtonContent.put(R.id.digit0, "0"); sButtonContent.put(R.id.digit1, "1");
        sButtonContent.put(R.id.digit2, "2"); sButtonContent.put(R.id.digit3, "3");
        sButtonContent.put(R.id.digit4, "4"); sButtonContent.put(R.id.digit5, "5");
        sButtonContent.put(R.id.digit6, "6"); sButtonContent.put(R.id.digit7, "7");
        sButtonContent.put(R.id.digit8, "8"); sButtonContent.put(R.id.digit9, "9");
        sButtonContent.put(R.id.plus, "+"); sButtonContent.put(R.id.point, ".");

        sButtonType = new SparseArray<>(16);
        sButtonType.put(R.id.digit0, TYPE_ZERO); sButtonType.put(R.id.digit1, TYPE_DIGIT);
        sButtonType.put(R.id.digit2, TYPE_DIGIT); sButtonType.put(R.id.digit3, TYPE_DIGIT);
        sButtonType.put(R.id.digit4, TYPE_DIGIT); sButtonType.put(R.id.digit5, TYPE_DIGIT);
        sButtonType.put(R.id.digit6, TYPE_DIGIT); sButtonType.put(R.id.digit7, TYPE_DIGIT);
        sButtonType.put(R.id.digit8, TYPE_DIGIT); sButtonType.put(R.id.digit9, TYPE_DIGIT);
        sButtonType.put(R.id.back, TYPE_BACK); sButtonType.put(R.id.plus, TYPE_PLUS);
        sButtonType.put(R.id.point, TYPE_POINT); sButtonType.put(R.id.qq_wallet, TYPE_PAY);
        sButtonType.put(R.id.weichat_wallet, TYPE_PAY); sButtonType.put(R.id.equal, TYPE_EQUAL);
    }

    private Button digit0;
    private Button digit1;
    private Button digit2;
    private Button digit3;
    private Button digit4;
    private Button digit5;
    private Button digit6;
    private Button digit7;
    private Button digit8;
    private Button digit9;
    private Button point;
    private Button plus;
    private ImageButton back;
    private Button equal;
    private Button qqWallet;
    private Button wechatWallet;
    private CalculatorDisplay mDisplay;
    private Typeface mTypeface;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);
        String font = "fonts/HelveticaNeue-Thin.otf";
        AssetManager assets = getActivity().getApplicationContext().getAssets();
        mTypeface = Typeface.createFromAsset(assets, font);
        mDisplay = (CalculatorDisplay) view.findViewById(R.id.display);
        mDisplay.setTypeface(mTypeface);
        initInputPad(view);
        return view;
    }

    // below handle button event
    private void initInputPad(View rootView) {
        digit0 = (Button) rootView.findViewById(R.id.digit0);
        digit1 = (Button) rootView.findViewById(R.id.digit1);
        digit2 = (Button) rootView.findViewById(R.id.digit2);
        digit3 = (Button) rootView.findViewById(R.id.digit3);
        digit4 = (Button) rootView.findViewById(R.id.digit4);
        digit5 = (Button) rootView.findViewById(R.id.digit5);
        digit6 = (Button) rootView.findViewById(R.id.digit6);
        digit7 = (Button) rootView.findViewById(R.id.digit7);
        digit8 = (Button) rootView.findViewById(R.id.digit8);
        digit9 = (Button) rootView.findViewById(R.id.digit9);
        point = (Button) rootView.findViewById(R.id.point);
        plus = (Button) rootView.findViewById(R.id.plus);
        back = (ImageButton) rootView.findViewById(R.id.back);
        equal = (Button) rootView.findViewById(R.id.equal);
        qqWallet = (Button) rootView.findViewById(R.id.qq_wallet);
        wechatWallet = (Button) rootView.findViewById(R.id.weichat_wallet);
        digit0.setOnClickListener(this); digit0.setTypeface(mTypeface);
        digit1.setOnClickListener(this); digit1.setTypeface(mTypeface);
        digit2.setOnClickListener(this); digit2.setTypeface(mTypeface);
        digit3.setOnClickListener(this); digit3.setTypeface(mTypeface);
        digit4.setOnClickListener(this); digit4.setTypeface(mTypeface);
        digit5.setOnClickListener(this); digit5.setTypeface(mTypeface);
        digit6.setOnClickListener(this); digit6.setTypeface(mTypeface);
        digit7.setOnClickListener(this); digit7.setTypeface(mTypeface);
        digit8.setOnClickListener(this); digit8.setTypeface(mTypeface);
        digit9.setOnClickListener(this); digit9.setTypeface(mTypeface);
        point.setOnClickListener(this); point.setTypeface(mTypeface);
        plus.setOnClickListener(this);  plus.setTypeface(mTypeface);
        back.setOnClickListener(this);
        equal.setOnClickListener(this); equal.setTypeface(mTypeface);
        qqWallet.setOnClickListener(this);
        wechatWallet.setOnClickListener(this);
        back.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(mDisplay.getContentLength() > 0) {
                    mDisplay.clearContent();
                    mStates.clear();
                    mState = STATE_START;
                    return true;
                }
                return false;
            }
        });
    }

    private static final int STATE_START = 0;
    private static final int STATE_LEAD_POINT = 1;
    private static final int STATE_LEAD_ZERO = 3;
    private static final int STATE_INTEGER = 4;
    private static final int STATE_DECIMAL = 5;
    private static final int STATE_PLUS = 6;
    private int mState = STATE_START;
    private Stack<Integer> mStates = new Stack<>();
    private boolean mEqualPressed = false;
    @Override
    public void onClick(View v) {
        int id = v.getId();
        int type = sButtonType.get(id);
        if(type == TYPE_PAY) {
            String result = mDisplay.getContent();
            if((mState == STATE_DECIMAL || mState == STATE_INTEGER) && !result.contains("+")) {
                float money = parseResult(mDisplay.getContent());
                Intent data = new Intent();
                data.putExtra(Cons.ARG_MONEY, money);
                int action = id == R.id.weichat_wallet ? Cons.ACTION_WECHAT_PAY : Cons.ACTION_QQ_PAY;
                mListener.onEvent(this, action, data);
            }
            else {
                Toast.makeText(getContext(), "invalid money", Toast.LENGTH_SHORT).show();
            }
        } else if(type == TYPE_BACK) {
            if(mEqualPressed) {
                clearState();
            }
            else if(mState != STATE_START) {
                mDisplay.removeTail();
                if(mStates.empty()) {
                    throw new RuntimeException("State error");
                }
                mState = mStates.peek();
                mStates.pop();
            }
        } else if(type == TYPE_EQUAL) {
            if(mState != STATE_START) {
                mEqualPressed = true;
//                Log.d(TAG, "expr = " + mDisplay.getContent());
                float result = parseResult(mDisplay.getContent());
                mDisplay.setContent(String.format("%.2f", result));
//                Log.d(TAG, "result = " + parseResult(mDisplay.getContent()));
                mState = STATE_DECIMAL;
                // restore states, since result is always formatted like 'xxx.xx', so restore is easy
                mStates.clear();
                mStates.push(STATE_START);
                if(result < 1) {
                    // if result < 1, means display content like 0.xx
                    mStates.push(STATE_LEAD_ZERO);
                }
                else {
                    int l = mDisplay.getContentLength() - 3;
                    for (int i = 0; i < l; i++) mStates.push(STATE_INTEGER);
                }
                // last three char '.xx',
                // so 2 decimal states should be pushed into stack
                // because the last char is represented by mState
                mStates.push(STATE_DECIMAL);
                mStates.push(STATE_DECIMAL);
            }
        } else {
            if(mEqualPressed) {
                // if press '+' we continue calculator, else we clear screen
                if(mEqualPressed) {
                    if(type == TYPE_PLUS) {
                        mEqualPressed = false;
                    }
                    else {
                        clearState();
                    }
                }
            }
            switch (mState) {
                case STATE_START:
                    if (type == TYPE_POINT || type == TYPE_ZERO || type == TYPE_DIGIT) {
                        mDisplay.appendTail(sButtonContent.get(id));
                        if (type == TYPE_POINT) mState = STATE_LEAD_POINT;
                        else if (type == TYPE_ZERO) mState = STATE_LEAD_ZERO;
                        else if (type == TYPE_DIGIT) mState = STATE_INTEGER;
                        mStates.push(STATE_START);
                    }
                    break;
                case STATE_LEAD_POINT:
                    if (type == TYPE_ZERO || type == TYPE_DIGIT) {
                        mDisplay.appendTail(sButtonContent.get(id));
                        mState = STATE_DECIMAL;
                        mStates.push(STATE_LEAD_POINT);
                    }
                    break;
                case STATE_LEAD_ZERO:
                    if (type == TYPE_DIGIT || type == TYPE_POINT) {
                        if (type == TYPE_POINT) {
                            mDisplay.appendTail(sButtonContent.get(id));
                            mState = STATE_DECIMAL;
                            mStates.push(STATE_LEAD_ZERO);
                        } else {
                            mDisplay.replaceTail(sButtonContent.get(id));
                            mState = STATE_INTEGER;
                        }
                    }
                    break;
                case STATE_INTEGER:
                    mDisplay.appendTail(sButtonContent.get(id));
                    if (type == TYPE_DIGIT) mState = STATE_INTEGER;
                    else if (type == TYPE_PLUS) mState = STATE_PLUS;
                    else if (type == TYPE_POINT) mState = STATE_DECIMAL;
                    else if (type == TYPE_ZERO) mState = STATE_INTEGER;
                    mStates.push(STATE_INTEGER);
                    break;
                case STATE_DECIMAL:
                    if (type == TYPE_DIGIT || type == TYPE_ZERO || type == TYPE_PLUS) {
                        mDisplay.appendTail(sButtonContent.get(id));
                        if(type == TYPE_DIGIT) mState = STATE_DECIMAL;
                        else if(type == TYPE_ZERO) mState = STATE_DECIMAL;
                        else mState = STATE_PLUS;
                        mStates.push(STATE_DECIMAL);
                    }
                    break;
                case STATE_PLUS:
                    if(type == TYPE_DIGIT || type == TYPE_ZERO || type == TYPE_POINT) {
                        mDisplay.appendTail(sButtonContent.get(id));
                        if(type == TYPE_DIGIT) mState = STATE_INTEGER;
                        else if(type == TYPE_ZERO) mState = STATE_LEAD_ZERO;
                        else if(type == TYPE_POINT) mState = STATE_LEAD_POINT;
                        mStates.push(STATE_PLUS);
                    }
                    break;
            }
        }
    }

    private void clearState() {
        mDisplay.setContent("");
        mState = STATE_START;
        mStates.clear();
        mEqualPressed = false;
    }

    private float parseResult(String expr) {
        return parseResult(expr, 0);
    }

    private float parseResult(String expr, int start) {
        if(start >= expr.length()) return 0;
        int e = start;
        while(e < expr.length() && expr.charAt(e) != '+') e++;
        return Float.valueOf(expr.substring(start, e)) + parseResult(expr, e+1);
    }


    @Override
    public void updateUserInfo(LoginResponse.ShopUser user) {

    }
}
