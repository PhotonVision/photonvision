/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
    protected final NetworkTable rootTable;
    final NetworkTableEntry rawBytesEntry;
    final NetworkTableEntry driverModeEntry;
    final NetworkTableEntry inputSaveImgEntry;
    final NetworkTableEntry outputSaveImgEntry;
    final NetworkTableEntry pipelineIndexEntry;
    final NetworkTableEntry ledModeEntry;
    final NetworkTableEntry versionEntry;

    private final String path;

    private static boolean VERSION_CHECK_ENABLED = true;

    public static void setVersionCheckEnabled(boolean enabled) {
        VERSION_CHECK_ENABLED = enabled;
    }

    Packet packet = new Packet(1);

    /**
     * Constructs a PhotonCamera from a root table.
     *
     * @param instance The NetworkTableInstance to pull data from. This can be a custom instance in
     *     simulation, but should *usually* be the default NTInstance from
     *     NetworkTableInstance::getDefault
     * @param cameraName The name of the camera, as seen in the UI.
     */
    public PhotonCamera(NetworkTableInstance instance, String cameraName) {
        var mainTable = instance.getTable("photonvision");
        this.rootTable = mainTable.getSubTable(cameraName);
        path = rootTable.getPath();
        rawBytesEntry = rootTable.getEntry("rawBytes");
        driverModeEntry = rootTable.getEntry("driverMode");
        inputSaveImgEntry = rootTable.getEntry("inputSaveImgCmd");
        outputSaveImgEntry = rootTable.getEntry("outputSaveImgCmd");
        pipelineIndexEntry = rootTable.getEntry("pipelineIndex");
        ledModeEntry = mainTable.getEntry("ledMode");
        versionEntry = mainTable.getEntry("version");
    }

    /**
     * Constructs a PhotonCamera from the name of the camera.
     *
     * @param cameraName The nickname of the camera (found in the PhotonVision UI).
     */
    public PhotonCamera(String cameraName) {
        this(NetworkTableInstance.getDefault(), cameraName);
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
        return driverModeEntry.getBoolean(false);
    }

    /**
     * Toggles driver mode.
     *
     * @param driverMode Whether to set driver mode.
     */
    public void setDriverMode(boolean driverMode) {
        driverModeEntry.setBoolean(driverMode);
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
        return pipelineIndexEntry.getNumber(0).intValue();
    }

    /**
     * Allows the user to select the active pipeline index.
     *
     * @param index The active pipeline index.
     */
    public void setPipelineIndex(int index) {
        pipelineIndexEntry.setNumber(index);
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
                return VisionLEDMode.kOff;
            case 1:
                return VisionLEDMode.kOn;
            case 2:
                return VisionLEDMode.kBlink;
            case -1:
            default:
                return VisionLEDMode.kDefault;
        }
    }

    /**
     * Sets the LED mode.
     *
     * @param led The mode to set to.
     */
    public void setLED(VisionLEDMode led) {
        ledModeEntry.setNumber(led.value);
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

    /**
     * Returns the hostname of the camera.
     * This will return the same value that was given to the constructor as cameraName.
     * @return The hostname of the camera.
     */
    public String getName(){
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void verifyVersion() {
        if (!VERSION_CHECK_ENABLED) return;

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
