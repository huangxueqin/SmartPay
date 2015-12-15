package com.android.smartpay.http.decoder;

import com.android.smartpay.Application;
import com.android.smartpay.http.HttpService;
import com.android.smartpay.jsonbeans.ErrorResponse;
import com.android.smartpay.jsonbeans.ParseBeanException;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by xueqin on 2015/12/15 0015.
 */
public class JSONStreamDecoder extends StreamDecoder {
    public static String METHOD_NAME_NEW_INSTANCE = "obtainInstance";

    private Class<?> clazz;
    private String errtype;
    private String errmsg;
    private String errcode;

    public JSONStreamDecoder(Class<?> clazz) {
        this.clazz = clazz;
    }

    public void setClass(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public String getErrMsg() {
        return errmsg;
    }

    @Override
    public String getErrCode() {
        return errcode;
    }

    @Override
    public void setErrCode(String errCode) {
        errcode = errCode;
    }

    @Override
    public void setErrMsg(String errMsg) {
        errmsg = errMsg;
    }

    /**
     * decode object from an input stream
     * @param is
     * @return null if decode fail, {@link JSONStreamDecoder#errcode} and {@link JSONStreamDecoder#errmsg}
     * may record the reason, else an object with type {@link JSONStreamDecoder#clazz} returned
     */
    @Override
    public Object decode(InputStream is) {
        String result = null;
        try {
            result = decodeStringFromInputStream(is);
            if(Application.NORMAL_DEBUG) {
                L(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(result != null) {
            return Application.GSON_ENABLE ? decodeObjectWithGson(result) : decodeObjectWithReflection(result);
        }
        return null;
    }

    private Object decodeObjectWithGson(String json) {
        errcode = null;
        errmsg = null;
        ErrorResponse error;
        try {
            error = new Gson().fromJson(json, ErrorResponse.class);
            errcode = error.errcode;
            errmsg = error.errmsg;
            if(errcode != null && errcode.equals("0")) {
                return new Gson().fromJson(json, clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        errcode = String.valueOf(HttpService.ERROR_FORMAT);
        errmsg = "invalid json format";
        return null;
    }

    /**
     * decode json string using reflection
     * The object we want to obtain should has a static method named with {@link JSONStreamDecoder#METHOD_NAME_NEW_INSTANCE}
     * accepts a json string as the only parameter and returns an instance of that type of the object
     * and throws a {@link ParseBeanException} exception
     * @param json
     * @return
     */
    private Object decodeObjectWithReflection(String json) {
        errcode = null;
        errmsg = null;
        try {
            Method method = clazz.getMethod(METHOD_NAME_NEW_INSTANCE, new Class<?>[] {String.class});
            Object o = method.invoke(null, json);
            Field ferrcode = clazz.getField("errcode");
            Field ferrmsg = clazz.getField("errmsg");
            errcode = (String) ferrcode.get(o);
            errmsg = (String) ferrmsg.get(o);
            if(errcode != null && errcode.equals("0")) {
                return o;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        errcode = String.valueOf(HttpService.ERROR_FORMAT);
        errmsg = "invalid json format";
        return null;
    }
}
