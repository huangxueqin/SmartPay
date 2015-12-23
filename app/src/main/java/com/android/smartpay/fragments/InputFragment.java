package com.android.smartpay.fragments;


import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.smartpay.R;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.utilities.Cons;

import java.math.BigDecimal;

/**
 * Created by xueqin on 2015/11/26 0026.
 */
public class InputFragment extends BaseFragment implements View.OnClickListener{
    private static final String TAG = "TAG--------->";

    enum Type {
        TYPE_ZERO, TYPE_DIGIT, TYPE_PLUS, TYPE_POINT, TYPE_EQUAL, TYPE_BACK, TYPE_PAY
    }

    private static SparseArray<String> sButtonContent;
    private static SparseArray<Type> sButtonType;
    static {
        sButtonContent = new SparseArray<>(12);
        sButtonContent.put(R.id.digit0, "0"); sButtonContent.put(R.id.digit1, "1");
        sButtonContent.put(R.id.digit2, "2"); sButtonContent.put(R.id.digit3, "3");
        sButtonContent.put(R.id.digit4, "4"); sButtonContent.put(R.id.digit5, "5");
        sButtonContent.put(R.id.digit6, "6"); sButtonContent.put(R.id.digit7, "7");
        sButtonContent.put(R.id.digit8, "8"); sButtonContent.put(R.id.digit9, "9");
        sButtonContent.put(R.id.plus, "+"); sButtonContent.put(R.id.point, ".");

        sButtonType = new SparseArray<>(16);
        sButtonType.put(R.id.digit0, Type.TYPE_ZERO); sButtonType.put(R.id.digit1, Type.TYPE_DIGIT);
        sButtonType.put(R.id.digit2, Type.TYPE_DIGIT); sButtonType.put(R.id.digit3, Type.TYPE_DIGIT);
        sButtonType.put(R.id.digit4, Type.TYPE_DIGIT); sButtonType.put(R.id.digit5, Type.TYPE_DIGIT);
        sButtonType.put(R.id.digit6, Type.TYPE_DIGIT); sButtonType.put(R.id.digit7, Type.TYPE_DIGIT);
        sButtonType.put(R.id.digit8, Type.TYPE_DIGIT); sButtonType.put(R.id.digit9, Type.TYPE_DIGIT);
        sButtonType.put(R.id.back, Type.TYPE_BACK); sButtonType.put(R.id.plus, Type.TYPE_PLUS);
        sButtonType.put(R.id.point, Type.TYPE_POINT); sButtonType.put(R.id.qq_wallet, Type.TYPE_PAY);
        sButtonType.put(R.id.weichat_wallet, Type.TYPE_PAY); sButtonType.put(R.id.equal, Type.TYPE_EQUAL);
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

    private Typeface mTypeface;
    private CalculatorDisplay mDisplay;
    private static final BigDecimal MAX_MONEY_VALUE = new BigDecimal(100000);


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_input, container, false);
        String font = "fonts/HelveticaNeue-Thin.otf";
        AssetManager assets = getActivity().getApplicationContext().getAssets();
        mTypeface = Typeface.createFromAsset(assets, font);
        mDisplay = (CalculatorDisplay) view.findViewById(R.id.display);
        mDisplay.setTypeface(mTypeface);
        mDisplay.setSecondDisplay((TextView) view.findViewById(R.id.second_display));
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
                if(inputEngine.getContentLength() > 0) {
                    inputEngine.reset();
                    mDisplay.clearAll();
                }
                return false;
            }
        });
    }


    InputEngine inputEngine = new InputEngine();
    boolean equalPressed = false;

    @Override
    public void onClick(View v) {
        int id = v.getId();
        Type type = sButtonType.get(id);
        if(type == Type.TYPE_PAY) {
            onPayPressed(id == R.id.weichat_wallet ? Cons.ACTION_WECHAT_PAY : Cons.ACTION_QQ_PAY);
        } else {
            switch (type) {
                case TYPE_BACK:
                    if (equalPressed) {
                        inputEngine.reset();
                        equalPressed = false;
                    } else {
                        inputEngine.removeTail();
                    }
                    mDisplay.setContent(inputEngine.getContent());
                    break;
                case TYPE_EQUAL:
                    if (inputEngine.getContentLength() > 0) {
                        onEqualPressed();
                    }
                    break;
                case TYPE_DIGIT:case TYPE_PLUS:
                case TYPE_POINT:case TYPE_ZERO:
                    if (equalPressed) {
                        equalPressed = false;
                        if (type != Type.TYPE_PLUS) {
                            mDisplay.clearAll();
                            inputEngine.reset();
                        } else {
                            if(mDisplay.maxTextReached()) {
                                equalPressed = true;
                            }
                        }
                    }
                    String buttonContent = sButtonContent.get(id);
                    L(buttonContent);
                    if(mDisplay.maxTextReached()) {
                        L("max Text Reached");
                    }
                    L(inputEngine.getContent());
                    L(inputEngine.printStates());
                    if(!mDisplay.maxTextReached() && inputEngine.append(buttonContent)) {
                        mDisplay.setContent(inputEngine.getContent());
                    }
                    break;
            }
        }
    }

    private void onEqualPressed() {
        if(!inputEngine.isContentNumber()) {
            equalPressed = true;
            inputEngine.collapseToResult();
            mDisplay.setResultWithAnim(inputEngine.getContent());
        }
    }

    private void onPayPressed(int payType) {
        if(inputEngine.isContentNumber()) {
            BigDecimal money = inputEngine.parseResult();
            if(money.compareTo(MAX_MONEY_VALUE) < 0) {
                Intent data = new Intent();
                data.putExtra(Cons.ARG_MONEY, money.floatValue());
                mListener.onEvent(this, payType, data);
            } else {
                T("金额过大, 请检查输入是否有误");
            }
        }
        else {
            T("输入金额不合法，请检查输入是否有误");
        }
    }

    @Override
    public void updateUserInfo(LoginResponse.ShopUser user) {

    }

    private void T(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }
    private static void L(String msg) {
        Log.d(TAG, msg);
    }
}
