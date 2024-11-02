/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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

import edu.wpi.first.hal.FRCNetComm.tResourceType;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.MatBuilder;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.numbers.*;
import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.BooleanSubscriber;
import edu.wpi.first.networktables.DoubleArraySubscriber;
import edu.wpi.first.networktables.IntegerEntry;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.MultiSubscriber;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.networktables.StringSubscriber;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.photonvision.common.hardware.VisionLEDMode;
import org.photonvision.common.networktables.PacketSubscriber;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.timesync.TimeSyncSingleton;

/** Represents a camera that is connected to PhotonVision. */
public class PhotonCamera implements AutoCloseable {
    private static int InstanceCount = 0;
    public static final String kTableName = "photonvision";

    private final NetworkTable cameraTable;
    PacketSubscriber<PhotonPipelineResult> resultSubscriber;
    BooleanPublisher driverModePublisher;
    BooleanSubscriber driverModeSubscriber;
    StringSubscriber versionEntry;
    IntegerEntry inputSaveImgEntry, outputSaveImgEntry;
    IntegerPublisher pipelineIndexRequest, ledModeRequest;
    IntegerSubscriber pipelineIndexState, ledModeState;
    IntegerSubscriber heartbeatEntry;
    DoubleArraySubscriber cameraIntrinsicsSubscriber;
    DoubleArraySubscriber cameraDistortionSubscriber;
    MultiSubscriber topicNameSubscriber;
    NetworkTable rootPhotonTable;

    @Override
    public void close() {
        resultSubscriber.close();
        driverModePublisher.close();
        driverModeSubscriber.close();
        versionEntry.close();
        inputSaveImgEntry.close();
        outputSaveImgEntry.close();
        pipelineIndexRequest.close();
        pipelineIndexState.close();
        ledModeRequest.close();
        ledModeState.close();
        pipelineIndexRequest.close();
        cameraIntrinsicsSubscriber.close();
        cameraDistortionSubscriber.close();
        topicNameSubscriber.close();
    }

    private final String path;
    private final String name;

    private static boolean VERSION_CHECK_ENABLED = true;
    private static long VERSION_CHECK_INTERVAL = 5;
    double lastVersionCheckTime = 0;

    private long prevHeartbeatValue = -1;
    private double prevHeartbeatChangeTime = 0;
    private static final double HEARTBEAT_DEBOUNCE_SEC = 0.5;

    double prevTimeSyncWarnTime = 0;
    private static final double WARN_DEBOUNCE_SEC = 5;

    public static void setVersionCheckEnabled(boolean enabled) {
        VERSION_CHECK_ENABLED = enabled;
    }

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
        rootPhotonTable = instance.getTable(kTableName);
        this.cameraTable = rootPhotonTable.getSubTable(cameraName);
        path = cameraTable.getPath();
        var rawBytesEntry =
                cameraTable
                        .getRawTopic("rawBytes")
                        .subscribe(
                                PhotonPipelineResult.photonStruct.getTypeString(),
                                new byte[] {},
                                PubSubOption.periodic(0.01),
                                PubSubOption.sendAll(true),
                                PubSubOption.pollStorage(20));
        resultSubscriber = new PacketSubscriber<>(rawBytesEntry, PhotonPipelineResult.photonStruct);
        driverModePublisher = cameraTable.getBooleanTopic("driverModeRequest").publish();
        driverModeSubscriber = cameraTable.getBooleanTopic("driverMode").subscribe(false);
        inputSaveImgEntry = cameraTable.getIntegerTopic("inputSaveImgCmd").getEntry(0);
        outputSaveImgEntry = cameraTable.getIntegerTopic("outputSaveImgCmd").getEntry(0);
        pipelineIndexRequest = cameraTable.getIntegerTopic("pipelineIndexRequest").publish();
        pipelineIndexState = cameraTable.getIntegerTopic("pipelineIndexState").subscribe(0);
        heartbeatEntry = cameraTable.getIntegerTopic("heartbeat").subscribe(-1);
        cameraIntrinsicsSubscriber =
                cameraTable.getDoubleArrayTopic("cameraIntrinsics").subscribe(null);
        cameraDistortionSubscriber =
                cameraTable.getDoubleArrayTopic("cameraDistortion").subscribe(null);

