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

#include <chrono>
#include <vector>

#include <units/angle.h>
#include <wpi/print.h>

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
  PhotonPipelineResult result(PhotonPipelineMetadata(0, 0, 1, 2),
                              std::vector<PhotonTrackedTarget>{}, std::nullopt);

  Packet p;
  p.Pack<decltype(result)>(result);
  auto b = p.Unpack<decltype(result)>();
  EXPECT_EQ(result, b);

  std::vector<PhotonTrackedTarget> targets{
      PhotonTrackedTarget{
          3.0, -4.0, 9.0, 4.0, 1, -1, -1.0f,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          -1.0,
          std::vector<TargetCorner>{
              TargetCorner{1., 2.}, TargetCorner{3.0, 4.0},
              TargetCorner{5., 6.}, TargetCorner{7.0, 8.0}},
          std::vector<TargetCorner>{
              TargetCorner{1., 2.}, TargetCorner{3.0, 4.0},
              TargetCorner{5., 6.}, TargetCorner{7.0, 8.0}}},
      PhotonTrackedTarget{
          3.0, -4.0, 9.1, 6.7, -1, -1, -1.0f,
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                           frc::Rotation3d(1_rad, 2_rad, 3_rad)),
          -1.0,
          std::vector<TargetCorner>{
              TargetCorner{1.0, 2.0}, TargetCorner{3.0, 4.0},
              TargetCorner{5.0, 6.0}, TargetCorner{7.0, 8.0}},
          std::vector<TargetCorner>{
              TargetCorner{1.0, 2.0}, TargetCorner{3.0, 4.0},
              TargetCorner{5.0, 6.0}, TargetCorner{7.0, 8.0}}}};

  MultiTargetPNPResult mtResult{
      PnpResult{frc::Transform3d{1_m, 2_m, 3_m,
                                 frc::Rotation3d{6_deg, 7_deg, 12_deg}},
                frc::Transform3d{8_m, 2_m, 1_m,
                                 frc::Rotation3d{0_deg, 1_deg, 88_deg}},
                // determined by throwing a few D20s
                17.0, 22.33, 2.54},
      std::vector<int16_t>{8, 7, 11, 22, 59, 40}};

  PhotonPipelineResult result2(PhotonPipelineMetadata{0, 0, 1, 1}, targets,
                               mtResult);

  Packet p2;
  auto t1 = std::chrono::steady_clock::now();
  p2.Pack<decltype(result2)>(result2);
  auto t2 = std::chrono::steady_clock::now();
  auto b2 = p2.Unpack<decltype(result2)>();
  auto t3 = std::chrono::steady_clock::now();
  EXPECT_EQ(result2, b2);

  wpi::println(
      "Pack {} unpack {} packet length {}",
      std::chrono::duration_cast<std::chrono::nanoseconds>(t2 - t1).count(),
      std::chrono::duration_cast<std::chrono::nanoseconds>(t3 - t2).count(),
      p2.GetDataSize());
}
