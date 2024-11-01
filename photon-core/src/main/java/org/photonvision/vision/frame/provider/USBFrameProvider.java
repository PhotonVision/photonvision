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
import org.photonvision.common.logging.LogGroup;
import org.photonvision.common.logging.Logger;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.processes.VisionSourceSettables;

public class USBFrameProvider extends CpuImageProcessor {
    private final Logger logger;

    private final CvSink cvSink;

    @SuppressWarnings("SpellCheckingInspection")
    private final VisionSourceSettables settables;

    @SuppressWarnings("SpellCheckingInspection")
    public USBFrameProvider(CvSink sink, VisionSourceSettables visionSettables) {
        logger = new Logger(USBFrameProvider.class, sink.getName(), LogGroup.Camera);

        cvSink = sink;
        cvSink.setEnabled(true);
        this.settables = visionSettables;
    }

    @Override
    public CapturedFrame getInputMat() {
        // We allocate memory so we don't fill a Mat in use by another thread (memory model is easier)
        var mat = new CVMat();
        // This is from wpi::Now, or WPIUtilJNI.now(). The epoch from grabFrame is uS since
        // Hal::initialize was called
        long captureTimeNs = cvSink.grabFrame(mat.getMat()) * 1000;

        if (captureTimeNs == 0) {
            var error = cvSink.getError();
            logger.error("Error grabbing image: " + error);
        }

        return new CapturedFrame(mat, settables.getFrameStaticProperties(), captureTimeNs);
    }

    @Override
    public String getName() {
        return "USBFrameProvider - " + cvSink.getName();
    }
}
