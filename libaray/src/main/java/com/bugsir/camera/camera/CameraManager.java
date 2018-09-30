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

package com.bugsir.camera.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.bugsir.camera.open.OpenCamera;
import com.bugsir.camera.open.OpenCameraInterface;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
@SuppressWarnings("deprecation") // camera APIs
public final class CameraManager {

  private static final String TAG = CameraManager.class.getSimpleName();


  private final Context context;
  private final CameraConfigurationManager configManager;
  private OpenCamera camera;
  private AutoFocusManager autoFocusManager;
  private boolean initialized;
  private boolean previewing;
  private int requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA;

  public CameraManager(Context context) {
    this.context = context;
    this.configManager = new CameraConfigurationManager(context);
  }
  
  /**
   * Opens the camera driver and initializes the hardware parameters.
   *
   * @param holder The surface object which the camera will draw preview frames into.
   * @throws IOException Indicates the camera driver failed to open.
   */
  public synchronized void openDriver(SurfaceHolder holder) throws IOException {
    OpenCamera theCamera = camera;
    if (theCamera == null) {
      theCamera = OpenCameraInterface.open(requestedCameraId);
      if (theCamera == null) {
        throw new IOException("Camera.open() failed to return object from driver");
      }
      camera = theCamera;
    }

    if (!initialized) {
      initialized = true;
      configManager.initFromCameraParameters(theCamera);
    }

    Camera cameraObject = theCamera.getCamera();
    Camera.Parameters parameters = cameraObject.getParameters();
    String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save these, temporarily
    try {
      configManager.setDesiredCameraParameters(theCamera, false);
    } catch (RuntimeException re) {
      if (parametersFlattened != null) {
        parameters = cameraObject.getParameters();
        parameters.unflatten(parametersFlattened);
        try {
          cameraObject.setParameters(parameters);
          configManager.setDesiredCameraParameters(theCamera, true);
        } catch (RuntimeException re2) {
          // Well, darn. Give up
          Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
        }
      }
    }
    cameraObject.setPreviewDisplay(holder);

  }

  public synchronized boolean isOpen() {
    return camera != null;
  }

  /**
   * Closes the camera driver if still in use.
   */
  public synchronized void closeDriver() {
    if (camera != null) {
      stopPreview();
      camera.getCamera().release();
      camera = null;
    }
  }


  /**
   *
   * @param newSetting if {@code true}, light should be turned on if currently off. And vice versa.
   */
  public synchronized void setTorch(boolean newSetting) {
    OpenCamera theCamera = camera;
    if (theCamera != null && newSetting != configManager.getTorchState(theCamera.getCamera())) {
      boolean wasAutoFocusManager = autoFocusManager != null;
      if (wasAutoFocusManager) {
        autoFocusManager.stop();
        autoFocusManager = null;
      }
      configManager.setTorch(theCamera.getCamera(), newSetting);
      if (wasAutoFocusManager) {
        autoFocusManager = new AutoFocusManager(context, theCamera.getCamera());
        autoFocusManager.start();
      }
    }
  }

  public Point getCameraResolution() {
    return configManager.getCameraResolution();
  }

  public int getCameraDegree()
  {
    return  configManager.getCwRotationFromDisplayToCamera();
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   */
  public synchronized void startPreview() {
    OpenCamera theCamera = camera;
    if (theCamera != null && !previewing) {
      theCamera.getCamera().startPreview();
      previewing = true;
      autoFocusManager = new AutoFocusManager(context, theCamera.getCamera());
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   */
  public synchronized void stopPreview() {
    if (autoFocusManager != null) {
      autoFocusManager.stop();
      autoFocusManager = null;
    }
    if (camera != null && previewing) {
      camera.getCamera().stopPreview();
      previewing = false;
    }
  }
  /**
   * 开启自动对焦
   */
  public synchronized void openFocus()
  {
    if (autoFocusManager!=null)
    {
      autoFocusManager.start();
    }
  }

  /**
   * 关闭自动对焦
   */
  public synchronized void closeFocus()
  {
    if (autoFocusManager!=null)
    {
      autoFocusManager.stop();
    }
  }

  /**
   * 拍照
   *
   * @param shutter ShutterCallback
   * @param raw     PictureCallback
   * @param jpeg    PictureCallback
   */
  public synchronized void takePicture(final Camera.ShutterCallback shutter, final Camera.PictureCallback raw,
                                       final Camera.PictureCallback jpeg) {
    try
    {
      camera.getCamera().takePicture(shutter, raw, jpeg);
    }catch(Exception e)
    {
        e.printStackTrace();
    }
  }
}
