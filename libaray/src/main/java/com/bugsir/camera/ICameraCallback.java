package com.bugsir.camera;

/**
 * @author: BUG SIR
 * @date: 2018/9/28 19:55
 * @description: 相机相关操作回调
 */
public interface ICameraCallback {
    void cameraError();
    void takePhotoSuccess();
}
