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

package org.photonvision;

import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.hardware.VisionLEDMode;
import org.photonvision.targeting.PhotonPipelineResult;

/** Represents a camera that is connected to PhotonVision. */
public class PhotonCamera {
    final NetworkTableEntry rawBytesEntry;
    final NetworkTableEntry driverModeEntry;
    final NetworkTableEntry inputSaveImgEntry;
    final NetworkTableEntry outputSaveImgEntry;
    final NetworkTableEntry pipelineIndexEntry;
    final NetworkTableEntry ledModeEntry;
    final NetworkTableEntry versionEntry;

    final NetworkTable mainTable = NetworkTableInstance.getDefault().getTable("photonvision");
    private final String path;

    boolean driverMode;
    int pipelineIndex;
    VisionLEDMode mode;

    Packet packet = new Packet(1);

    /**
    * Constructs a PhotonCamera from a root table.
    *
    * @param rootTable The root table that the camera is broadcasting information over.
    */
    public PhotonCamera(NetworkTable rootTable) {
        path = rootTable.getPath();
        rawBytesEntry = rootTable.getEntry("rawBytes");
        driverModeEntry = rootTable.getEntry("driverMode");
        inputSaveImgEntry = rootTable.getEntry("inputSaveImgCmd");
        outputSaveImgEntry = rootTable.getEntry("outputSaveImgCmd");
        pipelineIndexEntry = rootTable.getEntry("pipelineIndex");
        ledModeEntry = mainTable.getEntry("ledMode");
        versionEntry = mainTable.getEntry("version");

        driverMode = driverModeEntry.getBoolean(false);
        pipelineIndex = pipelineIndexEntry.getNumber(0).intValue();
        getLEDMode();
    }

    /**
    * Constructs a PhotonCamera from the name of the camera.
    *
    * @param cameraName The nickname of the camera (found in the PhotonVision UI).
    */
    public PhotonCamera(String cameraName) {
        this(NetworkTableInstance.getDefault().getTable("photonvision").getSubTable(cameraName));
    }

    /**
    * Returns the latest pipeline result.
    *
    * @return The latest pipeline result.
    */
    public PhotonPipelineResult getLatestResult() {
        verifyVersion();

        // Clear the packet.
        packet.clear();

        // Create latest result.
        var ret = new PhotonPipelineResult();

        // Populate packet and create result.
        packet.setData(rawBytesEntry.getRaw(new byte[] {}));
        if (packet.getSize() < 1) return ret;
        ret.createFromPacket(packet);

        // Return result.
        return ret;
    }

    /**
    * Returns whether the camera is in driver mode.
    *
    * @return Whether the camera is in driver mode.
    */
    public boolean getDriverMode() {
        return driverMode;
    }

    /**
    * Toggles driver mode.
    *
    * @param driverMode Whether to set driver mode.
    */
    public void setDriverMode(boolean driverMode) {
        if (this.driverMode != driverMode) {
            this.driverMode = driverMode;
            driverModeEntry.setBoolean(this.driverMode);
        }
    }

    /**
    * Request the camera to save a new image file from the input camera stream with overlays. Images
    * take up space in the filesystem of the PhotonCamera. Calling it frequently will fill up disk
    * space and eventually cause the system to stop working. Clear out images in
    * /opt/photonvision/photonvision_config/imgSaves frequently to prevent issues.
    */
    public void takeInputSnapshot() {
        inputSaveImgEntry.setBoolean(true);
    }

    /**
    * Request the camera to save a new image file from the output stream with overlays. Images take
    * up space in the filesystem of the PhotonCamera. Calling it frequently will fill up disk space
    * and eventually cause the system to stop working. Clear out images in
    * /opt/photonvision/photonvision_config/imgSaves frequently to prevent issues.
    */
    public void takeOutputSnapshot() {
        outputSaveImgEntry.setBoolean(true);
    }

    /**
    * Returns the active pipeline index.
    *
    * @return The active pipeline index.
    */
    public int getPipelineIndex() {
        return pipelineIndex;
    }

    /**
    * Allows the user to select the active pipeline index.
    *
    * @param index The active pipeline index.
    */
    public void setPipelineIndex(int index) {
        if (pipelineIndex != index) {
            pipelineIndex = index;
            pipelineIndexEntry.setNumber(pipelineIndex);
        }
    }

    /**
    * Returns the current LED mode.
    *
    * @return The current LED mode.
    */
    public VisionLEDMode getLEDMode() {
        int value = ledModeEntry.getNumber(-1).intValue();
        switch (value) {
            case 0:
                mode = VisionLEDMode.kOff;
                break;
            case 1:
                mode = VisionLEDMode.kOn;
                break;
            case 2:
                mode = VisionLEDMode.kBlink;
                break;
            case -1:
            default:
                mode = VisionLEDMode.kDefault;
                break;
        }
        return mode;
    }

    /**
    * Sets the LED mode.
    *
    * @param led The mode to set to.
    */
    public void setLED(VisionLEDMode led) {
        if (led != mode) {
            ledModeEntry.setNumber(led.value);
        }
    }

    /**
    * Returns whether the latest target result has targets.
    *
    * <p>This method is deprecated; {@link PhotonPipelineResult#hasTargets()} should be used instead.
    *
    * @deprecated This method should be replaced with {@link PhotonPipelineResult#hasTargets()}
    * @return Whether the latest target result has targets.
    */
    @Deprecated
    public boolean hasTargets() {
        return getLatestResult().hasTargets();
    }

    private void verifyVersion() {
        String versionString = versionEntry.getString("");
        if (versionString.equals("")) {
            DriverStation.reportError(
                    "PhotonVision coprocessor at path " + path + " not found on NetworkTables!", true);
        } else if (!PhotonVersion.versionMatches(versionString)) {
            DriverStation.reportError(
                    "Photon version "
                            + PhotonVersion.versionString
                            + " does not match coprocessor version "
                            + versionString
                            + "!",
                    true);
        }
    }
}
