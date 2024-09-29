import photonlibpy as pp

camera = pp.PhotonCamera("camera")

simcam = pp.PhotonCameraSim(camera)

simsystem = pp.VisionSystemSim("Test")

simsystem.AddCamera(simcam, pp.Transform3d())
simsystem.AddVisionTargets(
    "apriltag", [pp.VisionTargetSim(pp.Pose3d(), pp.TargetModel.kAprilTag36h11)]
)
