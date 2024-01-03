/*
 * MIT License
 *
 * Copyright (c) PhotonVision
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

#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

#include <frc/Timer.h>
#include <frc/interpolation/TimeInterpolatableBuffer.h>
#include <frc/smartdashboard/Field2d.h>
#include <frc/smartdashboard/FieldObject2d.h>
#include <frc/smartdashboard/SmartDashboard.h>

#include "photon/simulation/PhotonCameraSim.h"

namespace photon {
class VisionSystemSim {
 public:
  explicit VisionSystemSim(std::string visionSystemName) {
    std::string tableName = "VisionSystemSim-" + visionSystemName;
    frc::SmartDashboard::PutData(tableName + "/Sim Field", &dbgField);
  }
  std::optional<PhotonCameraSim*> GetCameraSim(std::string name) {
    auto it = camSimMap.find(name);
    if (it != camSimMap.end()) {
      return std::make_optional(it->second);
    } else {
      return std::nullopt;
    }
  }
  std::vector<PhotonCameraSim*> GetCameraSims() {
    std::vector<PhotonCameraSim*> retVal;
    for (auto const& cam : camSimMap) {
      retVal.emplace_back(cam.second);
    }
    return retVal;
  }
  void AddCamera(PhotonCameraSim* cameraSim,
                 const frc::Transform3d& robotToCamera) {
    auto found =
        camSimMap.find(std::string{cameraSim->GetCamera()->GetCameraName()});
    if (found == camSimMap.end()) {
      camSimMap[std::string{cameraSim->GetCamera()->GetCameraName()}] =
          cameraSim;
      camTrfMap.insert(std::make_pair(
          std::move(cameraSim),
          frc::TimeInterpolatableBuffer<frc::Pose3d>{bufferLength}));
      camTrfMap.at(cameraSim).AddSample(frc::Timer::GetFPGATimestamp(),
                                        frc::Pose3d{} + robotToCamera);
    }
  }
  void ClearCameras() {
    camSimMap.clear();
    camTrfMap.clear();
  }
  bool RemoveCamera(PhotonCameraSim* cameraSim) {
    int numOfElementsRemoved =
        camSimMap.erase(std::string{cameraSim->GetCamera()->GetCameraName()});
    if (numOfElementsRemoved == 1) {
      return true;
    } else {
      return false;
    }
  }
  std::optional<frc::Transform3d> GetRobotToCamera(PhotonCameraSim* cameraSim) {
    return GetRobotToCamera(cameraSim, frc::Timer::GetFPGATimestamp());
  }
  std::optional<frc::Transform3d> GetRobotToCamera(PhotonCameraSim* cameraSim,
                                                   units::second_t time) {
    if (camTrfMap.find(cameraSim) != camTrfMap.end()) {
      frc::TimeInterpolatableBuffer<frc::Pose3d> trfBuffer =
          camTrfMap.at(cameraSim);
      std::optional<frc::Pose3d> sample = trfBuffer.Sample(time);
      if (!sample) {
        return std::nullopt;
      } else {
        return std::make_optional(
            frc::Transform3d{frc::Pose3d{}, sample.value_or(frc::Pose3d{})});
      }
    } else {
      return std::nullopt;
    }
  }
  std::optional<frc::Pose3d> GetCameraPose(PhotonCameraSim* cameraSim) {
    return GetCameraPose(cameraSim, frc::Timer::GetFPGATimestamp());
  }
  std::optional<frc::Pose3d> GetCameraPose(PhotonCameraSim* cameraSim,
                                           units::second_t time) {
    auto robotToCamera = GetRobotToCamera(cameraSim, time);
    if (!robotToCamera) {
      return std::nullopt;
    } else {
      return std::make_optional(GetRobotPose(time) + robotToCamera.value());
    }
  }
  bool AdjustCamera(PhotonCameraSim* cameraSim,
                    const frc::Transform3d& robotToCamera) {
    if (camTrfMap.find(cameraSim) != camTrfMap.end()) {
      camTrfMap.at(cameraSim).AddSample(frc::Timer::GetFPGATimestamp(),
                                        frc::Pose3d{} + robotToCamera);
      return true;
    } else {
      return false;
    }
  }
  void ResetCameraTransforms() {
    for (const auto& pair : camTrfMap) {
      ResetCameraTransforms(pair.first);
    }
  }
  bool ResetCameraTransforms(PhotonCameraSim* cameraSim) {
    units::second_t now = frc::Timer::GetFPGATimestamp();
    if (camTrfMap.find(cameraSim) != camTrfMap.end()) {
      auto trfBuffer = camTrfMap.at(cameraSim);
      frc::Transform3d lastTrf{frc::Pose3d{},
                               trfBuffer.Sample(now).value_or(frc::Pose3d{})};
      trfBuffer.Clear();
      AdjustCamera(cameraSim, lastTrf);
      return true;
    } else {
      return false;
    }
  }
  std::vector<VisionTargetSim> GetVisionTargets() {
    std::vector<VisionTargetSim> all{};
    for (const auto& entry : targetSets) {
      for (const auto& target : entry.second) {
        all.emplace_back(target);
      }
    }
    return all;
  }
  std::vector<VisionTargetSim> GetVisionTargets(std::string type) {
    return targetSets[type];
  }
  void AddVisionTargets(const std::vector<VisionTargetSim>& targets) {
    AddVisionTargets("targets", targets);
  }
  void AddVisionTargets(std::string type,
                        const std::vector<VisionTargetSim>& targets) {
    if (!targetSets.contains(type)) {
      targetSets[type] = std::vector<VisionTargetSim>{};
    }
    for (const auto& tgt : targets) {
      targetSets[type].emplace_back(tgt);
    }
  }
  void AddAprilTags(const frc::AprilTagFieldLayout& layout) {
    std::vector<VisionTargetSim> targets;
    for (const frc::AprilTag& tag : layout.GetTags()) {
      targets.emplace_back(VisionTargetSim{layout.GetTagPose(tag.ID).value(),
                                           photon::kAprilTag36h11, tag.ID});
    }
    AddVisionTargets("apriltag", targets);
  }
  void ClearVisionTargets() { targetSets.clear(); }
  void ClearAprilTags() { RemoveVisionTargets("apriltag"); }
  void RemoveVisionTargets(std::string type) { targetSets.erase(type); }
  std::vector<VisionTargetSim> RemoveVisionTargets(
      const std::vector<VisionTargetSim>& targets) {
    std::vector<VisionTargetSim> removedList;
    for (auto& entry : targetSets) {
      for (auto target : entry.second) {
        auto it = std::find(targets.begin(), targets.end(), target);
        if (it != targets.end()) {
          removedList.emplace_back(target);
          entry.second.erase(it);
        }
      }
    }
    return removedList;
  }
  frc::Pose3d GetRobotPose() {
    return GetRobotPose(frc::Timer::GetFPGATimestamp());
  }
  frc::Pose3d GetRobotPose(units::second_t timestamp) {
    return robotPoseBuffer.Sample(timestamp).value_or(frc::Pose3d{});
  }
  void ResetRobotPose(const frc::Pose2d& robotPose) {
    ResetRobotPose(frc::Pose3d{robotPose});
  }
  void ResetRobotPose(const frc::Pose3d& robotPose) {
    robotPoseBuffer.Clear();
    robotPoseBuffer.AddSample(frc::Timer::GetFPGATimestamp(), robotPose);
  }
  frc::Field2d& GetDebugField() { return dbgField; }
  void Update(const frc::Pose2d& robotPose) { Update(frc::Pose3d{robotPose}); }
  void Update(const frc::Pose3d& robotPose) {
    for (auto& set : targetSets) {
      std::vector<frc::Pose2d> posesToAdd{};
      for (auto& target : set.second) {
        posesToAdd.emplace_back(target.GetPose().ToPose2d());
      }
      dbgField.GetObject(set.first)->SetPoses(posesToAdd);
    }

    units::second_t now = frc::Timer::GetFPGATimestamp();
    robotPoseBuffer.AddSample(now, robotPose);
    dbgField.SetRobotPose(robotPose.ToPose2d());

    std::vector<VisionTargetSim> allTargets{};
    for (const auto& set : targetSets) {
      for (const auto& target : set.second) {
        allTargets.emplace_back(target);
      }
    }

    std::vector<frc::Pose2d> visTgtPoses2d{};
    std::vector<frc::Pose2d> cameraPoses2d{};
    bool processed{false};
    for (const auto& entry : camSimMap) {
      auto camSim = entry.second;
      auto optTimestamp = camSim->ConsumeNextEntryTime();
      if (!optTimestamp) {
        continue;
      } else {
        processed = true;
      }
      uint64_t timestampNt = optTimestamp.value();
      units::second_t latency = camSim->prop.EstLatency();
      units::second_t timestampCapture =
          units::microsecond_t{static_cast<double>(timestampNt)} - latency;

      frc::Pose3d lateRobotPose = GetRobotPose(timestampCapture);
      frc::Pose3d lateCameraPose =
          lateRobotPose + GetRobotToCamera(camSim, timestampCapture).value();
      cameraPoses2d.push_back(lateCameraPose.ToPose2d());

      auto camResult = camSim->Process(latency, lateCameraPose, allTargets);
      camSim->SubmitProcessedFrame(camResult, timestampNt);
      for (const auto& target : camResult.GetTargets()) {
        auto trf = target.GetBestCameraToTarget();
        if (trf == kEmptyTrf) {
          continue;
        }
        visTgtPoses2d.push_back(lateCameraPose.TransformBy(trf).ToPose2d());
      }
    }
    if (processed) {
      dbgField.GetObject("visibleTargetPoses")->SetPoses(visTgtPoses2d);
    }
    if (cameraPoses2d.size() != 0) {
      dbgField.GetObject("cameras")->SetPoses(cameraPoses2d);
    }
  }

 private:
  std::unordered_map<std::string, PhotonCameraSim*> camSimMap{};
  static constexpr units::second_t bufferLength{1.5_s};
  std::unordered_map<PhotonCameraSim*,
                     frc::TimeInterpolatableBuffer<frc::Pose3d>>
      camTrfMap;
  frc::TimeInterpolatableBuffer<frc::Pose3d> robotPoseBuffer{bufferLength};
  std::unordered_map<std::string, std::vector<VisionTargetSim>> targetSets{};
  frc::Field2d dbgField{};
  const frc::Transform3d kEmptyTrf{};
};
}  // namespace photon
