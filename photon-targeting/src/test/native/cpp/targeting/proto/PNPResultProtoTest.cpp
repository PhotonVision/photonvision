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

#include "gtest/gtest.h"
#include "photon.pb.h"
#include "photon/targeting/PNPResult.h"

TEST(PNPResultTest, Roundtrip) {
  photon::PNPResult result;

  google::protobuf::Arena arena;
  google::protobuf::Message* proto =
      wpi::Protobuf<photon::PNPResult>::New(&arena);
  wpi::Protobuf<photon::PNPResult>::Pack(proto, result);

  photon::PNPResult unpacked_data =
      wpi::Protobuf<photon::PNPResult>::Unpack(*proto);

  EXPECT_EQ(result, unpacked_data);

  photon::PNPResult result1{
      frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                       frc::Rotation3d(1_rad, 2_rad, 3_rad)),
      0.1,
      frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                       frc::Rotation3d(1_rad, 2_rad, 3_rad)),
      0.1, 0};

  proto = wpi::Protobuf<photon::PNPResult>::New(&arena);
  wpi::Protobuf<photon::PNPResult>::Pack(proto, result1);

  photon::PNPResult unpacked_data2 =
      wpi::Protobuf<photon::PNPResult>::Unpack(*proto);

  EXPECT_EQ(result1, unpacked_data2);
}
