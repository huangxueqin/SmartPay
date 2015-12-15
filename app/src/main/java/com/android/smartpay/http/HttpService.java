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
import com.android.smartpay.http.decoder.JSONStreamDecoder;
import com.android.smartpay.http.decoder.StreamDecoder;
import com.android.smartpay.jsonbeans.LoginResponse;
import com.android.smartpay.jsonbeans.TokenResponse;
import com.android.smartpay.utilities.HttpUtils;
import com.android.smartpay.utilities.StorageUtils;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
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
    public static final int ERROR_FORMAT = 0xFF3;

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

    private long mLastAccessTokenTime = -1;
    private long mLastAccessTokenExpire = -1;
    private String mAccessToken;
    private String mRefreshToken;

    private Preferences mPreferences;
    private Handler mMainHandler;
    private Object mAccessTokenLock = new Object();
    private HashSet<AsyncTask> mRunningTasks;

    private JSONStreamDecoder mTokenDecoder;
    private JSONStreamDecoder mLoginDecoder;

    private HttpService() {
        if(sApplicationContext == null) {
            throw new RuntimeException("Application Context is not set");
        }
        mPreferences = new Preferences(sApplicationContext);
        mMainHandler = new Handler(sApplicationContext.getMainLooper());
        mRunningTasks = new HashSet<>();

        mTokenDecoder = new JSONStreamDecoder(TokenResponse.class);
        mLoginDecoder = new JSONStreamDecoder(LoginResponse.class);
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
                if(NORMAL_DEBUG) {
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
                        TokenResponse tokenResponse = (TokenResponse) mTokenDecoder.decode(is);
                        is.close();
                        if(tokenResponse != null) {
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
            D("login start");
            L("url is: " + urlStr);
            D("url is: " + urlStr);
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
                LoginResponse loginResponse = (LoginResponse) mLoginDecoder.decode(is);
                is.close();
                if(loginResponse != null) {
                    setAccessToken(loginResponse.data.token.access_token,
                            loginResponse.data.token.refresh_token,
                            loginResponse.data.token.expires_in * 1000);
                    mPreferences.setLoginInfo(loginResponse);
                    postComplete(callback, (Void) null);
                }
                else {
                    postError(callback, mLoginDecoder.getErrCode(), mLoginDecoder.getErrMsg());
                }
            }
            else {
                if(NORMAL_DEBUG) {
                    L("response code = " + responseCode);
                    D("response code = " + responseCode);
                    InputStream is = connection.getErrorStream();
                    L("error response = " + mLoginDecoder.decodeStringFromInputStream(is));
                    D("error response = " + mLoginDecoder.decodeStringFromInputStream(is));
                    is.close();
                }
                postError(callback, String.valueOf(responseCode), "net work error");
            }
            return;
        } catch (MalformedURLException e) {
            D("MalformedURLException: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            D("IOException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
        postError(callback, String.valueOf(ERROR_TIMEOUT), "time out");
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

    /**
     * execute the http request synchronously
     * @param decoder decoder input stream from http response to acquire specific object
     * @param method request method one of {@link RequestMethod}
     * @param params params[0] is url, if using post, then params[1] is the post entity
     * @return
     */
    private Object executeHttpRequestSync(StreamDecoder decoder, RequestMethod method, String... params) {
        if(!updateAccessTokenIfNecessary()) {
            decoder.setErrCode(String.valueOf(ERROR_TOKEN));
            decoder.setErrMsg("update access token failed");
            return null;
        }
        String url = params[0];
        if(method == RequestMethod.GET) {
            url += "&access_token=" + getAccessToken();
        } else if(method == RequestMethod.POST) {
            url = makePostUrl(url, params[1]);
        }

        if(NORMAL_DEBUG) {
            L("url = " + url);
        }

        Object o = null;
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            if(method == RequestMethod.GET) {
                setPropertiesForGet(connection);
            } else if(method == RequestMethod.POST) {
                setPropertiesForPost(connection);
                String postParams = params[1];
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                dos.write(postParams.getBytes("UTF-8"));
                dos.flush();
                dos.close();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                o = decoder.decode(is);
                is.close();
            }
            else {
                if (NORMAL_DEBUG) {
                    L("response code = " + responseCode);
                    InputStream is = connection.getErrorStream();
                    L("error response = " + decoder.decodeStringFromInputStream(is));
                    is.close();
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
        return o;
    }

    public Object executeHttpPostSync(String url, String params, StreamDecoder decoder) {
        return executeHttpRequestSync(decoder, RequestMethod.POST, url, params);
    }

    public Object executeHttpGetSync(String url, StreamDecoder decoder) {
        return executeHttpRequestSync(decoder, RequestMethod.GET, url);
    }

    public <T> T executeJsonGetSync(String url, Class<T> clazz) {
        return (T) executeHttpGetSync(url, new JSONStreamDecoder(clazz));
    }

    public <T> T executeJsonPostSync(String url, String params, Class<T> clazz) {
        return (T) executeHttpPostSync(url, params, new JSONStreamDecoder(clazz));
    }

    public <T> void executeHttpGetAsync(String url, StreamDecoder decoder, final OnRequest<T> callback) {
        if(callback == null) {
            throw new RuntimeException("should provide a callback");
        }
        HttpRequestTask<T> httpTask = new HttpRequestTask<>(callback, RequestMethod.GET, decoder);
        mRunningTasks.add(httpTask);
        httpTask.execute(url);
    }

    public <T> void executeHttpPostAsync(String url, String params, StreamDecoder decoder, final OnRequest<T> callback) {
        if(callback == null) {
            throw new RuntimeException("should provide a callback");
        }
        HttpRequestTask<T> httpTask = new HttpRequestTask<T>(callback, RequestMethod.POST, decoder);
        mRunningTasks.add(httpTask);
        httpTask.execute(url, params);
    }

    public <T> void executeJsonGetAsync(String url, Class<T> clazz, final OnRequest<T> callback) {
        executeHttpGetAsync(url, new JSONStreamDecoder(clazz), callback);
    }

    public <T> void executeJsonPostAsync(String url, String params, Class<T> clazz, final OnRequest<T> callback) {
        executeHttpPostAsync(url, params, new JSONStreamDecoder(clazz), callback);
    }

    /**
     * AsyncTask to execute http request asynchronously
     * @param <T>
     */
    private class HttpRequestTask<T> extends AsyncTask<String, Void, Void> {
        private WeakReference<OnRequest<T>> callback;
        private RequestMethod requestMethod;
        private StreamDecoder decoder;

        public HttpRequestTask(OnRequest<T> callback, RequestMethod requestMethod, StreamDecoder decoder) {
            this.callback = new WeakReference<OnRequest<T>>(callback);
            this.requestMethod = requestMethod;
            this.decoder = decoder;
        }

        @Override
        protected Void doInBackground(String... params) {
            if(!isConnected()) {
                // remove self from running tasks collection
                mRunningTasks.remove(this);
                if(callback.get() != null) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.get().onNoConnection();
                        }
                    });
                }
                return null;
            }

            T t = (T) executeHttpRequestSync(decoder, requestMethod, params);
            if(!isCancelled()) {
                if (t != null) {
                    postComplete(this, callback.get(), t);
                } else if (decoder.getErrCode() != null) {
                    postError(this, callback.get(), decoder.getErrCode(), decoder.getErrMsg());
                } else {
                    // timeout happens
                    postError(this, callback.get(), String.valueOf(ERROR_TIMEOUT), "time out");
                }
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }

    private <T> void postError(AsyncTask task, final OnRequest<T> callback, final String errcode, final String errmsg) {
        mRunningTasks.remove(task);
        if(!task.isCancelled() && callback != null) {
            postError(callback, errcode, errmsg);
        }
    }

    private <T> void postComplete(AsyncTask task, final OnRequest<T> callback, final T t) {
        mRunningTasks.remove(task);
        if(!task.isCancelled() && callback != null) {
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
    private static void D(String msg) {
        StorageUtils.dumpLog(msg);
    }
}
