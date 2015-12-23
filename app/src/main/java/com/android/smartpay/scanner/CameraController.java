package com.android.smartpay.scanner;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.android.smartpay.scanner.Utils.DisplayUtils;

import java.io.IOException;

/**
 * Created by xueqin on 2015/11/12 0012.
 */
public class CameraController {
    static final String TAG = "Camera Controller";
    Context mContext;
    Preview mPreview;
    Camera mCamera;
    int mNumOfCamera;
    int mDefaultCameraId;
    int mCameraCurrentLocked;
    Callback mCallback;
    boolean mHasSurface = false;

    public CameraController(Context context, Preview preview) {
        mContext = context;
        mPreview = preview;
        mPreview.setSurfaceCallback(mSurfaceHolderCallback);

        mNumOfCamera = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for(int i = 0; i < mNumOfCamera; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mDefaultCameraId = i;
            }
        }
    }

    public boolean openCamera() {
        return openCamera(mDefaultCameraId);
    }

    public boolean openCamera(final int id) {
        if (safeCameraOpen(id)) {
            Log.d(TAG, "camera open successfully");
            mPreview.setCamera(mCamera);
            return true;
        }
        else {
            Log.e(TAG, "Camera open failed");
            return false;
        }
    }

    public void closeCamera() {
        releaseCameraAndPreview();
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
            if(qOpened) {
                mCameraCurrentLocked = id;
            }
        } catch(Exception e) {
            Log.e(TAG, "Camera open failed");
            e.printStackTrace();
        }
        return qOpened;
    }

    private void releaseCameraAndPreview() {
        if(mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public void switchCamera(Camera camera) {
        if(camera != mCamera) {
            mPreview.setCamera(camera);
            try {
                mCamera.setPreviewDisplay(mPreview.getSurfaceHolder());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            setCameraParams();
            startPreviewSafe();
        }
    }

    private void setCameraParams() {
        Log.d(TAG, "set Camera Params running");
        Camera.Size previewSize = mPreview.getPreviewSize();
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewSize(previewSize.width, previewSize.height);
        // we need not taking picture, so settings of picture taking commented
//         set picture size the same as preview size
//        params.setPictureSize(previewSize.width, previewSize.height);
//        Log.d(TAG, "preview.width = " + previewSize.width + ", preview.height = " + previewSize.height);
//        params.setPictureFormat(ImageFormat.JPEG);
//         set auto focus, we don't need here, will use setautofucus manually latter
//		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);
        mPreview.requestLayout();

        if(DisplayUtils.getScreenOrientation(mContext) == Configuration.ORIENTATION_PORTRAIT) {
            mCamera.setDisplayOrientation(90);
        }
    }

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if(mCamera != null) {
                try {
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mHasSurface = true;
            Log.d(TAG, "surfaceChanged running" + ", width = " + width + ", height = " + height);
            if(mCamera == null) {
                Log.d(TAG, "mCamera is null currently");
                return;
            }
            if(mCallback != null) {
                mCallback.afterSurfaceChange();
            }
            setCameraParams();
            startPreviewSafe();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mHasSurface = false;
//            Log.d(TAG, "surfaceDestroyed running");
            if(mCamera != null) {
                stopPreview();
            }
        }
    };


    // interfaces

    public static interface Callback {
        void onCameraStartPreview();
        void onCameraStopPreview();
        void onAutoFocus(boolean success);
        void onPreviewFrame(byte[] data);
        void afterSurfaceChange();
    }


    private Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if(mCallback != null) {
                mCallback.onAutoFocus(success);
            }
        }
    };

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if(mCallback != null) {
                mCallback.onPreviewFrame(data);
            }
        }
    };


    public void registerCallback(Callback callback) {
        mCallback = callback;
    }

    public void setOneShotPreviewCallback() {
        mCamera.setOneShotPreviewCallback(mPreviewCallback);
    }

    public void clearOneShortPreviewCallback() {
        mCamera.setOneShotPreviewCallback(null);
    }

    public void setAutoFocus() {
        if(mCamera != null && mHasSurface) {
            mCamera.autoFocus(mAutoFocusCallback);
        }
    }

    public void cancelAutoFocus() {
        mCamera.cancelAutoFocus();
    }

    public void startPreviewSafe() {
        if(mHasSurface) {
            mCamera.startPreview();
            if (mCallback != null) {
                mCallback.onCameraStartPreview();
            }
        }
    }

    public void stopPreview() {
        mCamera.stopPreview();
        if(mCallback != null) {
            mCallback.onCameraStopPreview();
        }
    }

    public void takePicture(Camera.PictureCallback callback) {
        mCamera.takePicture(null, null, callback);
    }

    public Camera.Size getPreviewSize() {
        return mPreview.getPreviewSize();
    }
}
