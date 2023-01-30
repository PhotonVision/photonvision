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

import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.BooleanSubscriber;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.IntegerEntry;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.MultiSubscriber;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.networktables.RawSubscriber;
import edu.wpi.first.networktables.StringSubscriber;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import java.util.Set;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.common.hardware.VisionLEDMode;
import org.photonvision.targeting.PhotonPipelineResult;

/** Represents a camera that is connected to PhotonVision. */
public class PhotonCamera {
    static final String kTableName = "photonvision";

    protected final NetworkTable rootTable;
    RawSubscriber rawBytesEntry;
    BooleanPublisher driverModePublisher;
    BooleanSubscriber driverModeSubscriber;
    DoublePublisher latencyMillisEntry;
    BooleanPublisher hasTargetEntry;
    DoublePublisher targetPitchEntry;
    DoublePublisher targetYawEntry;
    DoublePublisher targetAreaEntry;
    DoubleArrayPublisher targetPoseEntry;
    DoublePublisher targetSkewEntry;
    StringSubscriber versionEntry;
    IntegerEntry inputSaveImgEntry, outputSaveImgEntry;
    IntegerEntry pipelineIndexEntry, ledModeEntry;
    IntegerSubscriber heartbeatEntry;

    public void close() {
        rawBytesEntry.close();
        driverModePublisher.close();
        driverModeSubscriber.close();
        latencyMillisEntry.close();
        hasTargetEntry.close();
        targetPitchEntry.close();
        targetYawEntry.close();
        targetAreaEntry.close();
        targetPoseEntry.close();
        targetSkewEntry.close();
        versionEntry.close();
        inputSaveImgEntry.close();
        outputSaveImgEntry.close();
        pipelineIndexEntry.close();
        ledModeEntry.close();
    }

    private final String path;
    private final String name;

    private static boolean VERSION_CHECK_ENABLED = true;
    private static long VERSION_CHECK_INTERVAL = 5;
    private double lastVersionCheckTime = 0;

    private long prevHeartbeatValue = -1;
    private double prevHeartbeatChangeTime = 0;
    private static final double HEARBEAT_DEBOUNCE_SEC = 0.5;

    public static void setVersionCheckEnabled(boolean enabled) {
        VERSION_CHECK_ENABLED = enabled;
    }

    Packet packet = new Packet(1);

    private final MultiSubscriber m_topicNameSubscriber;

    /**
     * Constructs a PhotonCamera from a root table.
     *
     * @param instance The NetworkTableInstance to pull data from. This can be a custom instance in
     *     simulation, but should *usually* be the default NTInstance from
     *     NetworkTableInstance::getDefault
     * @param cameraName The name of the camera, as seen in the UI.
     */
    public PhotonCamera(NetworkTableInstance instance, String cameraName) {
        name = cameraName;
        var mainTable = instance.getTable(kTableName);
        this.rootTable = mainTable.getSubTable(cameraName);
        path = rootTable.getPath();
        rawBytesEntry =
                rootTable
                        .getRawTopic("rawBytes")
                        .subscribe(
                                "rawBytes", new byte[] {}, PubSubOption.periodic(0.01), PubSubOption.sendAll(true));
        driverModePublisher = rootTable.getBooleanTopic("driverMode").publish();
        driverModeSubscriber = rootTable.getBooleanTopic("driverModeRequest").subscribe(false);
        inputSaveImgEntry = rootTable.getIntegerTopic("inputSaveImgCmd").getEntry(0);
        outputSaveImgEntry = rootTable.getIntegerTopic("outputSaveImgCmd").getEntry(0);
        pipelineIndexEntry = rootTable.getIntegerTopic("pipelineIndex").getEntry(0);
        heartbeatEntry = rootTable.getIntegerTopic("heartbeat").subscribe(-1);
        ledModeEntry = mainTable.getIntegerTopic("ledMode").getEntry(-1);
        versionEntry = mainTable.getStringTopic("version").subscribe("");

        m_topicNameSubscriber =
                new MultiSubscriber(
                        instance,
                        new String[] {"/photonvision/"},
                        new PubSubOption[] {PubSubOption.topicsOnly(true)});
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
        packet.setData(rawBytesEntry.get(new byte[] {}));

        if (packet.getSize() < 1) return ret;
        ret.createFromPacket(packet);

        // Set the timestamp of the result.
        // getLatestChange returns in microseconds so we divide by 1e6 to convert to seconds.
        ret.setTimestampSeconds((rawBytesEntry.getLastChange() / 1e6) - ret.getLatencyMillis() / 1e3);

        // Return result.
        return ret;
    }

