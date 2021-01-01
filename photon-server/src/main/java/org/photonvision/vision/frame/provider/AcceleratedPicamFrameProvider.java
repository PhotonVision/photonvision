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

import org.opencv.core.Mat;
import org.photonvision.raspi.PicamJNI;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameProvider;
import org.photonvision.vision.opencv.CVMat;
import org.photonvision.vision.processes.VisionSourceSettables;

public class AcceleratedPicamFrameProvider implements FrameProvider {

    private final VisionSourceSettables settables;

    private CVMat mat;

    public AcceleratedPicamFrameProvider(VisionSourceSettables visionSettables) {
        this.settables = visionSettables;

        var vidMode = settables.getCurrentVideoMode();
        PicamJNI.createCamera(vidMode.width, vidMode.height, vidMode.fps);
    }

    @Override
    public String getName() {
        return "AcceleratedPicamFrameProvider";
    }

    @Override
    public Frame get() {
        long matHandle = PicamJNI.grabFrame(false);
        mat = new CVMat(new Mat(matHandle));
        return new Frame(
                mat, System.nanoTime() - PicamJNI.getFrameLatency(), settables.getFrameStaticProperties());
    }
}
