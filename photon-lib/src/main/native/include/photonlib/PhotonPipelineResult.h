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

#include <string>

#include <frc/Errors.h>
#include <units/time.h>
#include <wpi/SmallVector.h>
#include <wpi/span.h>

#include "photonlib/Packet.h"
#include "photonlib/PhotonTrackedTarget.h"

namespace photonlib {
/**
 * Represents a pipeline result from a PhotonCamera.
 */
class PhotonPipelineResult {
 public:
  /**
   * Constructs an empty pipeline result.
   */
  PhotonPipelineResult() = default;

  /**
   * Constructs a pipeline result.
   * @param latency The latency in the pipeline.
   * @param targets The list of targets identified by the pipeline.
   */
  PhotonPipelineResult(units::second_t latency,
                       wpi::span<const PhotonTrackedTarget> targets);

  /**
   * Returns the best target in this pipeline result. If there are no targets,
   * this method will return an empty target with all values set to zero. The
   * best target is determined by the target sort mode in the PhotonVision UI.
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
   * Returns whether the pipeline has targets.
   * @return Whether the pipeline has targets.
   */
  bool HasTargets() const { return targets.size() > 0; }

  /**
   * Returns a reference to the vector of targets.
   * @return A reference to the vector of targets.
   */
  const wpi::span<const PhotonTrackedTarget> GetTargets() const {
    return targets;
  }

  bool operator==(const PhotonPipelineResult& other) const;
  bool operator!=(const PhotonPipelineResult& other) const;

  friend Packet& operator<<(Packet& packet, const PhotonPipelineResult& result);
  friend Packet& operator>>(Packet& packet, PhotonPipelineResult& result);

 private:
  units::second_t latency = 0_s;
  wpi::SmallVector<PhotonTrackedTarget, 10> targets;
  inline static bool HAS_WARNED = false;
};
}  // namespace photonlib
