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

package org.photonvision.vision.frame.consumer;

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.*;
import edu.wpi.first.util.PixelFormat;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.StaticFrames;
import org.photonvision.vision.opencv.CVMat;

public class MJPGFrameConsumer implements AutoCloseable {
  private static final double MAX_FRAMERATE = -1;
  private static final long MAX_FRAME_PERIOD_NS = Math.round(1e9 / MAX_FRAMERATE);

  private long lastFrameTimeNs;
  private CvSource cvSource;
  private MjpegServer mjpegServer;

  public MJPGFrameConsumer(String sourceName, int width, int height, int port) {
    this.cvSource = new CvSource(sourceName, PixelFormat.kMJPEG, width, height, 30);

    this.mjpegServer = new MjpegServer("serve_" + cvSource.getName(), port);
    mjpegServer.setSource(cvSource);
    mjpegServer.setCompression(75);
    CameraServer.addServer(mjpegServer);
  }

  public MJPGFrameConsumer(String name, int port) {
    this(name, 320, 240, port);
  }

  public void accept(CVMat image) {
    long now = MathUtils.wpiNanoTime();

    if (image == null || image.getMat() == null || image.getMat().empty()) {
      image.copyFrom(StaticFrames.LOST_MAT);
    }

    if (now - lastFrameTimeNs > MAX_FRAME_PERIOD_NS) {
      lastFrameTimeNs = now;
      cvSource.putFrame(image.getMat());
    }
  }

  @Override
  public void close() {
    CameraServer.removeServer(mjpegServer.getName());
    mjpegServer.close();
    cvSource.close();
    mjpegServer = null;
    cvSource = null;
  }
}
