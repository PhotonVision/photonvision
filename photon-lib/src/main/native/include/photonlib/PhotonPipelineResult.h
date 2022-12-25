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

#include <span>
#include <string>

#include <frc/Errors.h>
#include <units/time.h>
#include <wpi/SmallVector.h>

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
                       std::span<const PhotonTrackedTarget> targets);

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
   * Returns the estimated time the frame was taken,
   * This is much more accurate than using GetLatency()
   * @return The timestamp in seconds or -1 if this result was not initiated
   * with a timestamp.
   */
  units::second_t GetTimestamp() const { return timestamp; }

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
  bool operator!=(const PhotonPipelineResult& other) const;

  friend Packet& operator<<(Packet& packet, const PhotonPipelineResult& result);
  friend Packet& operator>>(Packet& packet, PhotonPipelineResult& result);

 private:
  units::second_t latency = 0_s;
  units::second_t timestamp = -1_s;
  wpi::SmallVector<PhotonTrackedTarget, 10> targets;
  inline static bool HAS_WARNED = false;
};
}  // namespace photonlib
