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

package org.photonvision.common.dataflow.networktables;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.opencv.core.Point;
import org.photonvision.common.dataflow.CVPipelineResultConsumer;
import org.photonvision.common.dataflow.structures.Packet;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import org.photonvision.targeting.TargetCorner;
import org.photonvision.vision.pipeline.result.CVPipelineResult;
import org.photonvision.vision.target.TrackedTarget;

import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.BooleanSubscriber;
import edu.wpi.first.networktables.BooleanTopic;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.IntegerPublisher;
import edu.wpi.first.networktables.IntegerSubscriber;
import edu.wpi.first.networktables.IntegerTopic;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEvent;
import edu.wpi.first.networktables.PubSubOption;
import edu.wpi.first.networktables.RawPublisher;

public class NTDataPublisher implements CVPipelineResultConsumer {
    private final NetworkTable rootTable = NetworkTablesManager.getInstance().kRootTable;
    private NetworkTable subTable;
    private RawPublisher rawBytesEntry;

    private IntegerTopic pipelineIndexTopic;
    private IntegerPublisher pipelineIndexPublisher;
    private IntegerSubscriber pipelineIndexSubscriber;

    private final Supplier<Integer> pipelineIndexSupplier;
    private final Consumer<Integer> pipelineIndexConsumer;
    private NTDataChangeListener pipelineIndexListener;

    private BooleanTopic driverModeEntry;
    private BooleanPublisher driverModePublisher;
    private BooleanSubscriber driverModeSubscriber;
    private final BooleanSupplier driverModeSupplier;
    private final Consumer<Boolean> driverModeConsumer;
    private NTDataChangeListener driverModeListener;

    private DoublePublisher latencyMillisEntry;
    private BooleanPublisher hasTargetEntry;
    private DoublePublisher targetPitchEntry;
    private DoublePublisher targetYawEntry;
    private DoublePublisher targetAreaEntry;
    private DoubleArrayPublisher targetPoseEntry;
    private DoublePublisher targetSkewEntry;

    // The raw position of the best target, in pixels.
    private DoublePublisher bestTargetPosX;
    private DoublePublisher bestTargetPosY;



    public NTDataPublisher(
            String cameraNickname,
            Supplier<Integer> pipelineIndexSupplier,
            Consumer<Integer> pipelineIndexConsumer,
            BooleanSupplier driverModeSupplier,
            Consumer<Boolean> driverModeConsumer) {
        this.pipelineIndexSupplier = pipelineIndexSupplier;
        this.pipelineIndexConsumer = pipelineIndexConsumer;
        this.driverModeSupplier = driverModeSupplier;
        this.driverModeConsumer = driverModeConsumer;

        updateCameraNickname(cameraNickname);
        updateEntries();
    }

    private void onPipelineIndexChange(NetworkTableEvent entryNotification) {
        var newIndex = (int) entryNotification.valueData.value.getDouble();
        var originalIndex = pipelineIndexSupplier.get();

        // ignore indexes below 0
        if (newIndex < 0) {
            pipelineIndexPublisher.set(originalIndex);
            return;
        }

        if (newIndex == originalIndex) {
            // TODO: Log
            return;
        }

        pipelineIndexConsumer.accept(newIndex);
        var setIndex = pipelineIndexSupplier.get();
        if (newIndex != setIndex) { // set failed
            pipelineIndexPublisher.set(setIndex);
            // TODO: Log
        }
        // TODO: Log
    }

    private void onDriverModeChange(NetworkTableEvent entryNotification) {
        var newDriverMode = entryNotification.valueData.value.getBoolean();
        var originalDriverMode = driverModeSupplier.getAsBoolean();

        if (newDriverMode == originalDriverMode) {
            // TODO: Log
            return;
        }

        driverModeConsumer.accept(newDriverMode);
        // TODO: Log
    }

    @SuppressWarnings("DuplicatedCode")
    private void removeEntries() {
        if (rawBytesEntry != null) rawBytesEntry.close();
        if (pipelineIndexListener != null) pipelineIndexListener.remove();
        if (pipelineIndexPublisher != null) pipelineIndexPublisher.close();
        if (pipelineIndexSubscriber != null) pipelineIndexSubscriber.close();

        if (driverModeListener != null) driverModeListener.remove();
        if (driverModePublisher != null) driverModePublisher.close();
        if (driverModeSubscriber != null) driverModeSubscriber.close();

        if (latencyMillisEntry != null) latencyMillisEntry.close();
        if (hasTargetEntry != null) hasTargetEntry.close();
        if (targetPitchEntry != null) targetPitchEntry.close();
        if (targetAreaEntry != null) targetAreaEntry.close();
        if (targetYawEntry != null) targetYawEntry.close();
        if (targetPoseEntry != null) targetPoseEntry.close();
        if (targetSkewEntry != null) targetSkewEntry.close();
        if (bestTargetPosX != null) bestTargetPosX.close();
        if (bestTargetPosY != null) bestTargetPosY.close();
    }

