import time
import photonlibpy as pp
from wpimath.geometry import *
from robotpy_apriltag import *

camera = pp.PhotonCamera("camera")

simcam = pp.PhotonCameraSim(camera)
simcam.EnableDrawWireframe(True)

simsystem = pp.VisionSystemSim("Test")

simsystem.AddCamera(simcam, Transform3d(0, 0, 1, Rotation3d()))

simsystem.AddAprilTags(AprilTagFieldLayout.loadField(AprilTagField.k2024Crescendo))

start = time.time()
for i in range(10):
    simsystem.Update(
        Pose3d(Translation3d(8, 6, 0), Rotation3d(0, 0, time.time() - start))
    )
    time.sleep(0.02)
