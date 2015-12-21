package com.android.smartpay.scanner;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;


import com.android.smartpay.scanner.Utils.DisplayUtils;

import java.util.List;

/**
 * Created by xueqin on 2015/11/12 0012.
 */
public class Preview extends ViewGroup {
    final private static boolean DEBUG = false;
    final private static String TAG = "TAG---------------->";

    private SurfaceView mSurfaceView;
    private SurfaceHolder mHolder;
    private View mMaskView;

    private Camera mCamera;
    private List<Camera.Size> mSupportedPreviewSizes;
    private Camera.Size mPreviewSize;
    private float mScale;

    public Preview(Context context) {
        this(context, null);
    }

    public Preview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Preview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mSurfaceView = new SurfaceView(context);
        addView(mSurfaceView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        mHolder = mSurfaceView.getHolder();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setMaskView(View mask) {
        mMaskView = mask;
        addView(mMaskView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setSurfaceCallback(SurfaceHolder.Callback callback) {
        mHolder.addCallback(callback);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }



    public void setCamera(Camera camera) {
//        Log.d(TAG, "set Camera running");
        if(mCamera != camera) {
            stopPreviewAndReleaseCamera();
            mCamera = camera;

            if(mCamera != null) {
                mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            }
        }
    }

    public SurfaceHolder getSurfaceHolder() {
        return mHolder;
    }

    public Camera.Size getPreviewSize() {
        if(mPreviewSize == null) {
            throw new RuntimeException("Preview Size has not initialized");
        }
        return mPreviewSize;
    }

    public float getDisplayToPreviewScaleRatio() {
        return mScale;
    }

    private void stopPreviewAndReleaseCamera() {
        if(mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(changed && getChildCount() > 0) {
            int width = r - l;
            int height = b - t;
            for(int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                child.layout((width - childWidth) / 2, (height - childHeight) / 2,
                        (width + childWidth) / 2, (height + childHeight) / 2);
            }
        }
        if(DEBUG) {
            Log.d(TAG, "after layout child width = " + mSurfaceView.getWidth() + ", height = " + mSurfaceView.getHeight());
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Log.d(TAG, "onMeasure running");
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if(mSupportedPreviewSizes != null) {
            mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width, height);
//            Log.d(TAG, "width = " + width + ", height = " + height);
//            Log.d(TAG, "preview.width = " + mPreviewSize.width + ", preview.height = " + mPreviewSize.height);
            int orientation = DisplayUtils.getScreenOrientation(getContext());
            int previewWidth = width;
            int previewHeight = height;
            if (mPreviewSize != null) {
                previewWidth =
                        orientation == Configuration.ORIENTATION_LANDSCAPE ? mPreviewSize.width : mPreviewSize.height;
                previewHeight =
                        orientation == Configuration.ORIENTATION_LANDSCAPE ? mPreviewSize.height : mPreviewSize.width;
            }
            int childWidth = width;
            int childHeight = height;
            if (DisplayUtils.getScreenOrientation(getContext()) == Configuration.ORIENTATION_PORTRAIT) {
                mScale = (float) width / previewWidth;
                childHeight = (int) (mScale * previewHeight);
            } else {
                mScale = (float) height / previewHeight;
                childWidth = (int) (mScale * previewWidth);
            }
            for (int i = 0; i < getChildCount(); i++) {
                getChildAt(i).measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY));
//                Log.d(TAG, "child " + i + " measureWidth = " + getChildAt(i).getMeasuredWidth() + ", measureHeight = " + getChildAt(i).getMeasuredHeight());
            }
            setMeasuredDimension(width, height);
        }
        else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> supportedSizes, int w, int h) {
//        Log.d(TAG, "get Optimal Preview Size running");
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double)w / h;
        if(supportedSizes == null) {
            return null;
        }

        if(DEBUG) {
            Log.d(TAG, "supportedSizes:");
            for (Camera.Size sz : supportedSizes) {
                Log.d(TAG, "" + sz.width + ", " + sz.height);
            }
        }

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;
        int screenOrientation = DisplayUtils.getScreenOrientation(getContext());
        for(Camera.Size sz : supportedSizes) {
            double supportWidth = screenOrientation == Configuration.ORIENTATION_LANDSCAPE ? sz.width : sz.height;
            double supportHeight = screenOrientation == Configuration.ORIENTATION_LANDSCAPE ? sz.height : sz.width;
            double ratio = (double) supportWidth / supportHeight;
            if(Math.abs(targetRatio - ratio) > ASPECT_TOLERANCE) continue;
            if(Math.abs(supportHeight - targetHeight) < minDiff) {
                optimalSize = sz;
                minDiff = Math.abs(supportHeight - targetHeight);
            }
        }

        if(optimalSize == null) {
            for(Camera.Size sz : supportedSizes) {
                double supportHeight = screenOrientation == Configuration.ORIENTATION_LANDSCAPE ? sz.height : sz.width;
                if(Math.abs(supportHeight - targetHeight) < minDiff) {
                    optimalSize = sz;
                    minDiff = Math.abs(supportHeight - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}
