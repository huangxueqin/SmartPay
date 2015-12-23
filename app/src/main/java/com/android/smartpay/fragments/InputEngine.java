package com.android.smartpay.fragments;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by xueqin on 2015/12/18 0018.
 */
public class InputEngine {
    enum Type {
        TYPE_INVALID, TYPE_ZERO, TYPE_DIGIT, TYPE_POINT, TYPE_PLUS
    }

    enum State {
        STATE_INVALID, STATE_START, STATE_LEAD_POINT, STATE_LEAD_ZERO, STATE_INTEGER, STATE_DECIMAL, STATE_PLUS
    }
    private StringBuffer mContent = new StringBuffer();
    private State mCurState = State.STATE_START;
    private Stack<State> mStates = new Stack<>();
    private int mDecimalDigitNum = 0;

    public boolean append(String s) {
        if(s == null || s.length() != 1) {
            return false;
        }
        return append(s.charAt(0));
    }

    public boolean append(char c) {
        State state = append(getType(c));
        if(state == State.STATE_INVALID) {
            return false;
        } else {
            mStates.push(mCurState);
            mCurState = state;
            mContent.append(c);
            return true;
        }
    }

    public void removeTail() {
        if(mContent.length() > 0) {
            mContent.setLength(mContent.length() - 1);
            onStatePoped(mCurState);
            mCurState = mStates.peek();
            mStates.pop();
        }
    }

    public void reset() {
        mStates.clear();
        mCurState = State.STATE_START;
        mContent.setLength(0);
    }

    public String getContent() {
        return mContent.toString();
    }

    public String printStates() {
        List<State> states = new ArrayList<>(mStates);
        String result = "";
        for(State state : states) {
            result += state.toString();
        }
        return result;
    }

    public int getContentLength() {
        return mContent.length();
    }

    public boolean isContentNumber() {
        if(mCurState == State.STATE_LEAD_POINT || mCurState == State.STATE_START || mCurState == State.STATE_PLUS
                || mCurState == State.STATE_INVALID) {
            return false;
        }
        for(int i = 0; i < mContent.length(); i++) {
            char c = mContent.charAt(i);
            if(c != '.' && (c < '0' || c > '9')) {
                return false;
            }
        }
        return true;
    }

    public BigDecimal parseResult() {
        return parseResult(mContent.toString());
    }

    public BigDecimal collapseToResult() {
        BigDecimal result = parseResult(mContent.toString());
        buildStatesFromResult(result);
        return result;
    }

    private void onStatePoped(State state) {
        switch (state) {
            case STATE_DECIMAL:
                mDecimalDigitNum--;
                break;
        }
    }

    private void buildStatesFromResult(BigDecimal result) {
        mContent = new StringBuffer(result.toString());
        if(mContent.length() <= 0) {
            reset();
        } else {
            mStates.push(State.STATE_START);
            int length = mContent.length();
            int pointIndex = mContent.indexOf(".");
            if(pointIndex == -1) {
                pointIndex = length;
            }
            int i = 0;
            if(mContent.charAt(i) == '0') {
                mStates.push(State.STATE_LEAD_ZERO);
                i++;
            } else if(mContent.charAt(i) == '.') {
                mStates.push(State.STATE_LEAD_POINT);
                i++;
            }
            while(i < pointIndex) {
                mStates.push(State.STATE_INTEGER);
                i++;
            }
            if(pointIndex < length && i == pointIndex) {
                mStates.push(State.STATE_DECIMAL);
                i++;
            }
            while(i < length) {
                mStates.push(State.STATE_DECIMAL);
                i++;
            }
            mCurState = mStates.peek();
            mStates.pop();
        }
    }

    public static BigDecimal parseResult(String expr) {
        return parseResult(expr, 0);
    }

    private static BigDecimal parseResult(String expr, int start) {
        if(start >= expr.length()) return BigDecimal.ZERO;
        int e = start;
        while(e < expr.length() && expr.charAt(e) != '+') e++;
        return new BigDecimal(expr.substring(start, e))
                .add(parseResult(expr, e+1));
    }

    /**
     *
     * @param type
     * @return state after append a char with TYPE type
     */
    private State append(Type type) {
        if(type == Type.TYPE_INVALID) {
            return State.STATE_INVALID;
        }
        State result = State.STATE_INVALID;
        switch (mCurState) {
            case STATE_START:
                result = handleStateStart(type);
                break;
            case STATE_LEAD_ZERO:
                result = handleStateLeadingZero(type);
                break;
            case STATE_LEAD_POINT:
                result = handleStateLeadingPoint(type);
                break;
            case STATE_INTEGER:
                result = handleStateInteger(type);
                break;
            case STATE_DECIMAL:
                result = handleStateDecimal(type);
                break;
            case STATE_PLUS:
                result = handleStatePlus(type);
                break;
        }
        return result;
    }

    private State handleStatePlus(Type type) {
        switch (type) {
            case TYPE_DIGIT:
                return State.STATE_INTEGER;
            case TYPE_ZERO:
                return State.STATE_LEAD_ZERO;
            case TYPE_POINT:
                mDecimalDigitNum = 0;
                return State.STATE_LEAD_POINT;
        }
        return State.STATE_INVALID;
    }

    private State handleStateDecimal(Type type) {
        switch (type) {
            case TYPE_DIGIT:
            case TYPE_ZERO:
                if(mDecimalDigitNum < 2) {
                    mDecimalDigitNum++;
                    return State.STATE_DECIMAL;
                }
                break;
            case TYPE_PLUS:
                return State.STATE_PLUS;
        }
        return State.STATE_INVALID;
    }

    private State handleStateInteger(Type type) {
        switch (type) {
            case TYPE_DIGIT:
            case TYPE_ZERO:
                return State.STATE_INTEGER;
            case TYPE_POINT:
                mDecimalDigitNum = 0;
                return State.STATE_DECIMAL;
            case TYPE_PLUS:
                return State.STATE_PLUS;
        }
        return State.STATE_INVALID;
    }

    private State handleStateLeadingPoint(Type type) {
        switch (type) {
            case TYPE_DIGIT:
            case TYPE_ZERO:
                mDecimalDigitNum++;
                return State.STATE_DECIMAL;
        }
        return State.STATE_INVALID;
    }

    private State handleStateStart(Type type) {
        switch (type) {
            case TYPE_DIGIT:
                return State.STATE_INTEGER;
            case TYPE_ZERO:
                return State.STATE_LEAD_ZERO;
            case TYPE_POINT:
                mDecimalDigitNum = 0;
                return State.STATE_LEAD_POINT;
        }
        return State.STATE_INVALID;
    }

    private State handleStateLeadingZero(Type type) {
        switch (type) {
            case TYPE_DIGIT:
                mContent.setLength(0);
                mStates.pop();
                mCurState = State.STATE_START;
                return State.STATE_INTEGER;
            case TYPE_POINT:
                mDecimalDigitNum = 0;
                return State.STATE_DECIMAL;
        }
        return State.STATE_INVALID;
    }



    private static final Type getType(String s) {
        if(s == null || s.length() != 1) {
            return Type.TYPE_INVALID;
        }
        return getType(s.charAt(0));
    }

    private static final Type getType(char c) {
        switch (c) {
            case '0':
                return Type.TYPE_ZERO;
            case '1':case '2':case '3':case '4':case '5':
            case '6':case '7':case '8':case '9':
                return Type.TYPE_DIGIT;
            case '+':
                return Type.TYPE_PLUS;
            case '.':
                return Type.TYPE_POINT;
            default:
                return Type.TYPE_INVALID;
        }
    }
}
