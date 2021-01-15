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

import edu.wpi.cscore.CvSink;
import org.photonvision.common.util.math.MathUtils;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.processes.VisionSourceSettables;

public class USBFrameProvider implements FrameProvider {
    private static final long unixEpochToNanoEpoch =
            System.nanoTime()
                    - MathUtils.millisToNanos(System.currentTimeMillis()); // Units are nanoseconds
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
    public Frame get() {
        var mat = new CVMat(); // We do this so that we don't fill a Mat in use by another thread
        long time =
                cvSink.grabFrame(
                        mat.getMat()); // Units are microseconds, epoch is the same as the Unix epoch
        return new Frame(
                mat,
                MathUtils.microsToNanos(time) + unixEpochToNanoEpoch,
                settables.getFrameStaticProperties());
    }

    @Override
    public String getName() {
        return "USBFrameProvider - " + cvSink.getName();
    }
}
