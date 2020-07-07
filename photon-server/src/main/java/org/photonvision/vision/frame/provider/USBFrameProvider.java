/*
 * Copyright (C) 2020 Photon Vision.
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
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.frame.FrameStaticProperties;
import org.photonvision.vision.opencv.CVMat;

public class USBFrameProvider implements FrameProvider {
    private final CvSink cvSink;
    private final FrameStaticProperties frameStaticProperties;
    private final CVMat mat;

    public USBFrameProvider(CvSink sink, FrameStaticProperties frameStaticProperties) {
        cvSink = sink;
        cvSink.setEnabled(true);
        this.frameStaticProperties = frameStaticProperties;
        mat = new CVMat();
    }

    @Override
    public Frame get() {
        if (mat.getMat() != null) {
            mat.release();
        }
        long time = cvSink.grabFrame(mat.getMat());
        return new Frame(mat, time, frameStaticProperties);
    }

    @Override
    public String getName() {
        return "USBFrameProvider - " + cvSink.getName();
    }
}
