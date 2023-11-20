/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#pragma once

#include <span>
#include <string>

#include <frc/Errors.h>
#include <units/time.h>
#include <wpi/SmallVector.h>
#include <wpi/protobuf/Protobuf.h>

#include "MultiTargetPNPResult.h"
#include "PhotonTrackedTarget.h"

namespace photon {
/**
 * Represents a pipeline result from a PhotonCamera.
 */
class PhotonPipelineResult {
 public:
  units::millisecond_t latency = 0_s;
  units::second_t timestamp = -1_s;
  wpi::SmallVector<PhotonTrackedTarget, 10> targets;
  MultiTargetPNPResult multitagResult;
  inline static bool HAS_WARNED = false;

  /**
   * Constructs an empty pipeline result
   */
  PhotonPipelineResult() = default;

  /**
   * Constructs a pipeline result.
   * @param latency The latency in the pipeline.
   * @param targets The list of targets identified by the pipeline.
   */
  PhotonPipelineResult(units::millisecond_t latency,
                       std::span<const PhotonTrackedTarget> targets)
      : latency(latency),
        targets(targets.data(), targets.data() + targets.size()) {}

  /**
   * Constructs a pipeline result.
   * @param latency The latency in the pipeline.
   * @param targets The list of targets identified by the pipeline.
   * @param multitagResult The multitarget result
   */
  PhotonPipelineResult(units::millisecond_t latency,
                       std::span<const PhotonTrackedTarget> targets,
                       MultiTargetPNPResult multitagResult)
      : latency(latency),
        targets(targets.data(), targets.data() + targets.size()),
        multitagResult(multitagResult) {}

  /**
   * Returns the best target in this pipeline result. If there are no targets,
   * this method will return null. The best target is determined by the target
   * sort mode in the PhotonVision UI.
   *
   * @return The best target of the pipeline result.
   */
  PhotonTrackedTarget GetBestTarget() const {
    if (!HasTargets() && !HAS_WARNED) {
      FRC_ReportError(
          frc::warn::Warning, "{}",
          "This PhotonPipelineResult object has no targets associated with it! "
          "Please check HasTargets() before calling this method. For more "
          "information, please review the PhotonLib documentation at "
          "http://docs.photonvision.org");
      HAS_WARNED = true;
    }
    return HasTargets() ? targets[0] : PhotonTrackedTarget();
  }

  /**
   * Returns the latency in the pipeline.
   * @return The latency in the pipeline.
   */
  units::second_t GetLatency() const { return latency; }

  /**
   * Returns the estimated time the frame was taken,
   * This is much more accurate than using GetLatency()
   * @return The timestamp in seconds or -1 if this result was not initiated
   * with a timestamp.
   */
  units::second_t GetTimestamp() const { return timestamp; }

  /**
   * Return the latest mulit-target result, as calculated on your coprocessor.
   * Be sure to check getMultiTagResult().estimatedPose.isPresent before using
   * the pose estimate!
   */
  const MultiTargetPNPResult& MultiTagResult() const { return multitagResult; }

  /**
   * Sets the timestamp in seconds
   * @param timestamp The timestamp in seconds
   */
  void SetTimestamp(const units::second_t timestamp) {
    this->timestamp = timestamp;
  }

  /**
   * Returns whether the pipeline has targets.
   * @return Whether the pipeline has targets.
   */
  bool HasTargets() const { return targets.size() > 0; }

  /**
   * Returns a reference to the vector of targets.
   * @return A reference to the vector of targets.
   */
  const std::span<const PhotonTrackedTarget> GetTargets() const {
    return targets;
  }

  bool operator==(const PhotonPipelineResult& other) const;
};
}  // namespace photon

template <>
struct wpi::Protobuf<photon::PhotonPipelineResult> {
  static google::protobuf::Message* New(google::protobuf::Arena* arena);
  static photon::PhotonPipelineResult Unpack(
      const google::protobuf::Message& msg);
  static void Pack(google::protobuf::Message* msg,
                   const photon::PhotonPipelineResult& value);
};
