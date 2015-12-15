package com.android.smartpay.http.decoder;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by xueqin on 2015/12/15 0015.
 */
public abstract class StreamDecoder {
    protected byte[] buffer = new byte[1024];

    // if success return object, or return null
    public abstract Object decode(InputStream is) ;

    public String decodeStringFromInputStream(InputStream is, String charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int index = -1;
        while((index = is.read(buffer)) != -1){
            baos.write(buffer, 0, index);
        }
        String result = baos.toString(charset);
        baos.close();
        return result;
    }

    public String decodeStringFromInputStream(InputStream is) throws IOException {
        return decodeStringFromInputStream(is, "UTF-8");
    }

    public String getErrCode() {
        return null;
    }

    public String getErrMsg() {
        return null;
    }

    public void setErrCode(String errCode) {

    }

    public void setErrMsg(String errMsg) {

    }

    protected static void L(String msg) {
        Log.d("decode json----->", msg);
    }
}
