// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.List;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj.smartdashboard.Field3d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Robot extends TimedRobot {
  private final XboxController m_controller = new XboxController(0);
  private final Field2d field = new Field2d();
  private Pose2d pose = new Pose2d();
  private final Field3d field3d = new Field3d();

  @Override
  public void robotInit() {
    // TODO Auto-generated method stub
    super.robotInit();
    // NetworkTableInstance.getDefault().stopClient();
    // NetworkTableInstance.getDefault().startClient("gloworm.local", 1735);
    field3d.getObject("target").setPoses(List.of(new Pose3d(1, 1, 1, new Rotation3d()), new Pose3d(1, 2, 1, new Rotation3d())));   
    SmartDashboard.putData(field);
    SmartDashboard.putData(field3d);
  }
  @Override
  public void robotPeriodic() {

  }
  @Override
  public void teleopPeriodic() {

    ChassisSpeeds speeds = driveWithJoystick();
    
    var newPose =
    pose.exp(
        new Twist2d(
            speeds.vxMetersPerSecond * 0.02,
            speeds.vyMetersPerSecond * 0.02,
            speeds.omegaRadiansPerSecond * 0.02));
    pose = newPose;

    field.setRobotPose(pose);
    field3d.setRobotPose(
      new Pose3d(pose)
      );
    
    
  }

  private ChassisSpeeds driveWithJoystick() {
    // Get the x speed. We are inverting this because Xbox controllers return
    // negative values when we push forward.
    final var xSpeed =
        -m_controller.getLeftY() * 5;
          

    // Get the y speed or sideways/strafe speed. We are inverting this because
    // we want a positive value when we pull to the left. Xbox controllers
    // return positive values when you pull to the right by default.
    final var ySpeed =
        -m_controller.getLeftX() * 5;

    // Get the rate of angular rotation. We are inverting this because we want a
    // positive value when we pull to the left (remember, CCW is positive in
    // mathematics). Xbox controllers return positive values when you pull to
    // the right by default.
    final var rot =
        -m_controller.getRightX()*Math.PI * 4;

    return ChassisSpeeds.fromFieldRelativeSpeeds(xSpeed, ySpeed, rot, pose.getRotation().plus(new Rotation2d(rot*0.01)));
  }
}
