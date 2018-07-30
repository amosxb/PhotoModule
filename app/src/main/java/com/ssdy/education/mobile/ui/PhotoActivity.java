package com.ssdy.education.mobile.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.ssdy.education.mobile.R;
import com.ssdy.education.mobile.utils.BitmapUtil;
import com.ssdy.education.mobile.utils.CameraManager;
import com.ssdy.education.mobile.utils.CropView;
import com.ssdy.education.mobile.utils.DisplayMetricsUtil;
import com.ssdy.education.mobile.utils.FileUtils;
import com.ssdy.education.mobile.utils.LogUtil;
import com.ssdy.education.mobile.utils.MyOrientationDetector;
import com.ssdy.education.mobile.utils.ToastUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 描述：设置照相机拍照
 * 作者：shaobing
 * 时间： 2017/2/26 19:34
 */
public class PhotoActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.AutoFocusCallback {
    public static final String FILEPATH = "filepath";
    public static final String TAG = "PhotoActivity";
    public static final int REQUESTCODE = 111;
    public static final String IS_SQUARE = "is_square";
    public static final int RESULT_FAIL = 11;

    //预览界面
    @Bind(R.id.cameraView)
    SurfaceView cameraView;
    @Bind(R.id.civ_crop)
    CropView civCrop;
    @Bind(R.id.iv_focuse)
    ImageView ivFocuse;
    private SurfaceHolder surfaceHolder;
    //照相机布局
    @Bind(R.id.rlyt_camera)
    RelativeLayout rlytCamera;
    //开始拍照界面
    @Bind(R.id.iv_start)
    ImageView ivStart;
    //取消拍照接卖弄
    @Bind(R.id.tv_cancel)
    TextView tvCancel;
    //
    @Bind(R.id.RelativeLayout1)
    RelativeLayout RelativeLayout1;

