package com.ssdy.education.mobile.utils;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.google.gson.Gson;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 主要对相机预览进行处理
 */
public class CameraManager {

    private static final boolean DEBUG = false; // TODO set false on release
    private static final String TAG = "CameraGLView";
    private static CameraManager mCameraManager;
    //主要进行预览的主要是 480 *640
    private static final int MIN_HEIGHT =480;
    private static final int CAMERA_ID = 0;
    public static Camera mCamera;
    //判断是否是前置摄像头
    private  static boolean mIsFrontFace;
    public static synchronized CameraManager getInstance() {
        if (mCameraManager == null) {
            mCameraManager = new CameraManager();
        }
        return mCameraManager;
    }

    /**
     * 根据设置的相机尺寸进行预览
     * @param width
     * @param height
     */
    public Camera init(Activity activity, final int width, final int height, OnCameraListener mListener) {
        if (DEBUG) Log.v(TAG, "startPreview:");
        if (mCamera == null) {
            // This is a sample project so just use 0 as camera ID.
            // it is better to selecting camera is available

            try {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                LogUtil.d("CAMERA_FACING_BACK   :"+ Camera.CameraInfo.CAMERA_FACING_BACK);
                final Camera.Parameters params = mCamera.getParameters();
                final List<String> focusModes = params.getSupportedFocusModes();
                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                } else {
                    if (DEBUG) Log.i(TAG, "Camera does not support autofocus");
                }
                // let's try fastest frame rate. You will getYiJing near 60fps, but your device become hot.
                final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();
//					final int n = supportedFpsRange != null ? supportedFpsRange.size() : 0;
//					int[] range;
//					for (int i = 0; i < n; i++) {
//						range = supportedFpsRange.getYiJing(i);
//						Log.i(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
//					}
                final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
                Log.i(TAG, String.format("fps:%d-%d", max_fps[0], max_fps[1]));
                params.setPreviewFpsRange(max_fps[0], max_fps[1]);
                params.setRecordingHint(true);
//                // request closest supported preview size
                //预览尺寸和图片尺寸比例必须相同。
                Camera.Size  previewSize = getSupportedSize(params.getSupportedPreviewSizes(),width, height,720);
                Camera.Size  pictureSize = getSupportedSize(params.getSupportedPictureSizes(),previewSize.width, previewSize.height,720);
                params.setPreviewSize(previewSize.width, previewSize.height);
                params.setPictureSize(pictureSize.width, pictureSize.height);
                params.setJpegQuality(70);
                // rotate camera preview according to the device orientation
                setRotation(activity,params,mListener);
                mCamera.setParameters(params);
                // adjust view size with keeping the aspect ration of camera preview.
                // here is not a UI thread and we should request parent view to execute.
//                final SurfaceTexture st = parent.getSurfaceTexture();
//                st.setDefaultBufferSize(previewSize.width, previewSize.height);
//                mCamera.setPreviewTexture(st);
                //encoder
                if(mListener!=null){
                    mListener.OnCameraSize(pictureSize);
                }
            }catch (final Exception e) {
                Log.e(TAG, "startPreview:", e);
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
                if(mListener!=null){
                    mListener.OnPermisson(false);
                }
            }
        }

        return  mCamera;
    }

    /**
     * 设置预览hoder
     * @param holder
     */
    public void setPreview(SurfaceHolder holder){
        try {
            if (mCamera != null) {
                // start camera preview display
                mCamera.setPreviewDisplay(holder);
            }
        }catch (Exception e){
            LogUtil.d("setPreview",e);
        }
    }

    /**
     * 开始预览
     */
    public void start(){
        try {
            if(mCamera!=null){
                mCamera.startPreview();
            }
        }catch ( Exception e){
           LogUtil.e(getClass() +"start  ",e);
        }
    }

    /**
     * 停止预览界面静止
     */
    public void stop(){
        try {
            mCamera.stopPreview();
        }catch ( Exception e){
            LogUtil.e(getClass() +"stopPreview  ",e);
        }
    }


    /**
     *图片拍照
     */
    public void  doTakePicture(Camera.PictureCallback pictureCallback){
        if((mCamera != null)){
            mCamera.takePicture(null, null, pictureCallback);
        }
    }

    /**
     * 计算出两组list中相同的数值，并且将相同的数值进行比较
     * @param requestedWidth
     * @param requestedHeight
     * @return
     */
    private static  Camera.Size getSupportedSize(List<Camera.Size> supportedSize, final int requestedWidth, final int requestedHeight,int minHeight) {
        //从小到大排序
        Collections.sort(supportedSize, new Comparator<Camera.Size>() {
            @Override
            public int compare(final Camera.Size lhs, final Camera.Size rhs) {
                return  new Integer(lhs.height).compareTo(rhs.height);
            }
        });
        //将最接近的尺寸进行匹配
        Collections.sort(supportedSize, new Comparator<Camera.Size>() {

            private float diff(final Camera.Size size) {
                float number = Math.abs(requestedHeight/((float)requestedWidth) - size.height/((float)size.width));
                return number;
            }
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return new Float(diff(lhs)).compareTo(diff(rhs));
            }

        });
        Log.i(TAG, "getSupportedSize  : "+new Gson().toJson(supportedSize));
        Camera.Size size =supportedSize.get(0) ;
        for(int i= 0;i<supportedSize.size();i++){
            if(supportedSize.get(i).height>=minHeight){
                size =supportedSize.get(i);
                break;
            }
        }
        Log.i(TAG, "SupportedSize  : "+new Gson().toJson(size));
        return size;
    }

    /**
     * rotate preview screen according to the device orientation
     * @param params
     */
    public   int setRotation(Activity activity, final Camera.Parameters params, OnCameraListener mListener) {
        int degrees = 0;
        try {
            if (DEBUG) Log.v(TAG, "setRotation:");
            final Display display = ((WindowManager)activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            final int rotation = display.getRotation();
            switch (rotation) {
                case Surface.ROTATION_0: degrees = 0; break;
                case Surface.ROTATION_90: degrees = 90; break;
                case Surface.ROTATION_180: degrees = 180; break;
                case Surface.ROTATION_270: degrees = 270; break;
            }
            // getYiJing whether the camera is front camera or back camera
            final Camera.CameraInfo info =
                    new Camera.CameraInfo();
            Camera.getCameraInfo(CAMERA_ID, info);
            mIsFrontFace = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
            if (mIsFrontFace) {	// front camera
                degrees = (info.orientation + degrees) % 360;
                degrees = (360 - degrees) % 360;  // reverse
            } else {  // back camera
                degrees = (info.orientation - degrees + 360) % 360;
            }
            // apply rotation setting
            mCamera.setDisplayOrientation(degrees);
            // XXX This method fails to call and camera stops working on some devices.
//			params.setRotation(degrees);
            if(mListener!=null){
                mListener.OnCameraRotation(degrees);
            }
        }catch (Exception e){
            LogUtil.d("setRotation",e);
        }
        return degrees;
    }


    /**
     * stop camera preview
     */
    public  void stopPreview() {
        try {
            if (DEBUG) Log.v(TAG, "stopPreview:");
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }        }catch ( Exception e){
            LogUtil.e(getClass() +"stopPreview  ",e);
        }
    }

    /**
     * 相机的接口回掉
     */
    public interface OnCameraListener {
        void OnPermisson(boolean flag);

        void OnCameraSize(Camera.Size size);

        void OnCameraRotation(int rotation);
    }

}