    private void updateEntries() {
        rawBytesEntry = subTable.getRawTopic("rawBytes").getEntry("", new byte[0], new PubSubOption[0]);

        if (pipelineIndexListener != null) {
            pipelineIndexListener.remove();
        }

        pipelineIndexTopic = subTable.getIntegerTopic("pipelineIndex");
        pipelineIndexPublisher = pipelineIndexTopic.publish();
        pipelineIndexSubscriber = pipelineIndexTopic.subscribe(0);
        pipelineIndexListener =
                new NTDataChangeListener(subTable.getInstance(), pipelineIndexSubscriber, this::onPipelineIndexChange);

        if (driverModeListener != null) {
            driverModeListener.remove();
        }
        driverModeEntry = subTable.getBooleanTopic("driverMode");
        driverModePublisher = driverModeEntry.publish();
        driverModeSubscriber = driverModeEntry.subscribe(false);
        driverModeListener = new NTDataChangeListener(subTable.getInstance(), driverModeSubscriber, this::onDriverModeChange);

        latencyMillisEntry = subTable.getDoubleTopic("latencyMillis").publish();
        hasTargetEntry = subTable.getBooleanTopic("hasTarget").publish();

        targetPitchEntry = subTable.getDoubleTopic("targetPitch").publish();
        targetAreaEntry = subTable.getDoubleTopic("targetArea").publish();
        targetYawEntry = subTable.getDoubleTopic("targetYaw").publish();
        targetPoseEntry = subTable.getDoubleArrayTopic("targetPose").publish();
        targetSkewEntry = subTable.getDoubleTopic("targetSkew").publish();

        bestTargetPosX = subTable.getDoubleTopic("targetPixelsX").publish();
        bestTargetPosY = subTable.getDoubleTopic("targetPixelsY").publish();
    }

    public void updateCameraNickname(String newCameraNickname) {
        removeEntries();
        subTable = rootTable.getSubTable(newCameraNickname);
        updateEntries();
    }

    @Override
    public void accept(CVPipelineResult result) {
        var simplified =
                new PhotonPipelineResult(
                        result.getLatencyMillis(), simpleFromTrackedTargets(result.targets));
        Packet packet = new Packet(simplified.getPacketSize());
        simplified.populatePacket(packet);

        rawBytesEntry.set(packet.getData());

        pipelineIndexPublisher.set(pipelineIndexSupplier.get());
        driverModePublisher.set(driverModeSupplier.getAsBoolean());
        latencyMillisEntry.set(result.getLatencyMillis());
        hasTargetEntry.set(result.hasTargets());

        if (result.hasTargets()) {
            var bestTarget = result.targets.get(0);

            targetPitchEntry.set(bestTarget.getPitch());
            targetYawEntry.set(bestTarget.getYaw());
            targetAreaEntry.set(bestTarget.getArea());
            targetSkewEntry.set(bestTarget.getSkew());

            var pose = bestTarget.getBestCameraToTarget3d();
            targetPoseEntry.set(
                    new double[] {
                        pose.getTranslation().getX(),
                        pose.getTranslation().getY(),
                        pose.getTranslation().getZ(),
                        pose.getRotation().getQuaternion().getW(),
                        pose.getRotation().getQuaternion().getX(),
                        pose.getRotation().getQuaternion().getY(),
                        pose.getRotation().getQuaternion().getZ()
                    });

            var targetOffsetPoint = bestTarget.getTargetOffsetPoint();
            bestTargetPosX.set(targetOffsetPoint.x);
            bestTargetPosY.set(targetOffsetPoint.y);
        } else {
            targetPitchEntry.set(0);
            targetYawEntry.set(0);
            targetAreaEntry.set(0);
            targetSkewEntry.set(0);
            targetPoseEntry.set(new double[] {0, 0, 0});
            bestTargetPosX.set(0);
            bestTargetPosY.set(0);
        }
        rootTable.getInstance().flush();
    }

    public static List<PhotonTrackedTarget> simpleFromTrackedTargets(List<TrackedTarget> targets) {
        var ret = new ArrayList<PhotonTrackedTarget>();
        for (var t : targets) {
            var points = new Point[4];
            t.getMinAreaRect().points(points);
            var cornerList = new ArrayList<TargetCorner>();

            for (int i = 0; i < 4; i++) cornerList.add(new TargetCorner(points[i].x, points[i].y));

            ret.add(
                    new PhotonTrackedTarget(
                            t.getYaw(),
                            t.getPitch(),
                            t.getArea(),
                            t.getSkew(),
                            t.getFiducialId(),
                            t.getBestCameraToTarget3d(),
                            t.getAltCameraToTarget3d(),
                            t.getPoseAmbiguity(),
                            cornerList));
        }
        return ret;
    }
}
