/*
 * Copyright (C) 2014 ZXing authors
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

import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Utility methods for configuring the Android camera.
 *
 * @author Sean Owen
 */
@SuppressWarnings("deprecation") // camera APIs
public final class CameraConfigurationUtils {

  private static final String TAG = "CameraConfiguration";

//  private static final Pattern SEMICOLON = Pattern.compile(";");

  private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
  private static final float MAX_EXPOSURE_COMPENSATION = 1.5f;
  private static final float MIN_EXPOSURE_COMPENSATION = 0.0f;
  private static final double MAX_ASPECT_DISTORTION = 0.15;

  private CameraConfigurationUtils() {
  }

  public static void setTorch(Camera.Parameters parameters, boolean on) {
    List<String> supportedFlashModes = parameters.getSupportedFlashModes();
    String flashMode;
    if (on) {
      flashMode = findSettableValue("flash mode",
                                    supportedFlashModes,
                                    Camera.Parameters.FLASH_MODE_TORCH,
                                    Camera.Parameters.FLASH_MODE_ON);
    } else {
      flashMode = findSettableValue("flash mode",
                                    supportedFlashModes,
                                    Camera.Parameters.FLASH_MODE_OFF);
    }
    if (flashMode != null) {
      if (flashMode.equals(parameters.getFlashMode())) {
        Log.i(TAG, "Flash mode already set to " + flashMode);
      } else {
        Log.i(TAG, "Setting flash mode to " + flashMode);
        parameters.setFlashMode(flashMode);
      }
    }
  }

  public static void setBestExposure(Camera.Parameters parameters, boolean lightOn) {
    int minExposure = parameters.getMinExposureCompensation();
    int maxExposure = parameters.getMaxExposureCompensation();
    float step = parameters.getExposureCompensationStep();
    if ((minExposure != 0 || maxExposure != 0) && step > 0.0f) {
      // Set low when light is on
      float targetCompensation = lightOn ? MIN_EXPOSURE_COMPENSATION : MAX_EXPOSURE_COMPENSATION;
      int compensationSteps = Math.round(targetCompensation / step);
      float actualCompensation = step * compensationSteps;
      // Clamp value:
      compensationSteps = Math.max(Math.min(compensationSteps, maxExposure), minExposure);
      if (parameters.getExposureCompensation() == compensationSteps) {
        Log.i(TAG, "Exposure compensation already set to " + compensationSteps + " / " + actualCompensation);
      } else {
        Log.i(TAG, "Setting exposure compensation to " + compensationSteps + " / " + actualCompensation);
        parameters.setExposureCompensation(compensationSteps);
      }
    } else {
      Log.i(TAG, "Camera does not support exposure compensation");
    }
  }


  public static void setZoom(Camera.Parameters parameters, double targetZoomRatio) {
    if (parameters.isZoomSupported()) {
      Integer zoom = indexOfClosestZoom(parameters, targetZoomRatio);
      if (zoom == null) {
        return;
      }
      if (parameters.getZoom() == zoom) {
        Log.i(TAG, "Zoom is already set to " + zoom);
      } else {
        Log.i(TAG, "Setting zoom to " + zoom);
        parameters.setZoom(zoom);
      }
    } else {
      Log.i(TAG, "Zoom is not supported");
    }
  }

  private static Integer indexOfClosestZoom(Camera.Parameters parameters, double targetZoomRatio) {
    List<Integer> ratios = parameters.getZoomRatios();
    Log.i(TAG, "Zoom ratios: " + ratios);
    int maxZoom = parameters.getMaxZoom();
    if (ratios == null || ratios.isEmpty() || ratios.size() != maxZoom + 1) {
      Log.w(TAG, "Invalid zoom ratios!");
      return null;
    }
    double target100 = 100.0 * targetZoomRatio;
    double smallestDiff = Double.POSITIVE_INFINITY;
    int closestIndex = 0;
    for (int i = 0; i < ratios.size(); i++) {
      double diff = Math.abs(ratios.get(i) - target100);
      if (diff < smallestDiff) {
        smallestDiff = diff;
        closestIndex = i;
      }
    }
    Log.i(TAG, "Chose zoom ratio of " + (ratios.get(closestIndex) / 100.0));
    return closestIndex;
  }


  public static Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {

    List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
    if (rawSupportedSizes == null) {
      Camera.Size defaultSize = parameters.getPreviewSize();
      if (defaultSize == null) {
        throw new IllegalStateException("Parameters contained no preview size!");
      }
      return new Point(defaultSize.width, defaultSize.height);
    }

    double screenAspectRatio = screenResolution.x / (double) screenResolution.y;

    // Find a suitable size, with max resolution
    int maxResolution = 0;
    Camera.Size maxResPreviewSize = null;
    for (Camera.Size size : rawSupportedSizes) {
      int realWidth = size.width;
      int realHeight = size.height;
      int resolution = realWidth * realHeight;
      if (resolution < MIN_PREVIEW_PIXELS) {
        continue;
      }

      boolean isCandidatePortrait = realWidth < realHeight;
      int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
      int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
      double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
      double distortion = Math.abs(aspectRatio - screenAspectRatio);
      if (distortion > MAX_ASPECT_DISTORTION) {
        continue;
      }

      if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
        Point exactPoint = new Point(realWidth, realHeight);
        return exactPoint;
      }

      // Resolution is suitable; record the one with max resolution
      if (resolution > maxResolution) {
        maxResolution = resolution;
        maxResPreviewSize = size;
      }
    }

    // If no exact match, use largest preview size. This was not a great idea on older devices because
    // of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
    // the CPU is much more powerful.
    if (maxResPreviewSize != null) {
      Point largestSize = new Point(maxResPreviewSize.width, maxResPreviewSize.height);
      return largestSize;
    }

    // If there is nothing at all suitable, return current preview size
    Camera.Size defaultPreview = parameters.getPreviewSize();
    if (defaultPreview == null) {
      throw new IllegalStateException("Parameters contained no preview size!");
    }
    Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
    return defaultSize;
  }

  private static String findSettableValue(String name,
                                          Collection<String> supportedValues,
                                          String... desiredValues) {
    Log.i(TAG, "Requesting " + name + " value from among: " + Arrays.toString(desiredValues));
    Log.i(TAG, "Supported " + name + " values: " + supportedValues);
    if (supportedValues != null) {
      for (String desiredValue : desiredValues) {
        if (supportedValues.contains(desiredValue)) {
          Log.i(TAG, "Can set " + name + " to: " + desiredValue);
          return desiredValue;
        }
      }
    }
    Log.i(TAG, "No supported values match");
    return null;
  }
}
