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

package org.photonvision.vision.pipe.impl;

import edu.wpi.first.math.Pair;
import java.awt.*;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;
import org.photonvision.vision.frame.FrameDivisor;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.DualOffsetValues;
import org.photonvision.vision.opencv.ImageRotationMode;
import org.photonvision.vision.pipe.MutatingPipe;
import org.photonvision.vision.target.RobotOffsetPointMode;
import org.photonvision.vision.target.TargetCalculations;
import org.photonvision.vision.target.TrackedTarget;

public class Draw2dCrosshairPipe
    extends MutatingPipe<
        Pair<Mat, List<TrackedTarget>>, Draw2dCrosshairPipe.Draw2dCrosshairParams> {
  @Override
  protected Void process(Pair<Mat, List<TrackedTarget>> in) {
    if (!params.shouldDraw()) return null;

    var image = in.getFirst();

    if (params.showCrosshair()) {
      double x = params.frameStaticProperties().centerX;
      double y = params.frameStaticProperties().centerY;
      double scale =
          params.frameStaticProperties().imageWidth / (double) params.divisor().value / 32.0;

      switch (params.robotOffsetPointMode()) {
        case None -> {}
        case Single -> {
          if (params.singleOffsetPoint().x != 0 && params.singleOffsetPoint().y != 0) {
            x = params.singleOffsetPoint().x;
            y = params.singleOffsetPoint().y;
          }
        }
        case Dual -> {
          if (!in.getSecond().isEmpty()) {
            var target = in.getSecond().get(0);
            if (target != null) {
              var area = target.getArea();
              var offsetCrosshair =
                  TargetCalculations.calculateDualOffsetCrosshair(params.dualOffsetValues(), area);
              x = offsetCrosshair.x;
              y = offsetCrosshair.y;
            }
          }
        }
      }

      x /= (double) params.divisor().value;
      y /= (double) params.divisor().value;

      Point xMax = new Point(x + scale, y);
      Point xMin = new Point(x - scale, y);
      Point yMax = new Point(x, y + scale);
      Point yMin = new Point(x, y - scale);

      Imgproc.line(image, xMax, xMin, ColorHelper.colorToScalar(params.crosshairColor()));
      Imgproc.line(image, yMax, yMin, ColorHelper.colorToScalar(params.crosshairColor()));
    }
    return null;
  }

  public static record Draw2dCrosshairParams(
      boolean shouldDraw,
      RobotOffsetPointMode robotOffsetPointMode,
      Point singleOffsetPoint,
      DualOffsetValues dualOffsetValues,
      FrameStaticProperties frameStaticProperties,
      FrameDivisor divisor,
      ImageRotationMode rotMode,
      boolean showCrosshair,
      Color crosshairColor) {
    public Draw2dCrosshairParams(
        boolean shouldDraw,
        RobotOffsetPointMode robotOffsetPointMode,
        Point singleOffsetPoint,
        DualOffsetValues dualOffsetValues,
        FrameStaticProperties frameStaticProperties,
        FrameDivisor divisor,
        ImageRotationMode rotMode) {
      this(
          shouldDraw,
          robotOffsetPointMode,
          singleOffsetPoint,
          dualOffsetValues,
          frameStaticProperties,
          divisor,
          rotMode,
          true,
          Color.GREEN);
    }

    public Draw2dCrosshairParams(
        FrameStaticProperties frameStaticProperties,
        FrameDivisor divisor,
        ImageRotationMode rotMode) {
      this(
          true,
          RobotOffsetPointMode.None,
          new Point(),
          new DualOffsetValues(),
          frameStaticProperties,
          divisor,
          rotMode);
    }
  }
}
