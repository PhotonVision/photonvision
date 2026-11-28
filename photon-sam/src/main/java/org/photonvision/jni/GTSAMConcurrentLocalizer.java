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

package org.photonvision.jni;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import org.photonvision.estimation.TargetModel;
import org.wpilib.fields.Field;
import org.wpilib.math.geometry.Pose3d;
import org.wpilib.math.geometry.Rotation3d;
import org.wpilib.math.geometry.Transform3d;
import org.wpilib.math.linalg.VecBuilder;
import org.wpilib.math.linalg.Vector;
import org.wpilib.math.numbers.N6;

/*
 * serialization format:
 * Pose3d: [x (m), y (m), z (m), roll (rad), pitch (rad), yaw (rad)]
 */

/** A wrapper around the GTSAM LocalizerCore implemented in C++. */
public class GTSAMConcurrentLocalizer {
    private final long handle;
    private static final Cleaner cleaner = Cleaner.create();
    private final Cleanable cleanable;

    public GTSAMConcurrentLocalizer(Field layout, TargetModel model) {
        var tags = layout.getTags();
        int[] tagIDs = new int[tags.size()];
        double[] tagPoses = new double[tags.size() * 6];
        for (int i = 0; i < tags.size(); i++) {
            tagIDs[i] = tags.get(i).getID();
            tagPoses[i * 6] = tags.get(i).getPose().getX();
            tagPoses[i * 6 + 1] = tags.get(i).getPose().getY();
            tagPoses[i * 6 + 2] = tags.get(i).getPose().getZ();
            tagPoses[i * 6 + 3] = tags.get(i).getPose().getRotation().getX();
            tagPoses[i * 6 + 4] = tags.get(i).getPose().getRotation().getY();
            tagPoses[i * 6 + 5] = tags.get(i).getPose().getRotation().getZ();
        }
        double[] tagCorners = new double[3 * model.vertices.size()];
        for (int i = 0; i < model.vertices.size(); i++) {
            tagCorners[i * 3] = model.vertices.get(i).getX();
            tagCorners[i * 3 + 1] = model.vertices.get(i).getY();
            tagCorners[i * 3 + 2] = model.vertices.get(i).getZ();
        }
        long ptr =
                createJNI(tagIDs, tagPoses, layout.getFieldWidth(), layout.getFieldLength(), tagCorners);
        cleanable = cleaner.register(this, () -> destroyJNI(ptr));
        handle = ptr;
    }

    public void submitReset(Pose3d wTr, GTSAMExtras.NoiseModel noise, long timeUs) {
        SubmitResetJNI(
                handle,
                new double[] {
                    wTr.getX(),
                    wTr.getY(),
                    wTr.getZ(),
                    wTr.getRotation().getX(),
                    wTr.getRotation().getY(),
                    wTr.getRotation().getZ()
                },
                noise.getHandle(),
                timeUs);
    }

    public void step() {
        StepJNI(handle);
    }

    public void submitOdometry(Pose3d poseDelta, GTSAMExtras.NoiseModel odometryNoise, long timeUs) {
        SubmitOdometryJNI(
                handle,
                new double[] {
                    poseDelta.getX(),
                    poseDelta.getY(),
                    poseDelta.getZ(),
                    poseDelta.getRotation().getX(),
                    poseDelta.getRotation().getY(),
                    poseDelta.getRotation().getZ()
                },
                odometryNoise.getHandle(),
                timeUs);
    }

    public long getLatestTimestamp() {
        return GetLatestTimestampJNI(handle);
    }

    public void submitTagObservation(
            long timeUs,
            int tagID,
            double[] corners,
            GTSAMExtras.Cal3S2 cameraCal,
            Transform3d robotTcamera,
            GTSAMExtras.NoiseModel cameraNoise) {
        SubmitTagObservationJNI(
                handle,
                timeUs,
                tagID,
                corners,
                cameraCal.getHandle(),
                new double[] {
                    robotTcamera.getX(),
                    robotTcamera.getY(),
                    robotTcamera.getZ(),
                    robotTcamera.getRotation().getX(),
                    robotTcamera.getRotation().getY(),
                    robotTcamera.getRotation().getZ()
                },
                cameraNoise.getHandle());
    }

    public Pose3d getLatestWorldToBody() {
        double[] pose = GetLatestWorldToBodyJNI(handle);
        return new Pose3d(pose[0], pose[1], pose[2], new Rotation3d(pose[3], pose[4], pose[5]));
    }

    public long getLastOdomTime() {
        return GetLastOdomTimeJNI(handle);
    }

    public Vector<N6> getPoseComponentStdDevs() {
        double[] noise = GetPoseComponentStdDevsJNI(handle);
        return VecBuilder.fill(noise[0], noise[1], noise[2], noise[3], noise[4], noise[5]);
    }

    // LocalizerCore JNI methods
    private static native long createJNI(
            int[] tagIDs, double[] tagPoses, double fieldWidth, double fieldLength, double[] tagCorners);

    private static native void destroyJNI(long LocalizerCore_handle);

    private static native void SubmitResetJNI(
            long LocalizerCore_handle, double[] wTr, long odometryNoise_handle, long timeUs);

    private static native void SubmitOdometryJNI(
            long LocalizerCore_handle, double[] poseDelta, long odometryNoise_handle, long timeUs);

    private static native void SubmitTagObservationJNI(
            long LocalizerCore_handle,
            long timeUs,
            int tagID,
            double[] corners,
            long cameraCal_handle,
            double[] robotTcamera,
            long cameraNoise_handle);

    private static native long GetLatestTimestampJNI(long LocalizerCore_handle);

    private static native void OptimizeJNI(long LocalizerCore_handle);

    private static native void StepJNI(long LocalizerCore_handle);

    private static native double[] GetLatestWorldToBodyJNI(long LocalizerCore_handle);

    private static native long GetLastOdomTimeJNI(long LocalizerCore_handle);

    private static native double[] GetPoseComponentStdDevsJNI(long LocalizerCore_handle);
}
