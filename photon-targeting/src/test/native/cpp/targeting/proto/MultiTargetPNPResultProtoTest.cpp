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
#include "photon/targeting/MultiTargetPNPResult.h"
#include "photon/targeting/proto/MultiTargetPNPResultProto.h"

// TEST(MultiTargetPNPResultTest, Roundtrip) {
//   photon::MultiTargetPNPResult result;

//   google::protobuf::Arena arena;
//   google::protobuf::Message* proto =
//       wpi::Protobuf<photon::MultiTargetPNPResult>::New(&arena);
//   wpi::Protobuf<photon::MultiTargetPNPResult>::Pack(proto, result);

//   photon::MultiTargetPNPResult unpacked_data =
//       wpi::Protobuf<photon::MultiTargetPNPResult>::Unpack(*proto);

//   EXPECT_EQ(result, unpacked_data);

//   photon::PnpResult pnpRes{
//       true,
//       frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
//                        frc::Rotation3d(1_rad, 2_rad, 3_rad)),
//       0.1,
//       frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
//                        frc::Rotation3d(1_rad, 2_rad, 3_rad)),
//       0.1,
//       0};

//   photon::MultiTargetPNPResult result1{pnpRes, {1, 2, 3, 4}};

//   proto = wpi::Protobuf<photon::MultiTargetPNPResult>::New(&arena);
//   wpi::Protobuf<photon::MultiTargetPNPResult>::Pack(proto, result1);

//   photon::MultiTargetPNPResult unpacked_data1 =
//       wpi::Protobuf<photon::MultiTargetPNPResult>::Unpack(*proto);

//   EXPECT_EQ(result1, unpacked_data1);
// }
