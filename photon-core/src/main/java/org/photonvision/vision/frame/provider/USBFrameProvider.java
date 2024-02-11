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

package org.photonvision.vision.frame.provider;

import edu.wpi.first.cscore.CvSink;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.processes.VisionSourceSettables;

public class USBFrameProvider extends CpuImageProcessor {
    private final CvSink cvSink;

    @SuppressWarnings("SpellCheckingInspection")
    private final VisionSourceSettables settables;

    @SuppressWarnings("SpellCheckingInspection")
    public USBFrameProvider(CvSink sink, VisionSourceSettables visionSettables) {
        cvSink = sink;
        cvSink.setEnabled(true);
        this.settables = visionSettables;
    }

    @Override
    public CapturedFrame getInputMat() {
        var mat = new CVMat(); // We do this so that we don't fill a Mat in use by another thread
        // This is from wpi::Now, or WPIUtilJNI.now()
        long time =
                cvSink.grabFrame(mat.getMat())
                        * 1000; // Units are microseconds, epoch is the same as the Unix epoch

        // Sometimes CSCore gives us a zero frametime.
        if (time <= 1e-6) {
            time = MathUtils.wpiNanoTime();
        }

        return new CapturedFrame(mat, settables.getFrameStaticProperties(), time);
    }

    @Override
    public String getName() {
        return "USBFrameProvider - " + cvSink.getName();
    }
}
