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

#include <frc/geometry/Pose3d.h>
#include <units/area.h>

namespace photonlib {
class SimVisionTarget {
 public:
  SimVisionTarget() = default;

  /**
   * Describes a vision target located somewhere on the field that your
   * SimVisionSystem can detect.
   *
   * @param targetPose Pose3d of the target in field-relative coordinates
   * @param targetWidth Width of the outer bounding box of the target.
   * @param targetHeight Pair Height of the outer bounding box of the
   * target.
   * @param targetId Id of the target
   */
  SimVisionTarget(frc::Pose3d targetPose, units::meter_t targetWidth,
                  units::meter_t targetHeight, int targetId)
      : targetPose(targetPose),
        targetWidth(targetWidth),
        targetHeight(targetHeight),
        targetArea(targetHeight * targetWidth),
        targetId(targetId) {}

  frc::Pose3d targetPose;
  units::meter_t targetWidth;
  units::meter_t targetHeight;
  units::square_meter_t targetArea;
  int targetId;
};
}  // namespace photonlib
