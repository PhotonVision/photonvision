/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.photonvision.vision.camera.csi;

import edu.wpi.first.cscore.VideoMode;
import edu.wpi.first.util.PixelFormat;
import java.util.HashMap;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.processes.VisionSourceSettables;
import org.photonvision.vision.camera.csi.LibcameraGpuSource.FPSRatedVideoMode;

public class GstreamerSettables extends VisionSourceSettables {
  static int width = 1456;
  static int height = 1088;
  static int fps;
  public final Object CAMERA_LOCK = new Object();
  // private HashMap<Integer, VideoMode> videoModes = new HashMap<Integer, VideoMode>();

  public GstreamerSettables(CameraConfiguration configuration) {
    super(configuration);
    videoModes.put(0, new FPSRatedVideoMode(PixelFormat.kUnknown, width, height, 60, 60, .39));
  }

  @Override
  protected void setVideoModeInternal(VideoMode videoMode) {
  }

  @Override
  public double getMinExposureRaw() {
    return 0;
  }

  @Override
  public double getMaxExposureRaw() {
    return 0;
  }

  @Override
  public void setWhiteBalanceTemp(double temp) {
  }

  @Override
  public double getMinWhiteBalanceTemp() {
    return 0;
  }

  @Override
  public double getMaxWhiteBalanceTemp() {
    return 0;
  }

  @Override
  public void setBrightness(int brightness) {
  }

  @Override
  public void setAutoExposure(boolean cameraAutoExposure) {
  }

  @Override
  public HashMap<Integer, VideoMode> getAllVideoModes() {
    return videoModes;
  }

  @Override
  public void setGain(int gain) {
  }

  @Override
  public void setExposureRaw(double exposureRaw) {
  }

  @Override
  public VideoMode getCurrentVideoMode() {
    return new FPSRatedVideoMode(PixelFormat.kUnknown, width, height, 60, 60, .39);
  }

  @Override
  public void setAutoWhiteBalance(boolean autowb) {
  }
}
