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

#include <algorithm>
#include <memory>
#include <string>
#include <vector>

#include <networktables/NetworkTableInstance.h>

#include "photonlib/PhotonCamera.h"
#include "photonlib/PhotonTargetSortMode.h"

namespace photonlib {
class SimPhotonCamera : public PhotonCamera {
 public:
  SimPhotonCamera(nt::NetworkTableInstance instance,
                  const std::string& cameraName)
      : PhotonCamera(instance, cameraName) {
    latencyMillisEntry = rootTable->GetEntry("latencyMillis");
    hasTargetEntry = rootTable->GetEntry("hasTargetEntry");
    targetPitchEntry = rootTable->GetEntry("targetPitchEntry");
    targetYawEntry = rootTable->GetEntry("targetYawEntry");
    targetAreaEntry = rootTable->GetEntry("targetAreaEntry");
    targetSkewEntry = rootTable->GetEntry("targetSkewEntry");
    targetPoseEntry = rootTable->GetEntry("targetPoseEntry");
    rawBytesPublisher = rootTable->GetRawTopic("rawBytes").Publish("rawBytes");
    versionEntry = instance.GetTable("photonvision")->GetEntry("version");
    // versionEntry.SetString(PhotonVersion.versionString);
  }

  explicit SimPhotonCamera(const std::string& cameraName)
      : SimPhotonCamera(nt::NetworkTableInstance::GetDefault(), cameraName) {}

  virtual ~SimPhotonCamera() = default;

  /**
   * Simulate one processed frame of vision data, putting one result to NT.
   *
   * @param latency Latency of the provided frame
   * @param targetList List of targets detected
   */
  void SubmitProcessedFrame(units::millisecond_t latency,
                            std::vector<PhotonTrackedTarget> targetList) {
    SubmitProcessedFrame(latency, PhotonTargetSortMode::LeftMost(), targetList);
  }

  /**
   * Simulate one processed frame of vision data, putting one result to NT.
   *
   * @param latency Latency of the provided frame
   * @param sortMode Order in which to sort targets
   * @param targetList List of targets detected
   */
  void SubmitProcessedFrame(
      units::millisecond_t latency,
      std::function<bool(const PhotonTrackedTarget& target1,
                         const PhotonTrackedTarget& target2)>
          sortMode,
      std::vector<PhotonTrackedTarget> targetList) {
    latencyMillisEntry.SetDouble(latency.to<double>());
    std::sort(targetList.begin(), targetList.end(),
              [&](auto lhs, auto rhs) { return sortMode(lhs, rhs); });
    PhotonPipelineResult newResult{latency, targetList};
    Packet packet{};
    packet << newResult;

    rawBytesPublisher.Set(
        std::span{packet.GetData().data(), packet.GetDataSize()});

    bool hasTargets = newResult.HasTargets();
    hasTargetEntry.SetBoolean(hasTargets);
    if (!hasTargets) {
      targetPitchEntry.SetDouble(0.0);
      targetYawEntry.SetDouble(0.0);
      targetAreaEntry.SetDouble(0.0);
      targetPoseEntry.SetDoubleArray(
          std::vector<double>{0.0, 0.0, 0.0, 0, 0, 0, 0});
      targetSkewEntry.SetDouble(0.0);
    } else {
      PhotonTrackedTarget bestTarget = newResult.GetBestTarget();
      targetPitchEntry.SetDouble(bestTarget.GetPitch());
      targetYawEntry.SetDouble(bestTarget.GetYaw());
      targetAreaEntry.SetDouble(bestTarget.GetArea());
      targetSkewEntry.SetDouble(bestTarget.GetSkew());

      frc::Transform3d transform = bestTarget.GetBestCameraToTarget();
      targetPoseEntry.SetDoubleArray(std::vector<double>{
          transform.X().to<double>(), transform.Y().to<double>(),
          transform.Z().to<double>(), transform.Rotation().GetQuaternion().W(),
          transform.Rotation().GetQuaternion().X(),
          transform.Rotation().GetQuaternion().Y(),
          transform.Rotation().GetQuaternion().Z()});
    }
  }

 private:
  nt::NetworkTableEntry latencyMillisEntry;
  nt::NetworkTableEntry hasTargetEntry;
  nt::NetworkTableEntry targetPitchEntry;
  nt::NetworkTableEntry targetYawEntry;
  nt::NetworkTableEntry targetAreaEntry;
  nt::NetworkTableEntry targetSkewEntry;
  nt::NetworkTableEntry targetPoseEntry;
  nt::NetworkTableEntry versionEntry;
  nt::RawPublisher rawBytesPublisher;
};
}  // namespace photonlib
