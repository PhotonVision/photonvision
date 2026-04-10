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

#include <wpi/math/interpolation/TimeInterpolatableBuffer.hpp>
#include <wpi/smartdashboard/Field2d.hpp>
#include <wpi/smartdashboard/FieldObject2d.hpp>
#include <wpi/smartdashboard/SmartDashboard.hpp>
#include <wpi/system/Timer.hpp>

#include "photon/simulation/PhotonCameraSim.h"

namespace photon {
/**
 * A simulated vision system involving a camera(s) and coprocessor(s) mounted on
 * a mobile robot running PhotonVision, detecting targets placed on the field.
 * VisionTargetSims added to this class will be detected by the PhotonCameraSim
 * added to this class. This class should be updated periodically with the
 * robot's current pose in order to publish the simulated camera target info.
 */
class VisionSystemSim {
 public:
  /**
   * A simulated vision system involving a camera(s) and coprocessor(s) mounted
   * on a mobile robot running PhotonVision, detecting targets placed on the
   * field. VisionTargetSims added to this class will be detected by the
   * PhotonCameraSims added to this class. This class should be updated
   * periodically with the robot's current pose in order to publish the
   * simulated camera target info.
   *
   * @param visionSystemName The specific identifier for this vision system in
   * NetworkTables.
   */
  explicit VisionSystemSim(std::string visionSystemName) {
    std::string tableName = "VisionSystemSim-" + visionSystemName;
    wpi::SmartDashboard::PutData(tableName + "/Sim Field", &dbgField);
  }

  /** Get one of the simulated cameras. */
  std::optional<PhotonCameraSim*> GetCameraSim(std::string name) {
    auto it = camSimMap.find(name);
    if (it != camSimMap.end()) {
      return std::make_optional(it->second);
    } else {
      return std::nullopt;
    }
  }

  /** Get all the simulated cameras. */
  std::vector<PhotonCameraSim*> GetCameraSims() {
    std::vector<PhotonCameraSim*> retVal;
    for (auto const& cam : camSimMap) {
      retVal.emplace_back(cam.second);
    }
    return retVal;
  }

  /**
   * Adds a simulated camera to this vision system with a specified
   * robot-to-camera transformation. The vision targets registered with this
   * vision system simulation will be observed by the simulated PhotonCamera.
   *
   * @param cameraSim The camera simulation
   * @param robotToCamera The transform from the robot pose to the camera pose
   */
  void AddCamera(PhotonCameraSim* cameraSim,
                 const wpi::math::Transform3d& robotToCamera) {
    auto found =
        camSimMap.find(std::string{cameraSim->GetCamera()->GetCameraName()});
    if (found == camSimMap.end()) {
      camSimMap[std::string{cameraSim->GetCamera()->GetCameraName()}] =
          cameraSim;
      camTrfMap.insert(
          std::make_pair(std::move(cameraSim),
                         wpi::math::TimeInterpolatableBuffer<wpi::math::Pose3d>{
                             bufferLength}));
      camTrfMap.at(cameraSim).AddSample(wpi::Timer::GetFPGATimestamp(),
                                        wpi::math::Pose3d{} + robotToCamera);
    }
  }

  /** Remove all simulated cameras from this vision system. */
  void ClearCameras() {
    camSimMap.clear();
    camTrfMap.clear();
  }

