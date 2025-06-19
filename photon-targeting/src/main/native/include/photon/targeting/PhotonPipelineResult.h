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

#include <span>
#include <string>
#include <utility>

#include <units/time.h>
#include <wpi/SmallVector.h>

#include "MultiTargetPNPResult.h"
#include "PhotonTrackedTarget.h"
#include "fmt/base.h"
#include "photon/dataflow/structures/Packet.h"
#include "photon/struct/PhotonPipelineResultStruct.h"

namespace photon {
/**
 * Represents a pipeline result from a PhotonCamera.
 */
class PhotonPipelineResult : public PhotonPipelineResult_PhotonStruct {
  using Base = PhotonPipelineResult_PhotonStruct;

 public:
  PhotonPipelineResult() : Base() {}
  explicit PhotonPipelineResult(Base&& data) : Base(data) {}

  // Don't forget to deal with our ntReceiveTimestamp
  PhotonPipelineResult(const PhotonPipelineResult& other)
      : Base(other), ntReceiveTimestamp(other.ntReceiveTimestamp) {}
  PhotonPipelineResult(PhotonPipelineResult& other)
      : Base(other), ntReceiveTimestamp(other.ntReceiveTimestamp) {}
  PhotonPipelineResult(PhotonPipelineResult&& other)
      : Base(std::move(other)),
        ntReceiveTimestamp(std::move(other.ntReceiveTimestamp)) {}
  auto& operator=(const PhotonPipelineResult& other) {
    Base::operator=(other);
    ntReceiveTimestamp = other.ntReceiveTimestamp;
    return *this;
  }
  auto& operator=(PhotonPipelineResult&& other) {
    ntReceiveTimestamp = other.ntReceiveTimestamp;
    Base::operator=(std::move(other));
    return *this;
  }

  template <typename... Args>
  explicit PhotonPipelineResult(Args&&... args)
      : Base{std::forward<Args>(args)...} {}

  /**
   * Returns the best target in this pipeline result. If there are no targets,
   * this method will return null. The best target is determined by the target
   * sort mode in the PhotonVision UI.
   *
   * @return The best target of the pipeline result.
   */
  PhotonTrackedTarget GetBestTarget() const {
    if (!HasTargets() && !HAS_WARNED) {
      fmt::println(
          "WARNING: This PhotonPipelineResult object has no targets associated "
          "with it! "
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
    return ntReceiveTimestamp - GetLatency();
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

  /** Sets the FPGA timestamp this result was Received by robot code */
  void SetReceiveTimestamp(const units::second_t timestamp) {
    this->ntReceiveTimestamp = timestamp;
  }

  /**
   * Returns whether the pipeline has targets.
   * @return Whether the pipeline has targets.
   */
  bool HasTargets() const { return targets.size() > 0; }

  /**
   * Returns a reference to the vector of targets.
   * <p> Returned in the order set by target sort mode. </p>
   * @return A reference to the vector of targets.
   */
  const std::span<const PhotonTrackedTarget> GetTargets() const {
    return targets;
  }

  friend bool operator==(PhotonPipelineResult const&,
                         PhotonPipelineResult const&) = default;

  // Since we don't trust NT time sync, keep track of when we got this packet
  // into robot code
  units::microsecond_t ntReceiveTimestamp = -1_s;

  inline static bool HAS_WARNED = false;
};
}  // namespace photon

#include "photon/serde/PhotonPipelineResultSerde.h"
