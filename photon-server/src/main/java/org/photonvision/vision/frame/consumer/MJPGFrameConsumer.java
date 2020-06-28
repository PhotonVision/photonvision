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

package org.photonvision.vision.frame.consumer;

import edu.wpi.cscore.CvSink;
import edu.wpi.cscore.CvSource;
import edu.wpi.first.cameraserver.CameraServer;
import org.apache.commons.lang3.NotImplementedException;
import org.photonvision.common.configuration.CameraConfiguration;
import org.photonvision.vision.camera.USBCameraSource;
import org.photonvision.vision.frame.Frame;
import org.photonvision.vision.frame.FrameConsumer;
import org.photonvision.vision.processes.VisionSource;

public class MJPGFrameConsumer implements FrameConsumer {

    private final CvSource cvSource;

    public MJPGFrameConsumer(String sourceName, int width, int height) {
        this.cvSource = CameraServer.getInstance().putVideo(sourceName, width, height);
    }

    public MJPGFrameConsumer(USBCameraSource visionSource) {
        this(visionSource.getCamera().getName(),
            visionSource.getFrameProvider().get().image.getMat().width(),
            visionSource.getFrameProvider().get().image.getMat().height());
    }

    @Override
    public void accept(Frame frame) {
        cvSource.putFrame(frame.image.getMat());
    }
}
