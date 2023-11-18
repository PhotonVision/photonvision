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

#include <iostream>

#include <units/angle.h>

#include "gtest/gtest.h"
#include "photon/dataflow/structures/Packet.h"
#include "photon/targeting/MultiTargetPNPResult.h"
#include "photon/targeting/PNPResult.h"
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"
#include "photon/targeting/TargetCorner.h"

TEST(PacketTest, PhotonTrackedTarget) {
  photon::PhotonTrackedTarget target{
      3.0,
      4.0,
      9.0,
      -5.0,
      -1,
      frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                       frc::Rotation3d(1_rad, 2_rad, 3_rad)),
      frc::Transform3d(frc::Translation3d(1_m, 2_m, 3_m),
                       frc::Rotation3d(1_rad, 2_rad, 3_rad)),
      -1,
      {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}},
      {std::pair{1, 2}, std::pair{3, 4}, std::pair{5, 6}, std::pair{7, 8}}};

  photon::Packet p;
  p << target;

  photon::PhotonTrackedTarget b;
  p >> b;

  for (auto& c : p.GetData()) {
    std::cout << static_cast<int>(c) << ",";
  }

  EXPECT_EQ(target, b);
}

TEST(PacketTest, PhotonPipelineResult) {
  photon::PhotonPipelineResult result{1_s, {}};
  photon::Packet p;
  p << result;

  photon::PhotonPipelineResult b;
  p >> b;

  EXPECT_EQ(result, b);

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
          {photon::TargetCorner(1,2), photon::TargetCorner(3,4), photon::TargetCorner(5,6), photon::TargetCorner(7,8)},
          {photon::TargetCorner(1,2), photon::TargetCorner(3,4), photon::TargetCorner(5,6), photon::TargetCorner(7,8)}},
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
          {photon::TargetCorner(1,2), photon::TargetCorner(3,4), photon::TargetCorner(5,6), photon::TargetCorner(7,8)},
          {photon::TargetCorner(1,2), photon::TargetCorner(3,4), photon::TargetCorner(5,6), photon::TargetCorner(7,8)}}};

  photon::PhotonPipelineResult result2{2_s, targets};
  photon::Packet p2;
  p2 << result2;

  photon::PhotonPipelineResult b2;
  p2 >> b2;

  EXPECT_EQ(result2, b2);
}
