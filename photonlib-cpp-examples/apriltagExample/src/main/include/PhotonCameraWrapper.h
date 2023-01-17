#pragma once

#include <photonlib/PhotonCamera.h>
#include <frc/apriltag/AprilTagFieldLayout.h>

class PhotonCameraWrapper {

public:

  // Change this to match the name of your camera
  photonlib::PhotonCamera m_camera{"photonvision"};

  std::vector<frc::AprilTag> m_tagList = {
      {0, frc::Pose3d(units::meter_t(3), units::meter_t(3), units::meter_t(3),
                      frc::Rotation3d())},
      {1, frc::Pose3d(units::meter_t(5), units::meter_t(5), units::meter_t(5),
                      frc::Rotation3d())}};

  frc::AprilTagFieldLayout m_fieldLayout {m_tagList, 54_ft, 27_ft};

};
