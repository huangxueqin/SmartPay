package com.android.smartpay.http;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.android.smartpay.Application;
import com.android.smartpay.Preferences;
import com.android.smartpay.jsonbeans.ErrorResponse;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.TokenResponse;
import com.android.smartpay.utilities.HttpUtils;
import com.google.gson.Gson;

import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Created by xueqin on 2015/12/1 0001.
 */
public class HttpService {
    private static final boolean NO_NETWORK_DEBUG = Application.NO_NETWORK_DEBUG;
    private static final boolean TEST_DEBUG = Application.TEST_DEBUG;
    private static final boolean NORMAL_DEBUG = Application.NORMAL_DEBUG;

    private static final String TAG = "TAG----------->";

    public static final int ERROR_TOKEN = 0xFF1;
    public static final int ERROR_TIMEOUT = 0xFF2;

    public static final int CONNECTION_TIMEOUT = 4500;
    public static final int READ_TIMEOUT = 10000;

    public static final long EXPIRE_TIME_REDUNDANCY = 500;
    public static final String SIGN_METHOD = "2";
    public static final String PLATFORM = "101";
    public static final String SKEY = "101";
    public static final String PIN_CODE = "app_101";

    private static Handler sWorkHandler;
    private static HandlerThread sWorkThread;
    static {
        sWorkThread = new HandlerThread("worker");
        sWorkThread.start();
        sWorkHandler = new Handler(sWorkThread.getLooper());
    }

    private static HttpService sINSTANCE;
    private static Context sApplicationContext;
    private static byte[] buffer = new byte[1024];

    private long mLastAccessTokenTime = -1;
    private long mLastAccessTokenExpire = -1;
    private String mAccessToken;
    private String mRefreshToken;

    private Preferences mPreferences;
    private Handler mMainHandler;
    private Object mAccessTokenLock = new Object();
    private HashSet<AsyncTask> mRunningTasks;

    private HttpService() {
        if(sApplicationContext == null) {
            throw new RuntimeException("Application Context is not set");
        }
        mPreferences = new Preferences(sApplicationContext);
        mMainHandler = new Handler(sApplicationContext.getMainLooper());
        mRunningTasks = new HashSet<>();
    }

    public Handler getMainHandler() {
        return mMainHandler;
    }

    public static Handler getWorkHandler() {
        return sWorkHandler;
    }

    public void close() {

    }

    public static synchronized HttpService get() {
        if(sINSTANCE == null) {
            sINSTANCE = new HttpService();
        }
        return sINSTANCE;
    }

    public static void setApplicationContext(Context context) {
        sApplicationContext = context.getApplicationContext();
    }

    public boolean accessTokenValid() {
        if(NO_NETWORK_DEBUG) {
            return true;
        }
        long currentMillis = android.os.SystemClock.uptimeMillis();
        return mLastAccessTokenTime + mLastAccessTokenExpire - currentMillis >= EXPIRE_TIME_REDUNDANCY;
    }

    public void setAccessToken(String accessToken, String refreshToken, long expires) {
        synchronized (mAccessTokenLock) {
            setAccessTokenLocked(accessToken, refreshToken, expires);
        }
    }

    public void setAccessTokenLocked(String accessToken, String refreshToken, long expires) {
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mLastAccessTokenTime = android.os.SystemClock.uptimeMillis();
        mLastAccessTokenExpire = expires;
    }

    private String getAccessToken() {
        return mAccessToken;
    }

