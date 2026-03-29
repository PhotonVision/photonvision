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

package org.photonvision.vision.frame;

import java.awt.Color;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.photonvision.common.util.ColorHelper;

public class StaticFrames {
    public static final Mat LOST_MAT = new Mat(120, 30 * 7, CvType.CV_8UC3);

    static {
        LOST_MAT.setTo(ColorHelper.colorToScalar(Color.BLACK));
        var col = 0;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 30, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0xa2a2a2)),
                -1);
        col += 30;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 30, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0xa2a300)),
                -1);
        col += 30;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 30, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x00a3a2)),
                -1);
        col += 30;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 30, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x00a200)),
                -1);
        col += 30;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 30, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x440090)),
                -1);
        col += 30;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 30, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x0000a2)),
                -1);
        col += 30;
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(col, 0, 30, LOST_MAT.height()),
                ColorHelper.colorToScalar(new Color(0)),
                -1);
        Imgproc.rectangle(
                LOST_MAT,
                new Rect(0, 100, LOST_MAT.width(), 20),
                ColorHelper.colorToScalar(new Color(0)),
                -1);
        Imgproc.rectangle(
                LOST_MAT, new Rect(30, 100, 60, 20), ColorHelper.colorToScalar(Color.WHITE), -1);

        Imgproc.putText(
                LOST_MAT, "Camera", new Point(28, 40), 0, 1.2, ColorHelper.colorToScalar(Color.white), 6);
        Imgproc.putText(
                LOST_MAT, "Lost", new Point(28, 90), 0, 1.2, ColorHelper.colorToScalar(Color.white), 6);
        Imgproc.putText(
                LOST_MAT, "Camera", new Point(28, 40), 0, 1.2, ColorHelper.colorToScalar(Color.RED), 2);
        Imgproc.putText(
                LOST_MAT, "Lost", new Point(28, 90), 0, 1.2, ColorHelper.colorToScalar(Color.RED), 2);
    }

    public StaticFrames() {}
}