    /**
     * Returns whether the camera is in driver mode.
     *
     * @return Whether the camera is in driver mode.
     */
    public boolean getDriverMode() {
        return driverModeSubscriber.get();
    }

    /**
     * Toggles driver mode.
     *
     * @param driverMode Whether to set driver mode.
     */
    public void setDriverMode(boolean driverMode) {
        driverModePublisher.set(driverMode);
    }

    /**
     * Request the camera to save a new image file from the input camera stream with overlays. Images
     * take up space in the filesystem of the PhotonCamera. Calling it frequently will fill up disk
     * space and eventually cause the system to stop working. Clear out images in
     * /opt/photonvision/photonvision_config/imgSaves frequently to prevent issues.
     */
    public void takeInputSnapshot() {
        inputSaveImgEntry.set(inputSaveImgEntry.get() + 1);
    }

    /**
     * Request the camera to save a new image file from the output stream with overlays. Images take
     * up space in the filesystem of the PhotonCamera. Calling it frequently will fill up disk space
     * and eventually cause the system to stop working. Clear out images in
     * /opt/photonvision/photonvision_config/imgSaves frequently to prevent issues.
     */
    public void takeOutputSnapshot() {
        outputSaveImgEntry.set(outputSaveImgEntry.get() + 1);
    }

    /**
     * Returns the active pipeline index.
     *
     * @return The active pipeline index.
     */
    public int getPipelineIndex() {
        return (int) pipelineIndexEntry.get(0);
    }

    /**
     * Allows the user to select the active pipeline index.
     *
     * @param index The active pipeline index.
     */
    public void setPipelineIndex(int index) {
        pipelineIndexEntry.set(index);
    }

    /**
     * Returns the current LED mode.
     *
     * @return The current LED mode.
     */
    public VisionLEDMode getLEDMode() {
        int value = (int) ledModeEntry.get(-1);
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
        ledModeEntry.set(led.value);
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
     * Returns the name of the camera. This will return the same value that was given to the
     * constructor as cameraName.
     *
     * @return The name of the camera.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether the camera is connected and actively returning new data. Connection status is
     * debounced.
     *
     * @return True if the camera is actively sending frame data, false otherwise.
     */
    public boolean isConnected() {
        var curHeartbeat = heartbeatEntry.get();
        var now = Timer.getFPGATimestamp();

        if (curHeartbeat != prevHeartbeatValue) {
            // New heartbeat value from the coprocessor
            prevHeartbeatChangeTime = now;
            prevHeartbeatValue = curHeartbeat;
        }

        return (now - prevHeartbeatChangeTime) < HEARBEAT_DEBOUNCE_SEC;
    }

    private void verifyVersion() {
        if (!VERSION_CHECK_ENABLED) return;

        if ((Timer.getFPGATimestamp() - lastVersionCheckTime) < VERSION_CHECK_INTERVAL) return;
        lastVersionCheckTime = Timer.getFPGATimestamp();

        // Heartbeat entry is assumed to always be present. If it's not present, we
        // assume that a camera with that name was never connected in the first place.
        if (!heartbeatEntry.exists()) {
            Set<String> cameraNames = rootTable.getInstance().getTable(kTableName).getSubTables();
            if (cameraNames.isEmpty()) {
                DriverStation.reportError(
                        "Could not find any PhotonVision coprocessors on NetworkTables. Double check that PhotonVision is running, and that your camera is connected!",
                        false);
            } else {
                DriverStation.reportError(
                        "PhotonVision coprocessor at path "
                                + path
                                + " not found on NetworkTables. Double check that your camera names match!",
                        true);
                DriverStation.reportError(
                        "Found the following PhotonVision cameras on NetworkTables:\n"
                                + String.join("\n", cameraNames),
                        false);
            }
        }
        // Check for connection status. Warn if disconnected.
        else if (!isConnected()) {
            DriverStation.reportWarning(
                    "PhotonVision coprocessor at path " + path + " is not sending new data.", true);
        }

        // Check for version. Warn if the versions aren't aligned.
        String versionString = versionEntry.get("");
        if (!versionString.equals("") && !PhotonVersion.versionMatches(versionString)) {
            // Error on a verified version mismatch
            // But stay silent otherwise
            DriverStation.reportWarning(
                    "Photon version "
                            + PhotonVersion.versionString
                            + " does not match coprocessor version "
                            + versionString
                            + "!",
                    true);
        }
    }
}
