// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.wpilibj.smartdashboard;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Quaternion;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.networktables.DoubleArrayEntry;
import java.util.ArrayList;
import java.util.List;

/** Game field object on a Field3d. */
public class FieldObject3d implements AutoCloseable {
  /**
   * Package-local constructor.
   *
   * @param name name
   */
  FieldObject3d(String name) {
    m_name = name;
  }

  @Override
  public void close() {
    m_entry.close();
  }

  /**
   * Set the pose from a Pose object.
   *
   * @param pose 3d pose
   */
  public synchronized void setPose(Pose3d pose) {
    setPoses(pose);
  }

  /**
   * Set the pose from x, y, and rotation.
   *
   * @param xMeters X location, in meters
   * @param yMeters Y location, in meters
   * @param rotation rotation
   */
  public synchronized void setPose(double xMeters, double yMeters, double zMeters, Rotation3d rotation) {
    setPose(new Pose3d(xMeters, yMeters, zMeters, rotation));
  }

  /**
   * Get the pose.
   *
   * @return 3d pose
   */
  public synchronized Pose3d getPose() {
    updateFromEntry();
    if (m_poses.isEmpty()) {
      return new Pose3d();
    }
    return m_poses.get(0);
  }

  /**
   * Set multiple poses from an list of Pose objects. The total number of poses is limited to 85.
   *
   * @param poses list of 3d poses
   */
  public synchronized void setPoses(List<Pose3d> poses) {
    m_poses.clear();
    for (Pose3d pose : poses) {
      m_poses.add(pose);
    }
    updateEntry();
  }

  /**
   * Set multiple poses from an list of Pose objects. The total number of poses is limited to 85.
   *
   * @param poses list of 3d poses
   */
  public synchronized void setPoses(Pose3d... poses) {
    m_poses.clear();
    for (Pose3d pose : poses) {
      m_poses.add(pose);
    }
    updateEntry();
  }

  /**
   * Sets poses from a trajectory.
   *
   * @param trajectory The trajectory from which the poses should be added.
   */
  public synchronized void setTrajectory(Trajectory trajectory) {
    m_poses.clear();
    for (Trajectory.State state : trajectory.getStates()) {
      m_poses.add(new Pose3d(state.poseMeters));
    }
    updateEntry();
  }

  /**
   * Get multiple poses.
   *
   * @return list of 3d poses
   */
  public synchronized List<Pose3d> getPoses() {
    updateFromEntry();
    return new ArrayList<Pose3d>(m_poses);
  }

  void updateEntry() {
    updateEntry(false);
  }

  synchronized void updateEntry(boolean setDefault) {
    if (m_entry == null) {
      return;
    }

    double[] arr = new double[m_poses.size() * 7];
    int ndx = 0;
    for (Pose3d pose : m_poses) {
      Translation3d translation = pose.getTranslation();
      Quaternion quat = pose.getRotation().getQuaternion();
      arr[ndx + 0] = translation.getX();
      arr[ndx + 1] = translation.getY();
      arr[ndx + 2] = translation.getZ();
      arr[ndx + 3] = quat.getW();
      arr[ndx + 4] = quat.getX();
      arr[ndx + 5] = quat.getY();
      arr[ndx + 6] = quat.getZ();
      ndx += 7;
    }

    if (setDefault) {
      m_entry.setDefault(arr);
    } else {
      m_entry.set(arr);
    }
  }

  private synchronized void updateFromEntry() {
    if (m_entry == null) {
      return;
    }

    double[] arr = m_entry.get((double[]) null);
    if (arr != null) {
      if ((arr.length % 7) != 0) {
        return;
      }

      m_poses.clear();
      for (int i = 0; i < arr.length; i += 7) {
        m_poses.add(
          new Pose3d(
            arr[i], arr[i + 1], arr[i+2],
            new Rotation3d(
              new Quaternion(arr[i+3],arr[i+4],arr[i+5],arr[i+6])
            )
          )
        );
      }
    }
  }

  String m_name;
  DoubleArrayEntry m_entry;
  private final List<Pose3d> m_poses = new ArrayList<>();
}
