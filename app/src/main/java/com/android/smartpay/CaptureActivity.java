package com.android.smartpay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.smartpay.scanner.CameraController;
import com.android.smartpay.scanner.GrayLuminanceSource;
import com.android.smartpay.scanner.Preview;
import com.android.smartpay.scanner.Utils.DisplayUtils;
import com.android.smartpay.scanner.ViewFinderView;
import com.android.smartpay.utilities.Cons;
import com.android.smartpay.utilities.Permission;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

public class CaptureActivity extends AppCompatActivity {
    private static final String TAG = "TAG---------->";
    private static final boolean DEBUG = Application.NO_NETWORK_DEBUG;
    public static final String SCAN_OK = "com.seuic.framework.scan_ok";

    private TextView mTitle;
    private Preview mPreview;
    private ViewFinderView mMaskView;
    private Button mButton;

    private CameraController mCameraController;
    private Camera.Size mPreviewSize;
    private Rect mCropRect = new Rect();
    private QRCodeReader mReader;
    private byte[] data;


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
        mCameraController.openCamera();
        mReader = new QRCodeReader();
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
                mAction = Cons.ACTION_QQ_PAY;
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
        mCameraController.startPreviewSafe();
    }

    @Override
	protected void onPause() {
        mCameraController.stopPreview();
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
            if(b == null) {
//                Toast.makeText(CaptureActivity.this, "Nothing found", Toast.LENGTH_SHORT).show();
                mCameraController.setAutoFocus();
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
            int dataWidth = mCropRect.width();
            int dataHeight = mCropRect.height();
            if(data == null || data.length < dataWidth * dataHeight) {
                data = new byte[dataWidth * dataHeight];
            }

            int left = mCropRect.left;
            int top = mCropRect.top;
            int sourceOffset = sourceWidth * top + left;
            int dataOffset = 0;
            for(int i = 0; i < dataHeight; i++) {
                System.arraycopy(source, sourceOffset, data, dataOffset, dataWidth);
                sourceOffset += sourceWidth;
                dataOffset += dataWidth;
            }
            // for testing
//            Bitmap b = Bitmap.createBitmap(dataWidth, dataHeight, Bitmap.Config.ARGB_8888);
//            for(int i = 0; i < dataWidth; i++) {
//                for(int j = 0; j < dataHeight; j++) {
//                    byte gray = data[dataWidth*j + i];
//                    b.setPixel(i, j, (0xff000000 | gray << 16 | gray << 8 | gray));
//                }
//            }
//            return b;

            GrayLuminanceSource luminanceSource = new GrayLuminanceSource(dataWidth, dataHeight, data);
            HybridBinarizer binarizer = new HybridBinarizer(luminanceSource);
            try {
                result = mReader.decode(new BinaryBitmap(binarizer)).getText();
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (ChecksumException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
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
//				bundle.putString(FragmentListener.KEY_SCAN_RESULT, text);
				resultIntent.putExtras(bundle);
				CaptureActivity.this.setResult(RESULT_OK, resultIntent);
				CaptureActivity.this.finish();
			}
		}
	};
}