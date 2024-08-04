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
#include <utility>

#include <frc/Errors.h>
#include <units/time.h>
#include <wpi/SmallVector.h>

#include "MultiTargetPNPResult.h"
#include "PhotonTrackedTarget.h"
#include "photon/dataflow/structures/Packet.h"
#include "photon/struct/PhotonPipelineResultStruct.h"

namespace photon {
/**
 * Represents a pipeline result from a PhotonCamera.
 */
class PhotonPipelineResult : public PhotonPipelineResult_PhotonStruct {
  using Base = PhotonPipelineResult_PhotonStruct;

 public:
  explicit PhotonPipelineResult(Base&& data) : Base(data) {}

  template <typename... Args>
  explicit PhotonPipelineResult(Args&&... args)
      : Base(std::forward<Args>(args)...) {}

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
    return HasTargets() ? targets[0] : PhotonTrackedTarget{};
  }

  /**
   * Returns the latency in the pipeline.
   * @return The latency in the pipeline.
   */
  units::millisecond_t GetLatency() const {
    return units::microsecond_t{static_cast<double>(
        metadata.publishTimestampMicros - metadata.captureTimestampMicros)};
  }

  /**
   * Returns the estimated time the frame was taken,
   * This is much more accurate than using GetLatency()
   * @return The timestamp in seconds or -1 if this result was not initiated
   * with a timestamp.
   */
  units::second_t GetTimestamp() const {
    return ntRecieveTimestamp - GetLatency();
  }

  /**
   * Return the latest mulit-target result, as calculated on your coprocessor.
   * Be sure to check getMultiTagResult().estimatedPose.isPresent before using
   * the pose estimate!
   */
  const std::optional<MultiTargetPNPResult>& MultiTagResult() const {
    return multitagResult;
  }

  /**
   * The number of non-empty frames processed by this camera since boot. Useful
   * to checking if a camera is alive.
   */
  int64_t SequenceID() const { return metadata.sequenceID; }

  /** Sets the FPGA timestamp this result was recieved by robot code */
  void SetRecieveTimestamp(const units::second_t timestamp) {
    this->ntRecieveTimestamp = timestamp;
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

  friend bool operator==(PhotonPipelineResult const&,
                         PhotonPipelineResult const&) = default;

  // Since we don't trust NT time sync, keep track of when we got this packet
  // into robot code
  units::microsecond_t ntRecieveTimestamp = -1_s;

  inline static bool HAS_WARNED = false;
};
}  // namespace photon

#include "photon/serde/PhotonPipelineResultSerde.h"
