/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#pragma once

#include <photonlib/PhotonCamera.h>
#include <photonlib/PhotonPoseEstimator.h>

#include <utility>

#include <frc/apriltag/AprilTagFieldLayout.h>
#include <frc/apriltag/AprilTagFields.h>

class PhotonCameraWrapper {
 public:
  photonlib::PhotonPoseEstimator m_poseEstimator{
      frc::LoadAprilTagLayoutField(frc::AprilTagField::k2023ChargedUp),
      photonlib::MULTI_TAG_PNP, std::move(photonlib::PhotonCamera{"WPI2023"}),
      frc::Transform3d{}};

  inline std::optional<photonlib::EstimatedRobotPose> Update(
      frc::Pose2d estimatedPose) {
    m_poseEstimator.SetReferencePose(frc::Pose3d(estimatedPose));
    return m_poseEstimator.Update();
  }
};
