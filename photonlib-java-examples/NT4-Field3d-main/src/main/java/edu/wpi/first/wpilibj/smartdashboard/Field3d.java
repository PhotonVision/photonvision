// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package edu.wpi.first.wpilibj.smartdashboard;

import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.networktables.NTSendable;
import edu.wpi.first.networktables.NTSendableBuilder;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.util.sendable.SendableRegistry;
import java.util.ArrayList;
import java.util.List;

/**
 * 3d representation of game field for dashboards.
 *
 * <p>An object's pose is the location shown on the dashboard view. Note that for the robot, this
 * may or may not match the internal odometry. For example, the robot is shown at a particular
 * starting location, the pose in this class would represent the actual location on the field, but
 * the robot's internal state might have a 0,0,0 pose (unless it's initialized to something
 * different).
 *
 * <p>As the user is able to edit the pose, code performing updates should get the robot pose,
 * transform it as appropriate (e.g. based on wheel odometry), and set the new pose.
 *
 * <p>This class provides methods to set the robot pose, but other objects can also be shown by
 * using the getObject() function. Other objects can also have multiple poses (which will show the
 * object at multiple locations).
 */
public class Field3d implements NTSendable, AutoCloseable {
  /** Constructor. */
  public Field3d() {
    FieldObject3d obj = new FieldObject3d("Robot");
    obj.setPose(new Pose3d());
    m_objects.add(obj);
    SendableRegistry.add(this, "Field");
  }

  @Override
  public void close() {
    for (FieldObject3d obj : m_objects) {
      obj.close();
    }
  }

  /**
   * Set the robot pose from a Pose object.
   *
   * @param pose 3d pose
   */
  public synchronized void setRobotPose(Pose3d pose) {
    m_objects.get(0).setPose(pose);
  }

  /**
   * Set the robot pose from x, y, and rotation.
   *
   * @param xMeters X location, in meters
   * @param yMeters Y location, in meters
   * @param rotation rotation
   */
  public synchronized void setRobotPose(double xMeters, double yMeters, double zMeters, Rotation3d rotation) {
    m_objects.get(0).setPose(xMeters, yMeters, zMeters, rotation);
  }

  /**
   * Get the robot pose.
   *
   * @return 3d pose
   */
  public synchronized Pose3d getRobotPose() {
    return m_objects.get(0).getPose();
  }

  /**
   * Get or create a field object.
   *
   * @param name The field object's name.
   * @return Field object
   */
  public synchronized FieldObject3d getObject(String name) {
    for (FieldObject3d obj : m_objects) {
      if (obj.m_name.equals(name)) {
        return obj;
      }
    }
    FieldObject3d obj = new FieldObject3d(name);
    m_objects.add(obj);
    if (m_table != null) {
      synchronized (obj) {
        obj.m_entry = m_table.getDoubleArrayTopic(name).getEntry(new double[] {});
      }
    }
    return obj;
  }

  /**
   * Get the robot object.
   *
   * @return Field object for robot
   */
  public synchronized FieldObject3d getRobotObject() {
    return m_objects.get(0);
  }

  @Override
  public void initSendable(NTSendableBuilder builder) {
    builder.setSmartDashboardType("Field3d");

    synchronized (this) {
      m_table = builder.getTable();
      for (FieldObject3d obj : m_objects) {
        synchronized (obj) {
          obj.m_entry = m_table.getDoubleArrayTopic(obj.m_name).getEntry(new double[] {});
          obj.updateEntry(true);
        }
      }
    }
  }

  private NetworkTable m_table;
  private final List<FieldObject3d> m_objects = new ArrayList<>();
}
