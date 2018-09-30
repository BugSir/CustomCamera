package com.bugsir.camera;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.bugsir.camera.camera.CameraManager;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * @author: BUG SIR
 * @date: 2018/9/28 19:23
 * @description: 相机操作管理工具类
 */
public class CameraManagerUtil implements SurfaceHolder.Callback {
    private Activity mContext;
    private CameraManager mCameraManager;
    private boolean hasSurface=false;
    private AmbientLightManager mAmbientLightManager;
    private BeepManager mBeepManager;
    private SurfaceView mSurfaceView;
    private Rect mCropRect;
    private View mCropView;
    private int mBeepResId;
    private byte[] mPicData;
    private int mStatusBarHeight=0;
    private ICameraCallback mCallback;

    private CameraManagerUtil(Activity context) {
        this.mContext = context;
        Window window = context.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mAmbientLightManager = new AmbientLightManager(mContext);
    }

    public static CameraManagerUtil with(Activity context) {
        return new CameraManagerUtil(context);
    }

    /**
     * @param surfaceView
     * @return
     */
    public CameraManagerUtil setSurfaceView(SurfaceView surfaceView) {
        this.mSurfaceView = surfaceView;
        return this;
    }

    /**
     * @param cropView 限制的拍照区域
     * @return
     */
    public CameraManagerUtil setCropView(View cropView) {
        this.mCropView = cropView;
        return this;
    }

    public CameraManagerUtil setBeepResId(int resId) {
        this.mBeepResId = resId;
        mBeepManager=new BeepManager(mContext,resId);
        return this;
    }

    public CameraManagerUtil setHasStatusBar(boolean has)
    {
        if (has)
        {
            mStatusBarHeight=getStatusBarHeight();
        }
        return this;
    }

    public CameraManagerUtil setCameraCallback(ICameraCallback cameraCallback)
    {
        this.mCallback=cameraCallback;
        return this;
    }

    public void takePhoto() {
        mCameraManager.closeFocus();
        if (mBeepManager!=null)
        {
            mBeepManager.playBeepSoundAndVibrate();
        }

        mCameraManager.takePicture(null, null, myjpegCallback);
    }

    /**
     * 取消拍照
     */
    public void cancelPhoto()
    {
        onResume();
    }

    /**
     * 拍照回调
     */
    Camera.PictureCallback myjpegCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            if (data == null) {
                return;
            }
            mPicData = data;
            if (mCallback!=null&&data!=null)
            {
                mCallback.takePhotoSuccess();
            }
        }
    };

    public Bitmap getPhotoResult() {
        try {
            if (mCropRect==null)
            {
                initCrop();
            }
            BitmapRegionDecoder regionDecoder = BitmapRegionDecoder.newInstance(mPicData, 0, mPicData.length, false);
            Bitmap bitmap = regionDecoder.decodeRegion(mCropRect, null);
            if (mCameraManager.getCameraDegree()==90||mCameraManager.getCameraDegree()==270)
            {//
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                //将照片转正
                Bitmap photo = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return photo;
            }else
            {
                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
//--------------------------------------------以下是activity里对应重写方法-----------------------------------------------------

    /**
     * activity重写onWindowFocusChanged 并调用此方法
     */
    public void onWindowFocusChanged() {
        if (mCropRect == null) {
            initCrop();
        }
    }

    public void onResume() {
        if (mPicData != null) {
            mPicData = null;
        }
        /**
         * 初始化camera
         */
        if (mCameraManager == null) {
            mCameraManager = new CameraManager(mContext.getApplicationContext());
        } else {
            mCameraManager.closeDriver();
        }
        mAmbientLightManager.start(mCameraManager);
        if (mBeepManager!=null) {
            mBeepManager.updatePrefs();
        }
        if (hasSurface) {
            initCamera(mSurfaceView.getHolder());
        } else {
            mSurfaceView.getHolder().addCallback(this);
        }

    }

    public void onPause() {
        if (mAmbientLightManager!=null)
        {
            mAmbientLightManager.stop();
        }

        if (mBeepManager!=null) {
            mBeepManager.close();
        }
        if (mCameraManager!=null)
        {
            mCameraManager.closeDriver();
        }

        if (!hasSurface)
        {
            mSurfaceView.getHolder().removeCallback(this);
        }
    }

    public void onDestory()
    {
        if (mCameraManager != null && mCameraManager.isOpen()) {
            mCameraManager.closeDriver();
        }
        if (mPicData != null) {
            mPicData = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        hasSurface = false;
        if (mCameraManager!=null)
        {
            mCameraManager.closeDriver();
        }
    }

    //---------------------------------------------以下是工具类的初始方法--------------------------------------------------
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (mCameraManager.isOpen()) {
            return;
        }
        try {
            mCameraManager.openDriver(surfaceHolder);
            mCameraManager.startPreview();
        } catch (IOException ioe) {
            //失败错误回调
            if (mCallback!=null)
            {
                mCallback.cameraError();
            }
//            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
//            displayFrameworkBugMessageAndExit();
            //失败错误回调
            if (mCallback!=null)
            {
                mCallback.cameraError();
            }
        }
    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        if (mCameraManager == null || mCameraManager.getCameraResolution() == null) {
            return;
        }
        boolean isPortrait = false;
        if (mCameraManager.getCameraDegree() == 90 || mCameraManager.getCameraDegree() == 270) {
            isPortrait = true;
        }
        //x,y翻转
        int cameraWidth = isPortrait ? mCameraManager.getCameraResolution().y : mCameraManager.getCameraResolution().x;
        int cameraHeight = isPortrait ? mCameraManager.getCameraResolution().x : mCameraManager.getCameraResolution().y;

        /** 获取布局中扫描框的位置信息 */
        int[] location = new int[2];
        mCropView.getLocationInWindow(location);

        int cropLeft = location[0];
        int cropTop = location[1]-mStatusBarHeight;

        int cropWidth = mCropView.getWidth();
        int cropHeight = mCropView.getHeight();

        /** 获取布局容器的宽高 */
        int containerWidth = mSurfaceView.getWidth();
        int containerHeight = mSurfaceView.getHeight();

        float scaleW = (float) cameraWidth / containerWidth;
        float scaleH = (float) cameraHeight / containerHeight;

        /** 计算最终截取的矩形的左上角顶点x坐标 */
        int x = (int) (cropLeft * scaleW);
        /** 计算最终截取的矩形的左上角顶点y坐标 */
        int y = (int) (cropTop * scaleH);
        /** 计算最终截取的矩形的宽度 */
        int width = (int) (cropWidth * scaleW);
        /** 计算最终截取的矩形的高度 */
        int height = (int) (cropHeight * scaleH);
        mCropRect = isPortrait ? new Rect(y, x, height + y, width + x) : new Rect(x, y, width + x, height + y);
    }

    /**
     * 获取通知栏高度
     *
     * @return 通知栏高度
     */
    public int getStatusBarHeight() {
        int statusBarHeight = 0;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object obj = clazz.newInstance();
            Field field = clazz.getField("status_bar_height");
            int temp = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = mContext.getResources().getDimensionPixelSize(temp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

}
