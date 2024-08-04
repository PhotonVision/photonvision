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

#include <chrono>

#include <units/angle.h>

#include "gtest/gtest.h"
#include "photon/dataflow/structures/Packet.h"
#include "photon/targeting/MultiTargetPNPResult.h"
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"
#include "photon/targeting/PnpResult.h"

using namespace photon;

TEST(PacketTest, PnpResult) {
  PnpResult result{};

  result.best = {1_m, 2_m, 3_m, frc::Rotation3d{6_deg, 7_deg, 12_deg}};
  result.alt = {8_m, 2_m, 1_m, frc::Rotation3d{0_deg, 1_deg, 88_deg}};
  // determined by throwing a few D20s
  result.bestReprojErr = 7;
  result.altReprojErr = 11;
  result.ambiguity = 5.0 / 13.0;

  Packet p;
  p.Pack<PnpResult>(result);

  PnpResult b = p.Unpack<PnpResult>();

  EXPECT_EQ(result, b);
}

// TEST(PacketTest, MultiTargetPNPResult) {
//   MultiTargetPNPResult result;
//   Packet p;
//   p << result;

//   MultiTargetPNPResult b;
//   p >> b;

//   EXPECT_EQ(result, b);
// }

// TEST(PacketTest, PhotonTrackedTarget) {
//   PhotonTrackedTarget target{
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

//   Packet p;
//   p << target;

//   PhotonTrackedTarget b;
//   p >> b;

//   EXPECT_EQ(target, b);
// }

TEST(PacketTest, PhotonPipelineResult) {
  PhotonPipelineResult result(PhotonPipelineMetadata{0, 0, 1},
                              std::vector<PhotonTrackedTarget>{}, std::nullopt);

  Packet p;
  p.Pack<decltype(result)>(result);
  auto b = p.Unpack<decltype(result)>();
  EXPECT_EQ(result, b);

  std::vector<PhotonTrackedTarget> targets{
      PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1, -1, -1.0,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          -1,
          std::vector<TargetCorner>{TargetCorner{1, 2}, TargetCorner{3, 4},
                                    TargetCorner{5, 6}, TargetCorner{7, 8}},
          std::vector<TargetCorner>{TargetCorner{1, 2}, TargetCorner{3, 4},
                                    TargetCorner{5, 6}, TargetCorner{7, 8}}},
      PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, -1, -1, -1.0,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          -1,
          std::vector<TargetCorner>{TargetCorner{1, 2}, TargetCorner{3, 4},
                                    TargetCorner{5, 6}, TargetCorner{7, 8}},
          std::vector<TargetCorner>{TargetCorner{1, 2}, TargetCorner{3, 4},
                                    TargetCorner{5, 6}, TargetCorner{7, 8}}}};

  MultiTargetPNPResult mtResult{
      PnpResult{frc::Transform3d{1_m, 2_m, 3_m,
                                 frc::Rotation3d{6_deg, 7_deg, 12_deg}},
                frc::Transform3d{8_m, 2_m, 1_m,
                                 frc::Rotation3d{0_deg, 1_deg, 88_deg}},
                // determined by throwing a few D20s
                17, 22.33, 2.54},
      std::vector<int16_t>{8, 7, 11, 22, 59, 40}};

  PhotonPipelineResult result2(PhotonPipelineMetadata{0, 0, 1}, targets,
                               mtResult);

  Packet p2;
  auto t1 = std::chrono::steady_clock::now();
  p2.Pack<decltype(result2)>(result2);
  auto t2 = std::chrono::steady_clock::now();
  auto b2 = p2.Unpack<decltype(result2)>();
  auto t3 = std::chrono::steady_clock::now();
  EXPECT_EQ(result2, b2);

  fmt::println(
      "Pack {} unpack {} packet length {}",
      std::chrono::duration_cast<std::chrono::nanoseconds>(t2 - t1).count(),
      std::chrono::duration_cast<std::chrono::nanoseconds>(t3 - t2).count(),
      p2.GetDataSize());
}
