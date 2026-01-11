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

import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.cscore.CvSink;
import edu.wpi.first.cscore.UsbCamera;
import org.photonvision.vision.processes.VisionSourceSettables;

/**
 * A {@link FrameProvider} that creates its own CvSink for a duplicate camera. This allows duplicate
 * cameras to process frames from the same physical camera without thread-safety issues in cscore.
 *
 * <p>Each duplicate creates its own CvSink manually (not using CameraServer.getVideo() which caches
 * by camera name) to avoid conflicts. This ensures proper input/output stream handling.
 */
public class DuplicateFrameProvider extends USBFrameProvider {
    /**
     * Create a new DuplicateFrameProvider that creates its own CvSink.
     *
     * @param camera The source camera's UsbCamera object
     * @param settables The settables for this duplicate camera (used for frame properties)
     * @param duplicateName The name of this duplicate camera for logging
     */
    public DuplicateFrameProvider(
            UsbCamera camera, VisionSourceSettables settables, String duplicateName) {
        super(camera, createDuplicateCvSink(camera, duplicateName), settables, () -> {}, duplicateName);

        logger.info("Created duplicate frame provider with sink: DUPLICATE_" + duplicateName);
    }

    /** Creates a unique CvSink for this duplicate camera to avoid CameraServer caching. */
    private static CvSink createDuplicateCvSink(UsbCamera camera, String duplicateName) {
        String sinkName = "DUPLICATE_" + duplicateName;
        CvSink cvSink = new CvSink(sinkName);
        cvSink.setSource(camera);
        CameraServer.addServer(cvSink);
        return cvSink;
    }

    @Override
    public String getName() {
        return "DuplicateFrameProvider - " + cvSink.getName();
    }

    @Override
    public void release() {
        logger.debug("Releasing duplicate frame provider: " + cvSink.getName());
        CameraServer.removeServer(cvSink.getName());
        cvSink.close();
    }
}
