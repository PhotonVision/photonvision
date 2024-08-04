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

#include <utility>

#include "photon/struct/TargetCornerStruct.h"

#pragma once

namespace photon {
class TargetCorner : public TargetCorner_PhotonStruct {
  using Base = TargetCorner_PhotonStruct;

 public:
  explicit TargetCorner(Base&& data) : Base(data) {}

  template <typename... Args>
  explicit TargetCorner(Args&&... args) : Base(std::forward<Args>(args)...) {}

  friend bool operator==(TargetCorner const&, TargetCorner const&) = default;
};
}  // namespace photon

#include "photon/serde/TargetCornerSerde.h"
