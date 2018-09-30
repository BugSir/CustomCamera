/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bugsir.camera.sample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bugsir.camera.CameraManagerUtil;
import com.bugsir.camera.FinishListener;
import com.bugsir.camera.ICameraCallback;

public final class CaptureActivity extends Activity implements ICameraCallback, View.OnClickListener {

    private ImageView mIvClose;
    private TextView mTvTake;
    private ImageView mIvCancel;
    private ImageView mIvOk;
    private RelativeLayout mRlytBottom;
    private FrameLayout mLlytResult;
    private ImageView mIvResult;
    private Button mBtnReset;
    private CameraManagerUtil mCameraUtil;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.capture);
        initView();
    }

    private void initView() {
        mIvClose = findViewById(R.id.iv_close);
        mTvTake = findViewById(R.id.tv_take);
        mIvCancel = findViewById(R.id.iv_cancel);
        mIvOk = findViewById(R.id.iv_ok);
        mRlytBottom = findViewById(R.id.rlyt_bottom);
        mIvOk.setOnClickListener(this);
        mIvCancel.setOnClickListener(this);
        mTvTake.setOnClickListener(this);
        mLlytResult = findViewById(R.id.llyt_result);
        mIvResult = findViewById(R.id.iv_result);
        mBtnReset = findViewById(R.id.btn_reset);
        mBtnReset.setOnClickListener(this);
        mCameraUtil=CameraManagerUtil.with(this).setCropView(findViewById(R.id.llyt_crop)).setSurfaceView((SurfaceView) findViewById(R.id.sfv_camera)).setCameraCallback(this).setHasStatusBar(true);
    }

    @Override
    public void cameraError() {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    @Override
    public void takePhotoSuccess() {
        findViewById(R.id.rlyt_bottom).setVisibility(View.VISIBLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mCameraUtil.onWindowFocusChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED)
        {
            mCameraUtil.onResume();
        }else
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},888);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,  @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mCameraUtil.onResume();
    }

    @Override
    protected void onPause() {
        mCameraUtil.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mCameraUtil.onDestory();
        mCameraUtil=null;
        super.onDestroy();
    }




    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_close:
                break;
            case R.id.iv_cancel:
                mCameraUtil.cancelPhoto();
                mRlytBottom.setVisibility(View.GONE);
                break;
            case R.id.iv_ok:
                mIvResult.setImageBitmap(mCameraUtil.getPhotoResult());
                mLlytResult.setVisibility(View.VISIBLE);
                break;
            case R.id.tv_take:
                mCameraUtil.takePhoto();
                break;
            case R.id.btn_reset:
                mLlytResult.setVisibility(View.GONE);
                mIvResult.setImageBitmap(null);
                mIvCancel.performClick();
                break;
            default:
                break;
        }
    }
}
