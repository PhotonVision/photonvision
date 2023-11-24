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
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"

// TODO
TEST(PhotonPipelineResultTest, Equality) {
  photon::PhotonPipelineResult a{12_ms, {}};
  photon::PhotonPipelineResult b{12_ms, {}};

  EXPECT_EQ(a, b);

  wpi::SmallVector<photon::PhotonTrackedTarget, 2> targets{
      photon::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.0,
          4.0,
          1,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          -1,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}},
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}},
      photon::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.1,
          6.7,
          -1,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          -1,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}},
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  photon::PhotonPipelineResult a1{12_ms, targets};
  photon::PhotonPipelineResult b1{12_ms, targets};

  EXPECT_EQ(a1, b1);

  photon::MultiTargetPNPResult multitagRes{
      frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                       frc::Rotation3d(1_rad, 2_rad, 3_rad)),
      0.1,
      frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                       frc::Rotation3d(1_rad, 2_rad, 3_rad)),
      0.1,
      0,
      {1, 2, 3, 4}};

  photon::PhotonPipelineResult a2{12_ms, targets, multitagRes};
  photon::PhotonPipelineResult b2{12_ms, targets, multitagRes};

  EXPECT_EQ(a2, b2);
}

TEST(PhotonPipelineResultTest, Roundtrip) {
  photon::PhotonPipelineResult result{12_ms, {}};

  google::protobuf::Arena arena;
  google::protobuf::Message* proto =
      wpi::Protobuf<photon::PhotonPipelineResult>::New(&arena);
  wpi::Protobuf<photon::PhotonPipelineResult>::Pack(proto, result);

  photon::PhotonPipelineResult unpacked_data =
      wpi::Protobuf<photon::PhotonPipelineResult>::Unpack(*proto);

  EXPECT_EQ(result, unpacked_data);

  wpi::SmallVector<photon::PhotonTrackedTarget, 2> targets{
      photon::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.0,
          4.0,
          1,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          -1,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}},
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}},
      photon::PhotonTrackedTarget{
          3.0,
          -4.0,
          9.1,
          6.7,
          -1,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          -1,
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}},
          {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6},
           std::pair{7, 8}}}};

  photon::PhotonPipelineResult result2{12_ms, targets};

  proto = wpi::Protobuf<photon::PhotonPipelineResult>::New(&arena);
  wpi::Protobuf<photon::PhotonPipelineResult>::Pack(proto, result2);

  photon::PhotonPipelineResult unpacked_data2 =
      wpi::Protobuf<photon::PhotonPipelineResult>::Unpack(*proto);

  EXPECT_EQ(result2, unpacked_data2);

  photon::MultiTargetPNPResult multitagRes{
      frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                       frc::Rotation3d(1_rad, 2_rad, 3_rad)),
      0.1,
      frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                       frc::Rotation3d(1_rad, 2_rad, 3_rad)),
      0.1,
      0,
      {1, 2, 3, 4}};

  photon::PhotonPipelineResult result3{12_ms, targets, multitagRes};

  proto = wpi::Protobuf<photon::PhotonPipelineResult>::New(&arena);
  wpi::Protobuf<photon::PhotonPipelineResult>::Pack(proto, result3);

  photon::PhotonPipelineResult unpacked_data3 =
      wpi::Protobuf<photon::PhotonPipelineResult>::Unpack(*proto);

  EXPECT_EQ(result3, unpacked_data3);
}
