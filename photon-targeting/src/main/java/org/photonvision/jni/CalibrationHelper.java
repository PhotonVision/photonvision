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

package org.photonvision.jni;

public class CalibrationHelper {
    public static class CalResult {}

    public static native long Create(int width, int height, long overlayMatPtr, double tolerance);

    public static native long Destroy();

    public static native CalResult Detect(long inputImg, long outputImg);

    public static void main(String[] args) {
        System.load(
                "/home/matt/Documents/GitHub/photonvision/photon-core/build/libs/photoncoreJNI/shared/linuxx86-64/release/libphotoncorejni.so");
        System.out.println(Create(1, 2, 3, 4));
    }
}