    // not a async method, should not call it in main thread
    public boolean updateAccessTokenIfNecessary() {
        if(accessTokenValid()) {
            return true;
        }
        synchronized (mAccessTokenLock) {
            if(!accessTokenValid()) {
                String username = mPreferences.getAccountName();
                String timestamp = HttpUtils.getTimeStamp();
                String platform = PLATFORM;
                String sign_method = SIGN_METHOD;
                String refresh_token = mRefreshToken;
                String signString  = SKEY + "platform" + platform +
                        "refresh_token" + refresh_token +
                        "sign_method" + sign_method +
                        "timestamp" + timestamp +
                        "username" + username + SKEY;
                String sign = HttpUtils.MD5Hash(signString);
                List<BasicNameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("platform", platform));
                params.add(new BasicNameValuePair("username", username));
                params.add(new BasicNameValuePair("refresh_token", refresh_token));
                params.add(new BasicNameValuePair("timestamp", timestamp));
                params.add(new BasicNameValuePair("sign_method", sign_method));
                params.add(new BasicNameValuePair("sign", sign));
                String urlStr = HttpUtils.buildUrlWithParams(HttpUtils.REFRESH_URL, params);
                if(TEST_DEBUG) {
                    L("token refresh url: " + urlStr);
                }
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    setPropertiesForGet(connection);
                    connection.setUseCaches(false);
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream is = connection.getInputStream();
                        String result = getResponseFromInputStream(is);
                        is.close();
                        if (NORMAL_DEBUG) {
                            L("result = " + result);
                        }
                        TokenResponse tokenResponse = new Gson().fromJson(result, TokenResponse.class);
                        if (tokenResponse != null && tokenResponse.errcode != null && tokenResponse.errcode.equals("0")) {
                            setAccessTokenLocked(tokenResponse.data.access_token,
                                    tokenResponse.data.refresh_token,
                                    tokenResponse.data.expires_in * 1000);
                            if(NORMAL_DEBUG) {
                                L("access_token = " + mAccessToken);
                                L("refresh_token = " + mRefreshToken);
                                L("expires_in = " + mLastAccessTokenExpire);
                                L(String.valueOf(mLastAccessTokenTime));
                            }
                            return true;
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if(connection != null) {
                        connection.disconnect();
                    }
                }
                return false;
            }
        }
        return accessTokenValid();
    }

    // not a async method, should not call it in main thread
    // if success, we write the user info into shared preferences directly here
    public void userLogin(String username, String password, final OnRequest<Void> callback) {
        String urlStr = HttpUtils.LOGIN_URL;
        // sign
        String platform = PLATFORM;
        String timestamp = HttpUtils.getTimeStamp();
        String sign_method = SIGN_METHOD;
        String signString = SKEY + "password" + password +
                "platform" + platform +
                "sign_method" + sign_method +
                "timestamp" + timestamp +
                "username" + username + SKEY;
        String sign = HttpUtils.MD5Hash(signString);
        // build url str
        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("sign", sign));
        params.add(new BasicNameValuePair("platform", platform));
        params.add(new BasicNameValuePair("username", username));
        params.add(new BasicNameValuePair("password", password));
        params.add(new BasicNameValuePair("timestamp", timestamp));
        params.add(new BasicNameValuePair("sign_method", sign_method));
        urlStr = HttpUtils.buildUrlWithParams(urlStr, params);
        if(NORMAL_DEBUG) {
            L("url is: " + urlStr);
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            setPropertiesForGet(connection);
            connection.setUseCaches(false);
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                String result = getResponseFromInputStream(is);
                is.close();
                if(NORMAL_DEBUG) {
                    L("result = " + result);
                }
                LoginResponse loginResponse = new Gson().fromJson(result, LoginResponse.class);
                if(loginResponse.errcode == null || !loginResponse.errcode.equals("0")) {
                    postError(callback, loginResponse.errcode, loginResponse.errmsg);
                }
                else {
                    setAccessToken(loginResponse.data.token.access_token,
                            loginResponse.data.token.refresh_token,
                            loginResponse.data.token.expires_in * 1000);
                    mPreferences.setLoginInfo(loginResponse);
                    postComplete(callback, (Void) null);
                }
                return;
            }
            else {
                if(NORMAL_DEBUG) {
                    L("response code = " + responseCode);
                    InputStream is = connection.getErrorStream();
                    L("error response = " + getResponseFromInputStream(is));
                    is.close();
                }
                else {
                    postError(callback, String.valueOf(responseCode), "net work error");
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
        postError(callback, String.valueOf(ERROR_TIMEOUT), "time out");
    }

    public <T> T executeJsonGetSync(String url, Class<T> clazz) {
        if(!updateAccessTokenIfNecessary()) {
            return null;
        }
        url += "&access_token=" + getAccessToken();
        if(NORMAL_DEBUG) {
            L("url = " + url);
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            setPropertiesForGet(connection);
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                String result = getResponseFromInputStream(is);
                if(NORMAL_DEBUG) {
                    L("result = " + result);
                }
                is.close();
                return (T) new Gson().fromJson(result, clazz);
            }
            else {
                if (NORMAL_DEBUG) {
                    L("response code = " + responseCode);
                    InputStream is = connection.getErrorStream();
                    L("error response = " + getResponseFromInputStream(is));
                    is.close();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String makePostUrl(String url, String params) {
        String timeStamp = HttpUtils.getTimeStamp();
        String sign = HttpUtils.MD5Hash(SKEY + params + timeStamp + SKEY);
        // build url
        List<BasicNameValuePair> urlParams = new ArrayList<>();
        urlParams.add(new BasicNameValuePair("access_token", getAccessToken()));
//        urlParams.add(new BasicNameValuePair("access_token", "7df79b725881fc745d4fc827bd8ca172"));
        urlParams.add(new BasicNameValuePair("timestamp", timeStamp));
        urlParams.add(new BasicNameValuePair("sign_method", SIGN_METHOD));
        urlParams.add(new BasicNameValuePair("sign", sign));
        return HttpUtils.buildUrlWithParams(url, urlParams);
    }

    // if connect timeout or network error return null
    // else return an instance of T, but this instance can also represent for error response
    // if the errcode of T is not "0"
    public <T> T executeJsonPostSync(String urlStr, String params, Class<T> clazz) {
        if(!updateAccessTokenIfNecessary()) {
            return null;
        }
        urlStr = makePostUrl(urlStr, params);
        HttpURLConnection connection = null;
        if(TEST_DEBUG) {
            L(urlStr);
            L(params);
        }
        try {
            connection = (HttpURLConnection) new URL(urlStr).openConnection();
            setPropertiesForPost(connection);
            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            dos.write(params.getBytes("UTF-8"));
            dos.flush();
            dos.close();
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                String result = getResponseFromInputStream(is);
                if(NORMAL_DEBUG) {
                    L(result);
                }
                is.close();
                return (T) new Gson().fromJson(result, clazz);
            } else {
                if (NORMAL_DEBUG) {
                    L("response code = " + responseCode);
                    InputStream is = connection.getErrorStream();
                    L("error response = " + getResponseFromInputStream(is));
                    is.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    public <T> void executeJsonGetAsync(String url, final OnRequest<T> callback, Class<T> clazz) {
        if(callback == null) {
            throw new RuntimeException("should provide a callback");
        }
        if(!isConnected()) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onNoConnection();
                }
            });
            return;
        }
        JsonPostGetTask<T> task = new JsonPostGetTask<T>(callback, clazz, RequestMethod.GET);
        mRunningTasks.add(task);
        task.execute(url);
    }

    public <T> void executeJsonPostAsync(String url, String jsonParams, final OnRequest<T> callback, Class<T> clazz) {
        if(callback == null) {
            throw new RuntimeException("should provide a callback");
        }
        if(!isConnected()) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onNoConnection();
                }
            });
            return ;
        }
        JsonPostGetTask<T> task = new JsonPostGetTask<T>(callback, clazz, RequestMethod.POST);
        mRunningTasks.add(task);
        task.execute(url, jsonParams);
    }

    // used for async http request, either GET or POST, decided by "requestMethod"
    // params[0] store url, if it is a POST request, then params[1] stores post params
    private class JsonPostGetTask<T> extends AsyncTask<String, Void, Void> {
//        private final String METHOD_NAME = "createInstance";
        private OnRequest<T> callback;
        private Class<T> clazz;
        private RequestMethod requestMethod;

        public JsonPostGetTask(OnRequest<T> callback, Class<T> clazz, RequestMethod requestMethod) {
            this.callback = callback;
            this.clazz = clazz;
            this.requestMethod = requestMethod;
        }

        @Override
        protected Void doInBackground(String... params) {
            if(!updateAccessTokenIfNecessary()) {
                postError(this, callback, String.valueOf(ERROR_TOKEN), "access token acquire failed");
                return null;
            }
            String url = params[0];
            HttpURLConnection connection = null;
            try {
                if (requestMethod == RequestMethod.GET) {
                    url += "&access_token=" + getAccessToken();
                    connection = (HttpURLConnection) new URL(url).openConnection();
                    setPropertiesForGet(connection);
                }
                else if(requestMethod == RequestMethod.POST) {
                    String postParams = params[1];
                    url = makePostUrl(url, postParams);
                    connection = (HttpURLConnection) new URL(url).openConnection();
                    setPropertiesForPost(connection);
                    DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                    dos.write(postParams.getBytes("UTF-8"));
                    dos.flush();
                    dos.close();
                }

                if(NORMAL_DEBUG) {
                    L("url = " + url);
                }

                int responseCode = connection.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = connection.getInputStream();
                    String result = getResponseFromInputStream(is);
                    is.close();
                    if(NORMAL_DEBUG) {
                        L("result = " + result);
                    }
                    ErrorResponse error = new Gson().fromJson(result, ErrorResponse.class);
                    if(error != null && error.errcode != null && error.errcode.equals("0")) {
                        T t = (T) new Gson().fromJson(result, clazz);
                        postComplete(this, callback, t);
                    }
                    else {
                        postError(this, callback, error.errcode, error.errmsg);
                    }
                }
                else {
                    if(NORMAL_DEBUG) {
                        L("response code = " + responseCode);
                        InputStream is = connection.getErrorStream();
                        L("error response = " + getResponseFromInputStream(is));
                        is.close();
                    }
                    postError(this, callback, String.valueOf(responseCode), "net work error");
                }
                return null;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(connection != null) {
                    connection.disconnect();
                }
            }
            postError(this, callback, String.valueOf(ERROR_TIMEOUT), "time out");
            return null;
        }
    }

    private <T> void postError(AsyncTask task, final OnRequest<T> callback, final String errcode, final String errmsg) {
        mRunningTasks.remove(task);
        if(!task.isCancelled()) {
            postError(callback, errcode, errmsg);
        }
    }

    private <T> void postComplete(AsyncTask task, final OnRequest<T> callback, final T t) {
        mRunningTasks.remove(task);
        if(!task.isCancelled()) {
            postComplete(callback, t);
        }
    }

    private <T> void postError(final OnRequest<T> callback, final String errcode, final String errmsg) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onFail(errcode, errmsg);
            }
        });
    }

    private <T> void postComplete(final OnRequest<T> callback, final T t) {
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onComplete(t);
            }
        });
    }

    public void cancelAllRunningTasks() {
        Iterator<AsyncTask> it = mRunningTasks.iterator();
        while(it.hasNext()) {
            AsyncTask task = it.next();
            task.cancel(true);
            it.remove();
        }
    }

    public static boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) sApplicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; ++i) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String getResponseFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int index = -1;
        while((index = is.read(buffer)) != -1){
            baos.write(buffer, 0, index);
        }
        String result = baos.toString("UTF-8");
        baos.close();
        return result;
    }

    private static void setPropertiesForGet(HttpURLConnection conn) throws ProtocolException {
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setUseCaches(true);
    }

    private static void setPropertiesForPost(HttpURLConnection conn) throws ProtocolException {
        conn.setConnectTimeout(CONNECTION_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(true);
    }

    private static void L(String msg) {
        Log.d(TAG, msg);
    }
}
