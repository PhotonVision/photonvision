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

import edu.wpi.first.util.RuntimeLoader;
import java.io.IOException;
import java.util.Arrays;
import org.opencv.core.Core;
import org.opencv.imgcodecs.Imgcodecs;

public class MrginghamJniTest {
    public static void main(String[] args) throws IOException {
        MrginghamJNI.forceLoad();

        var loader =
                new RuntimeLoader<>(
                        Core.NATIVE_LIBRARY_NAME, RuntimeLoader.getDefaultExtractionRoot(), Core.class);
        loader.loadLibrary();

        var img =
                Imgcodecs.imread(
                        "/home/matt/Documents/GitHub/mrgingham/testimgs/1686868697564383507.jpeg",
                        Imgcodecs.IMREAD_IGNORE_ORIENTATION | Imgcodecs.IMREAD_GRAYSCALE);

        for (int i = 0; i < 100; i++) {
            var start = System.currentTimeMillis();
            var ret = MrginghamJNI.detectChessboard(img, true, 1, true, 7);
            var end = System.currentTimeMillis();
            var dt = (end - start);

            System.out.println(Arrays.toString(ret));
            System.out.printf("Ran in %f ms!", (double) dt);
        }
    }
}