        ledModeRequest = rootPhotonTable.getIntegerTopic("ledModeRequest").publish();
        ledModeState = rootPhotonTable.getIntegerTopic("ledModeState").subscribe(-1);
        versionEntry = rootPhotonTable.getStringTopic("version").subscribe("");

        // Existing is enough to make this multisubscriber do its thing
        topicNameSubscriber =
                new MultiSubscriber(
                        instance, new String[] {"/photonvision/"}, PubSubOption.topicsOnly(true));

        HAL.report(tResourceType.kResourceType_PhotonCamera, InstanceCount);
        InstanceCount++;

        // HACK - start a TimeSyncServer, if we haven't yet.
        TimeSyncSingleton.load();
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
     * The list of pipeline results sent by PhotonVision since the last call to getAllUnreadResults().
     * Calling this function clears the internal FIFO queue, and multiple calls to
     * getAllUnreadResults() will return different (potentially empty) result arrays. Be careful to
     * call this exactly ONCE per loop of your robot code! FIFO depth is limited to 20 changes, so
     * make sure to call this frequently enough to avoid old results being discarded, too!
     */
    public List<PhotonPipelineResult> getAllUnreadResults() {
        verifyVersion();

        List<PhotonPipelineResult> ret = new ArrayList<>();

        // Grab the latest results. We don't care about the timestamps from NT - the metadata header has
        // this, latency compensated by the Time Sync Client
        var changes = resultSubscriber.getAllChanges();
        for (var c : changes) {
            var result = c.value;
            checkTimeSyncOrWarn(result);
            ret.add(result);
        }

        return ret;
    }

    /**
     * Returns the latest pipeline result. This is simply the most recent result Received via NT.
     * Calling this multiple times will always return the most recent result.
     *
     * <p>Replaced by {@link #getAllUnreadResults()} over getLatestResult, as this function can miss
     * results, or provide duplicate ones!
     */
    @Deprecated(since = "2024", forRemoval = true)
    public PhotonPipelineResult getLatestResult() {
        verifyVersion();

        // Grab the latest result. We don't care about the timestamp from NT - the metadata header has
        // this, latency compensated by the Time Sync Client
        var ret = resultSubscriber.get();

        if (ret.timestamp == 0) return new PhotonPipelineResult();

        var result = ret.value;

        checkTimeSyncOrWarn(result);

        return result;
    }

    private void checkTimeSyncOrWarn(PhotonPipelineResult result) {
        if (result.metadata.timeSinceLastPong > 5L * 1000000L) {
            if (Timer.getFPGATimestamp() > (prevTimeSyncWarnTime + WARN_DEBOUNCE_SEC)) {
                prevTimeSyncWarnTime = Timer.getFPGATimestamp();

                DriverStation.reportWarning(
                        "PhotonVision coprocessor at path "
                                + path
                                + " is not connected to the TimeSyncServer? It's been "
                                + String.format("%.2f", result.metadata.timeSinceLastPong / 1e6)
                                + "s since the coprocessor last heard a pong.\n\nCheck /photonvision/.timesync/{COPROCESSOR_HOSTNAME} for more information.",
                        false);
            }
        } else {
            // Got a valid packet, reset the last time
            prevTimeSyncWarnTime = 0;
        }
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
        return (int) pipelineIndexState.get(0);
    }

    /**
     * Allows the user to select the active pipeline index.
     *
     * @param index The active pipeline index.
     */
    public void setPipelineIndex(int index) {
        pipelineIndexRequest.set(index);
    }

    /**
     * Returns the current LED mode.
     *
     * @return The current LED mode.
     */
    public VisionLEDMode getLEDMode() {
        int value = (int) ledModeState.get(-1);
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
        ledModeRequest.set(led.value);
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

        return (now - prevHeartbeatChangeTime) < HEARTBEAT_DEBOUNCE_SEC;
    }

    public Optional<Matrix<N3, N3>> getCameraMatrix() {
        var cameraMatrix = cameraIntrinsicsSubscriber.get();
        if (cameraMatrix != null && cameraMatrix.length == 9) {
            return Optional.of(MatBuilder.fill(Nat.N3(), Nat.N3(), cameraMatrix));
        } else return Optional.empty();
    }