    @Bind(R.id.photo_controller_layout)
    LinearLayout photoControllerLayout;
    //文件路径
    private String filePath = "";
    //权限判断
    private boolean isPermisson;
    //照相机
    private Camera mCamera;
    private Camera.Size mCameraSize;
    private int mRotation;
    // 截图区域为方形   true 为方形  false 默认形状
    private boolean isSquare;
    //是否已经拍照
    private boolean isPhotoed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_photo);
        ButterKnife.bind(this);
        filePath = getIntent().getStringExtra(FILEPATH);
        isSquare = getIntent().getBooleanExtra(IS_SQUARE, false);
        initData();
        initEvent();
    }


    private void initData() {
        float width = DisplayMetricsUtil.getDisplayWidth(PhotoActivity.this);
        float height = DisplayMetricsUtil.getDisplayHeight(PhotoActivity.this);
        mCamera = CameraManager.getInstance().init(PhotoActivity.this, (int) height, (int) width, new CameraManager.OnCameraListener() {
            @Override
            public void OnPermisson(boolean flag) {
                ToastUtil.showLongToast(PhotoActivity.this, getString(R.string.jurisdiction_camera_refuse));
                isPermisson = true;
            }

            @Override
            public void OnCameraSize(Camera.Size size) {
                LogUtil.d("OnCameraSize  " + size.width + "  " + size.height);
                mCameraSize = size;
            }

            @Override
            public void OnCameraRotation(int rotation) {
                mRotation = rotation;
            }
        });
        surfaceHolder = cameraView.getHolder();
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        if (mCameraSize != null) {
            surfaceHolder.setFixedSize(mCameraSize.width, mCameraSize.height);
        }
        surfaceHolder.addCallback(this);
        try {
            if (null == filePath || filePath.isEmpty()) {
                filePath = Environment.getExternalStorageDirectory() + "/aaaaaa/" + System.currentTimeMillis() + ".jpg";
            }
            File file = new File(filePath);
            LogUtil.d(file.getAbsolutePath());
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            filePath = file.getAbsolutePath();
        } catch (Exception e) {
            LogUtil.d("videoFilePath ERROR: ", e);
        }
        if (isSquare) {
            civCrop.setIsSquare(true);
        }
    }


    private void initEvent() {
        cameraView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        focusOnTouch((int) event.getX(), (int) event.getY());
                        break;
                    }
                }
                return false;
            }
        });
    }

    /**
     * 设置界面
     *
     * @param state 0 默认界面  1.点击拍照之后的界面 2.剪切中
     */
    private void handleView(int state) {
        switch (state) {
            case 0:
                handleImage(this, ivStart, R.drawable.take_photo_anwser_nor);
                tvCancel.setTag("close");
                tvCancel.setText(getString(R.string.close));
                civCrop.setVisibility(View.GONE);
                ivStart.setTag("start");
                break;
            case 1:
                handleImage(this, ivStart, R.drawable.icon_sure_anwser_nor);
                tvCancel.setTag("cancel");
                tvCancel.setText(getString(R.string.cancel));
                civCrop.setVisibility(View.VISIBLE);
                ivStart.setTag("use");
                break;
            case 2:
                ivStart.setClickable(false);
                tvCancel.setClickable(false);
                tvCancel.setText(getString(R.string.cute));
                break;
        }
    }


    /**
     * 聚焦
     *
     * @param x
     * @param y
     */
    private void focusOnTouch(int x, int y) {
        Rect rect = new Rect(x - 100, y - 100, x + 100, y + 100);
        int left = rect.left * 2000 / cameraView.getWidth() - 1000;
        int top = rect.top * 2000 / cameraView.getHeight() - 1000;
        int right = rect.right * 2000 / cameraView.getWidth() - 1000;
        int bottom = rect.bottom * 2000 / cameraView.getHeight() - 1000;
        // 如果超出了(-1000,1000)到(1000, 1000)的范围，则会导致相机崩溃
        left = left < -1000 ? -1000 : left;
        top = top < -1000 ? -1000 : top;
        right = right > 1000 ? 1000 : right;
        bottom = bottom > 1000 ? 1000 : bottom;
        focusOnRect(new Rect(left, top, right, bottom));
        if (ivFocuse != null) {
            ivFocuse.setVisibility(View.VISIBLE);
            ivFocuse.setX(x - DisplayMetricsUtil.dip2px(this, 30));
            ivFocuse.setY(y - DisplayMetricsUtil.dip2px(this, 30));
        }
    }

    protected void focusOnRect(Rect rect) {
        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters(); // 先获取当前相机的参数配置对象
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // 设置聚焦模式
            Log.d(TAG, "parameters.getMaxNumFocusAreas() : " + parameters.getMaxNumFocusAreas());
            if (parameters.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(new Camera.Area(rect, 1000));
                parameters.setFocusAreas(focusAreas);
            }
            mCamera.cancelAutoFocus(); // 先要取消掉进程中所有的聚焦功能
            mCamera.setParameters(parameters); // 一定要记得把相应参数设置给相机
            mCamera.autoFocus(this);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        MyOrientationDetector.getInstance(this).enable();

    }

    @Override
    protected void onPause() {
        super.onPause();
        MyOrientationDetector.getInstance(this).disable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!isPhotoed) {
            FileUtils.delFileByPath(filePath);
            setResult(RESULT_FAIL);
        }
        CameraManager.getInstance().stopPreview();
    }

    @OnClick({R.id.cameraView, R.id.iv_start, R.id.tv_cancel})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_start:
                if (ivStart.getTag().equals("start")) {
                    //拍照
                    //判断权限
                    if (isPermisson) {
                        return;
                    }
                    CameraManager.getInstance().doTakePicture(pictureCallback);
                    handleView(1);
                } else {
                    //使用
                    //设置状态剪切中
                    handleView(2);
                    getImage(mData);
                    setResult(RESULT_OK);
                    isPhotoed = true;
                    finish();
                }
                break;
            case R.id.tv_cancel:
                if ("close".equals(tvCancel.getTag())) {
                    finish();
                } else {
                    CameraManager.getInstance().start();
                    handleView(0);
                }
                break;
        }
    }

    //获取图片数据
    private byte[] mData;

    /**
     * 图片回调
     */
    // 0   竖上  270 左横   90 右横  180 竖下
    Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            LogUtil.d("onPictureTaken    " + data);
            mData = data;
            CameraManager.getInstance().stop();
        }
    };

    /**
     * 获取并且保存图片
     *
     * @param data
     */
    public void getImage(byte[] data) {
        if (data == null || data.length == 0) {
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        //旋转到和屏幕一致的角度
        mRotation = CameraManager.getInstance().setRotation(PhotoActivity.this, mCamera.getParameters(), null);
        Bitmap bitmapRotate = BitmapUtil.adjustPhotoRotation2(bitmap, mRotation);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
            bitmap = null;
        }
        int height = 0;
        int width = 0;
        if (bitmapRotate.getHeight() > bitmapRotate.getWidth()) {
            height = bitmapRotate.getHeight();
            width = bitmapRotate.getWidth();
        } else {
            height = bitmapRotate.getWidth();
            width = bitmapRotate.getHeight();
        }
        //获取拍照画面宽高
        RectF r1 = new RectF(0, 0, civCrop.getWidth(), civCrop.getHeight() - photoControllerLayout.getHeight());
        //获取图片宽高
        RectF r2 = new RectF(0, 0, width, height);
        //图片和拍照画面映射拉伸
        Matrix mat = new Matrix();
        mat.setRectToRect(r1, r2, Matrix.ScaleToFit.FILL);
        //获取对应点
        RectF rect = civCrop.getCropRect();
        mat.mapRect(rect);

//        RectF rect= civCrop.getCropRect();   //1080*1920;
//        rect.left = (rect.left / (float) DisplayMetricsUtil.getDisplayWidth(PhotoActivity.this) * width);
//        rect.right = (rect.right / (float) DisplayMetricsUtil.getDisplayWidth(PhotoActivity.this) * width);
//        rect.top = (rect.top / (float) DisplayMetricsUtil.getDisplayHeight(PhotoActivity.this) * height);
//        rect.bottom = (rect.bottom / (float) DisplayMetricsUtil.getDisplayHeight(PhotoActivity.this) * height);
//
//        if(rect.right>bitmapRotate.getWidth()){
//            rect.right =bitmapRotate.getWidth();
//        }
//        if(rect.bottom>bitmapRotate.getHeight()){
//            rect.bottom =bitmapRotate.getHeight();
//        }
//        if(rect.left<0){
//            rect.left = 0;
//        }
//        if(rect.top<0){
//            rect.top = 0;
//        }

        //剪切
        Bitmap bmp = Bitmap.createBitmap(bitmapRotate, (int) rect.left, (int) rect.top, (int) (rect.right - rect.left), (int) (rect.bottom - rect.top), null, false);
        if (bitmapRotate != null && !bitmapRotate.isRecycled()) {
            bitmapRotate.recycle();
            bitmapRotate = null;
        }
        //旋转到正常的角度
        Bitmap bmpRotate = BitmapUtil.adjustPhotoRotation2(bmp, MyOrientationDetector.getInstance(PhotoActivity.this).getScreenOrientation());
        if (bmp != null && !bmp.isRecycled()) {
            bmp.recycle();
            bmp = null;
        }
        FileUtils.saveBitmap(bmpRotate, filePath);
    }


    /**
     * @param drawId
     */
    public static void handleImage(final Context context, final ImageView ivRecord, final int drawId) {
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.anim_photo_in);
        final Animation animation2 = AnimationUtils.loadAnimation(context, R.anim.anim_photo_out);
        ivRecord.startAnimation(animation);
        ivRecord.setClickable(false);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (ivRecord != null) {
                    ivRecord.startAnimation(animation2);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        animation2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (ivRecord != null) {
                    ivRecord.setClickable(true);
                    ivRecord.setImageDrawable(context.getResources().getDrawable(drawId));
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        CameraManager.getInstance().setPreview(holder);
        CameraManager.getInstance().start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CameraManager.getInstance().stop();
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        LogUtil.d("onAutoFocus  :" + success);
//        camera.autoFocus(this);
        if (ivFocuse != null) {
            ivFocuse.setVisibility(View.GONE);
        }
    }
}
