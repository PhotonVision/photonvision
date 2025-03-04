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

#include <utility>

#include <frc/geometry/Transform3d.h>
#include <wpi/SmallVector.h>

#include "PnpResult.h"
#include "photon/dataflow/structures/Packet.h"
#include "photon/struct/MultiTargetPNPResultStruct.h"

namespace photon {
class MultiTargetPNPResult : public MultiTargetPNPResult_PhotonStruct {
  using Base = MultiTargetPNPResult_PhotonStruct;

 public:
  explicit MultiTargetPNPResult(Base&& data) : Base(data) {}

  template <typename... Args>
  explicit MultiTargetPNPResult(Args&&... args)
      : Base{std::forward<Args>(args)...} {}

  friend bool operator==(MultiTargetPNPResult const&,
                         MultiTargetPNPResult const&) = default;
};
}  // namespace photon

#include "photon/serde/MultiTargetPNPResultSerde.h"