    /**
     * The camera calibration's distortion coefficients, in OPENCV8 form. Higher-order terms are set
     * to 0
     */
    public Optional<Matrix<N8, N1>> getDistCoeffs() {
        var distCoeffs = cameraDistortionSubscriber.get();
        if (distCoeffs != null && distCoeffs.length <= 8) {
            // Copy into array of length 8, and explicitly null higher order terms out
            double[] data = new double[8];
            Arrays.fill(data, 0);
            System.arraycopy(distCoeffs, 0, data, 0, distCoeffs.length);

            return Optional.of(MatBuilder.fill(Nat.N8(), Nat.N1(), data));
        } else return Optional.empty();
    }

    /**
     * Gets the NetworkTable representing this camera's subtable. You probably don't ever need to call
     * this.
     */
    public final NetworkTable getCameraTable() {
        return cameraTable;
    }

    void verifyVersion() {
        if (!VERSION_CHECK_ENABLED) return;

        if ((Timer.getFPGATimestamp() - lastVersionCheckTime) < VERSION_CHECK_INTERVAL) return;
        lastVersionCheckTime = Timer.getFPGATimestamp();

        // Heartbeat entry is assumed to always be present. If it's not present, we
        // assume that a camera with that name was never connected in the first place.
        if (!heartbeatEntry.exists()) {
            var cameraNames = getTablesThatLookLikePhotonCameras();
            if (cameraNames.isEmpty()) {
                DriverStation.reportError(
                        "Could not find **any** PhotonVision coprocessors on NetworkTables. Double check that PhotonVision is running, and that your camera is connected!",
                        false);
            } else {
                DriverStation.reportError(
                        "PhotonVision coprocessor at path "
                                + path
                                + " not found on NetworkTables. Double check that your camera names match!",
                        true);

                var cameraNameStr = new StringBuilder();
                for (var c : cameraNames) {
                    cameraNameStr.append(" ==> ");
                    cameraNameStr.append(c);
                    cameraNameStr.append("\n");
                }

                DriverStation.reportError(
                        "Found the following PhotonVision cameras on NetworkTables:\n"
                                + cameraNameStr.toString(),
                        false);
            }
        }
        // Check for connection status. Warn if disconnected.
        else if (!isConnected()) {
            DriverStation.reportWarning(
                    "PhotonVision coprocessor at path " + path + " is not sending new data.", false);
        }

        String versionString = versionEntry.get("");

        // Check mdef UUID
        String local_uuid = PhotonPipelineResult.photonStruct.getInterfaceUUID();
        String remote_uuid = resultSubscriber.getInterfaceUUID();

        if (remote_uuid == null || remote_uuid.isEmpty()) {
            // not connected yet?
            DriverStation.reportWarning(
                    "PhotonVision coprocessor at path "
                            + path
                            + " has not reported a message interface UUID - is your coprocessor's camera started?",
                    false);
        } else if (!local_uuid.equals(remote_uuid)) {
            // Error on a verified version mismatch
            // But stay silent otherwise

            String bfw =
                    "\n\n\n\n\n"
                            + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"
                            + ">>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
                            + ">>>                                          \n"
                            + ">>> You are running an incompatible version  \n"
                            + ">>> of PhotonVision on your coprocessor!     \n"
                            + ">>>                                          \n"
                            + ">>> This is neither tested nor supported.    \n"
                            + ">>> You MUST update PhotonVision,            \n"
                            + ">>> PhotonLib, or both.                      \n"
                            + ">>>                                          \n"
                            + ">>> Your code will now crash.                \n"
                            + ">>> We hope your day gets better.            \n"
                            + ">>>                                          \n"
                            + ">>> !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!\n"
                            + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n";

            DriverStation.reportWarning(bfw, false);
            var versionMismatchMessage =
                    "Photon version "
                            + PhotonVersion.versionString
                            + " (message definition version "
                            + local_uuid
                            + ")"
                            + " does not match coprocessor version "
                            + versionString
                            + " (message definition version "
                            + remote_uuid
                            + ")"
                            + "!";
            DriverStation.reportError(versionMismatchMessage, false);
            throw new UnsupportedOperationException(versionMismatchMessage);
        }
    }

    private List<String> getTablesThatLookLikePhotonCameras() {
        return rootPhotonTable.getSubTables().stream()
                .filter(
                        it -> {
                            return rootPhotonTable.getSubTable(it).getEntry("rawBytes").exists();
                        })
                .collect(Collectors.toList());
    }
}
