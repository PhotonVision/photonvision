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
    public static final Mat LOST_MAT = new Mat(60, 15 * 7, CvType.CV_8UC3);
    public static final Mat EMPTY_MAT = new Mat(60, 15 * 7, CvType.CV_8UC3);

    static {
        EMPTY_MAT.setTo(ColorHelper.colorToScalar(Color.BLACK));
        var col = 0;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0xa2a2a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0xa2a300)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x00a3a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x00a200)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x440045)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0x0000a2)),
                -1);
        col += 15;
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(col, 0, 15, EMPTY_MAT.height()),
                ColorHelper.colorToScalar(new Color(0)),
                -1);
        Imgproc.rectangle(
                EMPTY_MAT,
                new Rect(0, 50, EMPTY_MAT.width(), 10),
                ColorHelper.colorToScalar(new Color(0)),
                -1);
        Imgproc.rectangle(
                EMPTY_MAT, new Rect(15, 50, 30, 10), ColorHelper.colorToScalar(Color.WHITE), -1);

        EMPTY_MAT.copyTo(LOST_MAT);

        Imgproc.putText(
                EMPTY_MAT, "Stream", new Point(14, 20), 0, 0.6, ColorHelper.colorToScalar(Color.white), 2);
        Imgproc.putText(
                EMPTY_MAT,
                "Disabled",
                new Point(14, 45),
                0,
                0.6,
                ColorHelper.colorToScalar(Color.white),
                2);

        Imgproc.putText(
                EMPTY_MAT, "Stream", new Point(14, 20), 0, 0.6, ColorHelper.colorToScalar(Color.RED), 1);
        Imgproc.putText(
                EMPTY_MAT, "Disabled", new Point(14, 45), 0, 0.6, ColorHelper.colorToScalar(Color.RED), 1);

        Imgproc.putText(
                LOST_MAT, "Camera", new Point(14, 20), 0, 0.6, ColorHelper.colorToScalar(Color.white), 2);
        Imgproc.putText(
                LOST_MAT, "Lost", new Point(14, 45), 0, 0.6, ColorHelper.colorToScalar(Color.white), 2);
        Imgproc.putText(
                LOST_MAT, "Camera", new Point(14, 20), 0, 0.6, ColorHelper.colorToScalar(Color.RED), 1);
        Imgproc.putText(
                LOST_MAT, "Lost", new Point(14, 45), 0, 0.6, ColorHelper.colorToScalar(Color.RED), 1);
    }

    public StaticFrames() {}
}
