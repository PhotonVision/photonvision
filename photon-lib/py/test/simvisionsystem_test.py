import time
import photonlibpy as pp
from wpimath.geometry import *

camera = pp.PhotonCamera("camera")

simcam = pp.PhotonCameraSim(camera)
simcam.EnableDrawWireframe(True)

simsystem = pp.VisionSystemSim("Test")

simsystem.AddCamera(simcam, Transform3d())
simsystem.AddVisionTargets(
    "apriltag", [pp.VisionTargetSim(Pose3d(), pp.TargetModel.kAprilTag36h11)]
)

start = time.time()
while True:
    simsystem.Update(Pose3d(
        Translation3d(2, 0, 0),
        Rotation3d(0, 0, time.time() - start)
    ))
    time.sleep(0.02)
