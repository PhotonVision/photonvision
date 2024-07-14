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
#include "photon/targeting/PhotonTrackedTarget.h"
#include "photon/targeting/proto/PhotonTrackedTargetProto.h"

// TEST(PhotonTrackedTargetTest, Roundtrip) {
//   photon::PhotonTrackedTarget target{
//       3.0,
//       4.0,
//       9.0,
//       -5.0,
//       -1,
//       -1,
//       -1.0,
//       frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
//                        frc::Rotation3d(1_rad, 2_rad, 3_rad)),
//       frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
//                        frc::Rotation3d(1_rad, 2_rad, 3_rad)),
//       -1,
//       {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}},
//       {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}};

//   google::protobuf::Arena arena;
//   google::protobuf::Message* proto =
//       wpi::Protobuf<photon::PhotonTrackedTarget>::New(&arena);
//   wpi::Protobuf<photon::PhotonTrackedTarget>::Pack(proto, target);

//   photon::PhotonTrackedTarget unpacked_data =
//       wpi::Protobuf<photon::PhotonTrackedTarget>::Unpack(*proto);

//   EXPECT_EQ(target, unpacked_data);
// }
