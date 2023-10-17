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

#include <frc/geometry/Transform3d.h>
#include <wpi/SmallVector.h>

#include "photonlib/Packet.h"

namespace photonlib {

class PNPResults {
 public:
  // This could be wrapped in an std::optional, but chose to do it this way to
  // mirror Java
  bool isValid;

  frc::Transform3d best;
  double bestReprojectionErr;

  frc::Transform3d alt;
  double altReprojectionErr;

  double ambiguity;

  friend Packet& operator<<(Packet& packet, const PNPResults& result);
  friend Packet& operator>>(Packet& packet, PNPResults& result);
};

class MultiTargetPnpResult {
 public:
  PNPResults result;
  wpi::SmallVector<int16_t, 32> fiducialIdsUsed;

  friend Packet& operator<<(Packet& packet, const MultiTargetPnpResult& result);
  friend Packet& operator>>(Packet& packet, MultiTargetPnpResult& result);
};

}  // namespace photonlib
