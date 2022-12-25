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

#include <cmath>

#include "photonlib/PhotonTrackedTarget.h"

namespace photonlib {

namespace PhotonTargetSortMode {

struct Smallest {
  inline bool operator()(const PhotonTrackedTarget& target1,
                         const PhotonTrackedTarget& target2) {
    return target1.GetArea() < target2.GetArea();
  }
};

struct Largest {
  inline bool operator()(const PhotonTrackedTarget& target1,
                         const PhotonTrackedTarget& target2) {
    return target1.GetArea() > target2.GetArea();
  }
};

struct Highest {
  inline bool operator()(const PhotonTrackedTarget& target1,
                         const PhotonTrackedTarget& target2) {
    return target1.GetPitch() < target2.GetPitch();
  }
};

struct Lowest {
  inline bool operator()(const PhotonTrackedTarget& target1,
                         const PhotonTrackedTarget& target2) {
    return target1.GetPitch() > target2.GetPitch();
  }
};

struct RightMost {
  inline bool operator()(const PhotonTrackedTarget& target1,
                         const PhotonTrackedTarget& target2) {
    return target1.GetYaw() < target2.GetYaw();
  }
};

struct LeftMost {
  inline bool operator()(const PhotonTrackedTarget& target1,
                         const PhotonTrackedTarget& target2) {
    return target1.GetYaw() > target2.GetYaw();
  }
};

struct CenterMost {
  inline bool operator()(const PhotonTrackedTarget& target1,
                         const PhotonTrackedTarget& target2) {
    return std::pow(target1.GetPitch(), 2) + std::pow(target1.GetYaw(), 2) <
           std::pow(target2.GetPitch(), 2) + std::pow(target2.GetYaw(), 2);
  }
};
}  // namespace PhotonTargetSortMode
}  // namespace photonlib
