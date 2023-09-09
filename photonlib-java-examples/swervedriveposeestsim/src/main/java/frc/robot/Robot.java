// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

public class Robot extends TimedRobot {
    
    @Override
    public void robotInit() {}
    
    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();
    }
    
    @Override
    public void disabledInit() {}
    
    @Override
    public void disabledPeriodic() {}
    
    @Override
    public void disabledExit() {}
    
    @Override
    public void autonomousInit() {}
    
    @Override
    public void autonomousPeriodic() {}
    
    @Override
    public void autonomousExit() {}
    
    @Override
    public void teleopInit() {}
    
    @Override
    public void teleopPeriodic() {}
    
    @Override
    public void teleopExit() {}
    
    @Override
    public void testInit() {}
    
    @Override
    public void testPeriodic() {}
    
    @Override
    public void testExit() {}
}