  /**
   * Remove a simulated camera from this vision system.
   *
   * @param cameraSim The camera to remove
   * @return If the camera was present and removed
   */
  bool RemoveCamera(PhotonCameraSim* cameraSim) {
    int numOfElementsRemoved =
        camSimMap.erase(std::string{cameraSim->GetCamera()->GetCameraName()});
    if (numOfElementsRemoved == 1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get a simulated camera's position relative to the robot. If the requested
   * camera is invalid, an empty optional is returned.
   *
   * @param cameraSim The specific camera to get the robot-to-camera transform
   * of
   * @return The transform of this camera, or an empty optional if it is invalid
   */
  std::optional<wpi::math::Transform3d> GetRobotToCamera(
      PhotonCameraSim* cameraSim) {
    return GetRobotToCamera(cameraSim, wpi::Timer::GetFPGATimestamp());
  }

  /**
   * Get a simulated camera's position relative to the robot. If the requested
   * camera is invalid, an empty optional is returned.
   *
   * @param cameraSim The specific camera to get the robot-to-camera transform
   * of
   * @param time Timestamp of when the transform should be observed
   * @return The transform of this camera, or an empty optional if it is invalid
   */
  std::optional<wpi::math::Transform3d> GetRobotToCamera(
      PhotonCameraSim* cameraSim, wpi::units::second_t time) {
    if (camTrfMap.find(cameraSim) != camTrfMap.end()) {
      wpi::math::TimeInterpolatableBuffer<wpi::math::Pose3d> trfBuffer =
          camTrfMap.at(cameraSim);
      std::optional<wpi::math::Pose3d> sample = trfBuffer.Sample(time);
      if (!sample) {
        return std::nullopt;
      } else {
        return std::make_optional(wpi::math::Transform3d{
            wpi::math::Pose3d{}, sample.value_or(wpi::math::Pose3d{})});
      }
    } else {
      return std::nullopt;
    }
  }

  /**
   * Get a simulated camera's position on the field. If the requested camera is
   * invalid, an empty optional is returned.
   *
   * @param cameraSim The specific camera to get the field pose of
   * @return The pose of this camera, or an empty optional if it is invalid
   */
  std::optional<wpi::math::Pose3d> GetCameraPose(PhotonCameraSim* cameraSim) {
    return GetCameraPose(cameraSim, wpi::Timer::GetFPGATimestamp());
  }

  /**
   * Get a simulated camera's position on the field. If the requested camera is
   * invalid, an empty optional is returned.
   *
   * @param cameraSim The specific camera to get the field pose of
   * @param time Timestamp of when the pose should be observed
   * @return The pose of this camera, or an empty optional if it is invalid
   */
  std::optional<wpi::math::Pose3d> GetCameraPose(PhotonCameraSim* cameraSim,
                                                 wpi::units::second_t time) {
    auto robotToCamera = GetRobotToCamera(cameraSim, time);
    if (!robotToCamera) {
      return std::nullopt;
    } else {
      return std::make_optional(GetRobotPose(time) + robotToCamera.value());
    }
  }

  /**
   * Adjust a camera's position relative to the robot. Use this if your camera
   * is on a gimbal or turret or some other mobile platform.
   *
   * @param cameraSim The simulated camera to change the relative position of
   * @param robotToCamera New transform from the robot to the camera
   * @return If the cameraSim was valid and transform was adjusted
   */
  bool AdjustCamera(PhotonCameraSim* cameraSim,
                    const wpi::math::Transform3d& robotToCamera) {
    if (camTrfMap.find(cameraSim) != camTrfMap.end()) {
      camTrfMap.at(cameraSim).AddSample(wpi::Timer::GetFPGATimestamp(),
                                        wpi::math::Pose3d{} + robotToCamera);
      return true;
    } else {
      return false;
    }
  }

  /** Reset the previous transforms for all cameras to their current transform.
   */
  void ResetCameraTransforms() {
    for (const auto& pair : camTrfMap) {
      ResetCameraTransforms(pair.first);
    }
  }

  /**
   * Reset the transform history for this camera to just the current transform.
   *
   * @param cameraSim The camera to reset
   * @return If the cameraSim was valid and transforms were reset
   */
  bool ResetCameraTransforms(PhotonCameraSim* cameraSim) {
    wpi::units::second_t now = wpi::Timer::GetFPGATimestamp();
    if (camTrfMap.find(cameraSim) != camTrfMap.end()) {
      auto trfBuffer = camTrfMap.at(cameraSim);
      wpi::math::Transform3d lastTrf{
          wpi::math::Pose3d{},
          trfBuffer.Sample(now).value_or(wpi::math::Pose3d{})};
      trfBuffer.Clear();
      AdjustCamera(cameraSim, lastTrf);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns all the vision targets on the field.
   *
   * @return The vision targets
   */
  std::vector<VisionTargetSim> GetVisionTargets() {
    std::vector<VisionTargetSim> all{};
    for (const auto& entry : targetSets) {
      for (const auto& target : entry.second) {
        all.emplace_back(target);
      }
    }
    return all;
  }

  /**
   * Returns all the vision targets of the specified type on the field.
   *
   * @param type The type of vision targets to return
   * @return The vision targets
   */
  std::vector<VisionTargetSim> GetVisionTargets(std::string type) {
    return targetSets[type];
  }

  /**
   * Adds targets on the field which your vision system is designed to detect.
   * The PhotonCameras simulated from this system will report the location of
   * the camera relative to the subset of these targets which are visible from
   * the given camera position.
   *
   * By default these are added under the type "targets".
   *
   * @param targets Targets to add to the simulated field
   */
  void AddVisionTargets(const std::vector<VisionTargetSim>& targets) {
    AddVisionTargets("targets", targets);
  }

  /**
   * Adds targets on the field which your vision system is designed to detect.
   * The PhotonCameras simulated from this system will report the location of
   * the camera relative to the subset of these targets which are visible from
   * the given camera position.
   *
   * @param type Type of target (e.g. "cargo").
   * @param targets Targets to add to the simulated field
   */
  void AddVisionTargets(std::string type,
                        const std::vector<VisionTargetSim>& targets) {
    if (!targetSets.contains(type)) {
      targetSets[type] = std::vector<VisionTargetSim>{};
    }
    for (const auto& tgt : targets) {
      targetSets[type].emplace_back(tgt);
    }
  }

  /**
   * Adds targets on the field which your vision system is designed to detect.
   * The PhotonCameras simulated from this system will report the location of
   * the camera relative to the subset of these targets which are visible from
   * the given camera position.
   *
   * The AprilTags from this layout will be added as vision targets under the
   * type "apriltag". The poses added preserve the tag layout's current alliance
   * origin. If the tag layout's alliance origin is changed, these added tags
   * will have to be cleared and re-added.
   *
   * @param layout The field tag layout to get Apriltag poses and IDs from
   */
  void AddAprilTags(const wpi::apriltag::AprilTagFieldLayout& layout) {
    std::vector<VisionTargetSim> targets;
    for (const wpi::apriltag::AprilTag& tag : layout.GetTags()) {
      targets.emplace_back(VisionTargetSim{layout.GetTagPose(tag.ID).value(),
                                           photon::kAprilTag36h11, tag.ID});
    }
    AddVisionTargets("apriltag", targets);
  }
  /** Removes every VisionTargetSim from the simulated field. */
  void ClearVisionTargets() { targetSets.clear(); }
  /** Removes all simulated AprilTag targets from the simulated field. */
  void ClearAprilTags() { RemoveVisionTargets("apriltag"); }

  /**
   * Removes every VisionTargetSim of the specified type from the simulated
   * field.
   *
   * @param type Type of target (e.g. "cargo"). Same as the type passed into
   *  #addVisionTargets(String, VisionTargetSim...)
   * @return The removed targets, or null if no targets of the specified type
   * exist
   */
  void RemoveVisionTargets(std::string type) { targetSets.erase(type); }

  /**
   * Removes the specified VisionTargetSims from the simulated field.
   *
   * @param targets The targets to remove
   * @return The targets that were actually removed
   */
  std::vector<VisionTargetSim> RemoveVisionTargets(
      const std::vector<VisionTargetSim>& targets) {
    std::vector<VisionTargetSim> removedList;
    for (auto& entry : targetSets) {
      auto& vec = entry.second;
      auto it = vec.begin();
      while (it != vec.end()) {
        if (std::find(targets.begin(), targets.end(), *it) != targets.end()) {
          removedList.emplace_back(*it);
          it = vec.erase(it);
        } else {
          ++it;
        }
      }
    }
    return removedList;
  }

  /**
   * Get the latest robot pose in meters saved by the vision system.
   *
   * @return The latest robot pose
   */
  wpi::math::Pose3d GetRobotPose() {
    return GetRobotPose(wpi::Timer::GetFPGATimestamp());
  }

  /**
   * Get the robot pose in meters saved by the vision system at this timestamp.
   *
   * @param timestamp Timestamp of the desired robot pose
   * @return The robot pose
   */
  wpi::math::Pose3d GetRobotPose(wpi::units::second_t timestamp) {
    return robotPoseBuffer.Sample(timestamp).value_or(wpi::math::Pose3d{});
  }

  /**
   * Clears all previous robot poses and sets robotPose at current time.
   *
   * @param robotPose The robot pose
   */
  void ResetRobotPose(const wpi::math::Pose2d& robotPose) {
    ResetRobotPose(wpi::math::Pose3d{robotPose});
  }

  /**
   * Clears all previous robot poses and sets robotPose at current time.
   *
   * @param robotPose The robot pose
   */
  void ResetRobotPose(const wpi::math::Pose3d& robotPose) {
    robotPoseBuffer.Clear();
    robotPoseBuffer.AddSample(wpi::Timer::GetFPGATimestamp(), robotPose);
  }
  wpi::Field2d& GetDebugField() { return dbgField; }

  /**
   * Periodic update. Ensure this is called repeatedly-- camera performance is
   * used to automatically determine if a new frame should be submitted.
   *
   * @param robotPoseMeters The simulated robot pose in meters
   */
  void Update(const wpi::math::Pose2d& robotPose) {
    Update(wpi::math::Pose3d{robotPose});
  }

  /**
   * Periodic update. Ensure this is called repeatedly-- camera performance is
   * used to automatically determine if a new frame should be submitted.
   *
   * @param robotPoseMeters The simulated robot pose in meters
   */
  void Update(const wpi::math::Pose3d& robotPose) {
    for (auto& set : targetSets) {
      std::vector<wpi::math::Pose2d> posesToAdd{};
      for (auto& target : set.second) {
        posesToAdd.emplace_back(target.GetPose().ToPose2d());
      }
      dbgField.GetObject(set.first)->SetPoses(posesToAdd);
    }

    wpi::units::second_t now = wpi::Timer::GetFPGATimestamp();
    robotPoseBuffer.AddSample(now, robotPose);
    dbgField.SetRobotPose(robotPose.ToPose2d());

    std::vector<VisionTargetSim> allTargets{};
    for (const auto& set : targetSets) {
      for (const auto& target : set.second) {
        allTargets.emplace_back(target);
      }
    }

    std::vector<wpi::math::Pose2d> visTgtPoses2d{};
    std::vector<wpi::math::Pose2d> cameraPoses2d{};
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
      wpi::units::second_t latency = camSim->prop.EstLatency();
      wpi::units::second_t timestampCapture =
          wpi::units::microsecond_t{static_cast<double>(timestampNt)} - latency;

      wpi::math::Pose3d lateRobotPose = GetRobotPose(timestampCapture);
      wpi::math::Pose3d lateCameraPose =
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
  static constexpr wpi::units::second_t bufferLength{1.5_s};
  std::unordered_map<PhotonCameraSim*,
                     wpi::math::TimeInterpolatableBuffer<wpi::math::Pose3d>>
      camTrfMap;
  wpi::math::TimeInterpolatableBuffer<wpi::math::Pose3d> robotPoseBuffer{
      bufferLength};
  std::unordered_map<std::string, std::vector<VisionTargetSim>> targetSets{};
  wpi::Field2d dbgField{};
  const wpi::math::Transform3d kEmptyTrf{};
};
}  // namespace photon
