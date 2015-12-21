package com.android.smartpay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.smartpay.scanner.CameraController;
import com.android.smartpay.scanner.Preview;
import com.android.smartpay.scanner.Utils.DisplayUtils;
import com.android.smartpay.scanner.ViewFinderView;
import com.android.smartpay.utilities.Cons;
import com.android.smartpay.utilities.Permission;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CaptureActivity extends AppCompatActivity {
    private static final String TAG = "TAG---------->";
    private static final boolean DEBUG = Application.NO_NETWORK_DEBUG;
    public static final String SCAN_OK = "com.seuic.framework.scan_ok";
    public static final long SCAN_DELAY = 2000;

    private static Set<BarcodeFormat> PRODUCT_FORMATS;
    private static Set<BarcodeFormat> INDUSTRIAL_FORMATS;
    static {
        PRODUCT_FORMATS = EnumSet.of(BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED);
        INDUSTRIAL_FORMATS = EnumSet.of(BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR);
    }

    private TextView mTitle;
    private Preview mPreview;
    private ViewFinderView mMaskView;
    private Button mButton;

    private CameraController mCameraController;
    private Camera.Size mPreviewSize;
    private boolean mCameraOpenSuccess;
    private Rect mCropRect = new Rect();
    private MultiFormatReader mMultiFormatReader;
    private byte[] data;
    private int[] rgbArray;
    private boolean cancelDecode = false;


    private Handler mHandler;
    private int mAction;
    private float mMoney;
    private int mPermission;


	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_capture);
        registerReceiver(mScanReceiver, new IntentFilter(SCAN_OK));
        setupToolbar();
        mHandler = new Handler(getMainLooper());
        mMoney = getIntent().getFloatExtra(Cons.ARG_MONEY, -1);
        if(mMoney <= 0) {
            throw new RuntimeException("Money must bigger than 0");
        }
        mAction = getIntent().getIntExtra(Cons.ARG_ACTION, Cons.ACTION_WECHAT_PAY);
        mPermission = getIntent().getIntExtra(Cons.ARG_PERM, 0);

        mPreview = (Preview) findViewById(R.id.preview);
        mMaskView = (ViewFinderView) findViewById(R.id.view_finder_view);
        mMaskView.setMoney(String.format("%.2f", mMoney));
        mButton = (Button) findViewById(R.id.switch_paymode);
        mButton.setOnClickListener(mButtonOnClickListener);
        setupViewByAction();

        mCameraController = new CameraController(this, mPreview);
        mCameraController.registerCallback(mCameraControllerCallback);
        mCameraOpenSuccess = mCameraController.openCamera();

        // init barcode reader
        mMultiFormatReader = new MultiFormatReader();
        Map<DecodeHintType,Object> hints = new EnumMap<DecodeHintType,Object>(DecodeHintType.class);
        // Add specific formats
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);
        formats.addAll(PRODUCT_FORMATS);
        formats.addAll(INDUSTRIAL_FORMATS);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        mMultiFormatReader.setHints(hints);
	}

    private void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if(toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            mTitle = (TextView) findViewById(R.id.appbar_title);
        }
    }

    private void T(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener mButtonOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if((mAction == Cons.ACTION_QQ_PAY && !Permission.hasPermWechatPay(mPermission)) ||
                    mAction == Cons.ACTION_WECHAT_PAY && !Permission.hasPermQQPay(mPermission)) {
                T("没有权限" + (mAction == Cons.ACTION_QQ_PAY ? "微信支付" : "QQ支付"));
                return ;
            }
            if(mAction == Cons.ACTION_QQ_PAY) {
                mAction = Cons.ACTION_WECHAT_PAY;
            }
            else if(mAction == Cons.ACTION_WECHAT_PAY) {
                T("QQ钱包暂时不可用");
                return;
//                mAction = Cons.ACTION_QQ_PAY;
            }
            setupViewByAction();
        }
    };

    private void setupViewByAction() {
        if(mAction == Cons.ACTION_QQ_PAY) {
            mTitle.setText(R.string.ca_title_qq);
            mButton.setText(R.string.ca_bt_wechat);
        }
        else if(mAction == Cons.ACTION_WECHAT_PAY) {
            mTitle.setText(R.string.ca_title_wechat);
            mButton.setText(R.string.ca_bt_qq);
        }
    }

    private void setResultAndFinish() {
        setResultAndFinish(false, null);
    }

    private void setResultAndFinish(boolean ok, String scanResult) {
        if(ok) {
            Intent data = new Intent();
            data.putExtra(Cons.ARG_ACTION, mAction);
            data.putExtra(Cons.ARG_SCAN_RESULT, scanResult);
            int payType = mAction == Cons.ACTION_QQ_PAY ? 8 : 2; // 2 for wechat pay, 8 for qq pay
            data.putExtra(Cons.ARG_PAY_TYPE, payType);
            data.putExtra(Cons.ARG_MONEY, mMoney);
            setResult(RESULT_OK, data);
        }
        else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancelDecode = true;
        setResultAndFinish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                setResultAndFinish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mCameraOpenSuccess) {
            mCameraController.startPreviewSafe();
        }
    }

    @Override
	protected void onPause() {
        if(mCameraOpenSuccess) {
            mCameraController.stopPreview();
        }
		super.onPause();
	}

	@Override
	protected void onDestroy() {
        unregisterReceiver(mScanReceiver);
		mCameraController.closeCamera();
		super.onDestroy();
	}

    private CameraController.Callback mCameraControllerCallback = new CameraController.Callback() {
        @Override
        public void onCameraStartPreview() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mMaskView.startScanAnimation();
                    // when call this directly, auto Focus seems not work, don't know why
                    mCameraController.setAutoFocus();
                }
            });
        }

        @Override
        public void onCameraStopPreview() {
            mCameraController.cancelAutoFocus();
            mCameraController.clearOneShortPreviewCallback();
            mMaskView.stopScanAnimation();
        }

        @Override
        public void onAutoFocus(boolean success) {
            if(success) {
                mCameraController.setOneShotPreviewCallback();
            }
            else {
                mCameraController.setAutoFocus();
            }
        }

        @Override
        public void onPreviewFrame(byte[] source) {
            new DecodeTask().execute(source);
        }

        @Override
        public void afterSurfaceChange() {
            initDataAfterPreviewStarted();
        }

        private void initDataAfterPreviewStarted() {
            mPreviewSize = mCameraController.getPreviewSize();
            float scale = mPreview.getDisplayToPreviewScaleRatio();
            Rect windowRect = mMaskView.getWindowRect();
            int a = (int) (windowRect.left / scale - 0.5);
            int b = (int) (windowRect.top / scale - 0.5);
            int c = (int) (windowRect.right / scale + 0.5);
            int d = (int) (windowRect.bottom / scale + 0.5);
            if(DisplayUtils.getScreenOrientation(CaptureActivity.this) == Configuration.ORIENTATION_PORTRAIT) {
                mCropRect.set(b, mPreviewSize.height - c, d, mPreviewSize.height - a);
            }
            else {
                mCropRect.set(a, b, c, d);
            }
        }
    };

    private class DecodeTask extends AsyncTask<byte[], Void, String> {
        @Override
        protected void onPostExecute(String b) {
            super.onPostExecute(b);
            if(cancelDecode) {
                return;
            }
            if(b == null) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mCameraController != null) {
                            mCameraController.setAutoFocus();
                        }
                    }
                }, SCAN_DELAY);
                return;
            }
            setResultAndFinish(true, b);
        }

        @Override
        protected String doInBackground(byte[]... params) {
            // we only need luminance part of NV21
            String result = null;
            byte[] source = params[0];
            int sourceWidth = mPreviewSize.width;
            int sourceHeight = mPreviewSize.height;
            Bitmap b = YUV420spToBitmap(source, sourceWidth, sourceHeight, mCropRect);
            int dataWidth = mCropRect.width();
            int dataHeight = mCropRect.height();
            if(DisplayUtils.getScreenOrientation(CaptureActivity.this) == Configuration.ORIENTATION_PORTRAIT) {
                // rotate 270
                Matrix matrix = new Matrix();
                matrix.postRotate(270);
                b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                dataHeight = mCropRect.width();
                dataWidth = mCropRect.height();
            }
            if(cancelDecode) {
                return null;
            }
            if(rgbArray == null) {
                rgbArray = new int[dataWidth * dataHeight];
            }
            b.getPixels(rgbArray, 0, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
            LuminanceSource luminance = new RGBLuminanceSource(dataWidth, dataHeight, rgbArray);
            BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(luminance));
            try {
                result = mMultiFormatReader.decodeWithState(bb).getText();
            } catch (NotFoundException e) {
                e.printStackTrace();
            } finally {
                mMultiFormatReader.reset();
            }
            return result;
        }
    }

	private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (SCAN_OK.equals(intent.getAction())) {
				String text = intent.getStringExtra("scan_data");
				Intent resultIntent = new Intent();
				Bundle bundle = new Bundle();
				resultIntent.putExtras(bundle);
				CaptureActivity.this.setResult(RESULT_OK, resultIntent);
				CaptureActivity.this.finish();
			}
		}
	};

    private static Bitmap YUV420spToBitmap(byte[] yuv, int width, int height, Rect cropRect) {
        YuvImage yuvImage = new YuvImage(yuv, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if(cropRect == null) {
            cropRect = new Rect(0, 0, width, height);
        }
        yuvImage.compressToJpeg(cropRect, 80, baos);
        byte[] data = baos.toByteArray();
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
        return b;
    }

    private static void L(String msg) {
        Log.d(TAG, msg);
    }
}