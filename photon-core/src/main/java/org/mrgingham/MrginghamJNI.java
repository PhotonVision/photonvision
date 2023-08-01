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

package org.mrgingham;

import java.io.IOException;
import org.opencv.core.Mat;
import org.photonvision.jni.PhotonJniCommon;

public class MrginghamJNI extends PhotonJniCommon {
    public static synchronized void forceLoad() throws IOException {
        forceLoad(MrginghamJNI.class);
    }

    private static native Object[] detectChessboardNative(
            long imageNativeObj, boolean doClAHE, int blurRadius, boolean do_refine, int gridn);

    public static PointDouble[] detectChessboard(
            Mat grayImage, boolean doClAHE, int blurRadius, boolean do_refine, int gridn) {
        return (PointDouble[])
                detectChessboardNative(grayImage.getNativeObjAddr(), doClAHE, blurRadius, do_refine, gridn);
    }
}
