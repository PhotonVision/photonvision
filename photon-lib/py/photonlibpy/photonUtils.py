import photonlibpy
from wpimath.geometry import Translation2d, Rotation2d, Pose2d, Transform2d, Pose3d, Transform3d
import math

class PhotonUtils:
    def __init__():
        pass
    
    @staticmethod
    def calculateDistanceToTargetMeters(cameraHeightMeters : float, targetHeightMeters : float, cameraPitchRadians : float, targetPitchRadians : float) -> float:
        return (targetHeightMeters - cameraHeightMeters) / math.tan(cameraPitchRadians + targetPitchRadians)
    
    @staticmethod
    def estimateCameraToTargetTranslation(targetDistanceMeters : float, yaw : Rotation2d):
        return Translation2d(yaw.cos() * targetDistanceMeters, yaw.sin() * targetDistanceMeters)
    
    @staticmethod
    def estimateCameraToTarget(cameraToTargetTranslation : Translation2d, fieldToTarget : Pose2d, gyroAngle : Rotation2d):
        return Transform2d(cameraToTargetTranslation, gyroAngle.__mul__(-1).__sub__(fieldToTarget.rotation()))
    
    @staticmethod
    def estimateFieldToCamera(cameraToTarget : Transform2d, fieldToTarget : Pose2d):
        targetToCamera = cameraToTarget.inverse()
        return fieldToTarget.transformBy(targetToCamera)

    @staticmethod
    def estimateFieldToRobot(cameraToTarget : Transform2d, fieldToTarget : Pose2d, cameraToRobot : Transform2d):
        return PhotonUtils.estimateFieldToCamera(cameraToTarget, fieldToTarget).transformBy(cameraToRobot)
    
    @staticmethod
    def estimateFieldToRobotAprilTag(cameraToTarget : Transform3d, fieldRelativeTagPose : Pose3d, cameraToRobot : Transform3d):
        return fieldRelativeTagPose.__add__(cameraToTarget.inverse()).__add__(cameraToRobot)

    @staticmethod
    def getYawToPose(robotPose : Pose2d, targetPose : Pose2d):
        relativeTr1 = targetPose.relativeTo(robotPose).translation()
        return Rotation2d(relativeTr1.X(), relativeTr1.Y())
    
    @staticmethod
    def getDistanceToPose(robotPose : Pose2d, targetPose : Pose2d):
        return robotPose.translation().distance(targetPose.translation())

    @staticmethod
    def estimateFieldToRobot(cameraHeightMeters : float, targetHeightMeters : float, cameraPitchRadians : float, targetPitchRadians : float, targetYaw : Rotation2d, gyroAngle : Rotation2d, fieldToTarget : Pose2d, cameraToRobot : Transform2d):
        return PhotonUtils.estimateFieldToRobot(PhotonUtils.estimateCameraToTarget(PhotonUtils.estimateCameraToTargetTranslation(PhotonUtils.calculateDistanceToTargetMeters(cameraHeightMeters, targetHeightMeters, cameraPitchRadians, targetPitchRadians), targetYaw), fieldToTarget, gyroAngle), fieldToTarget, cameraToRobot)