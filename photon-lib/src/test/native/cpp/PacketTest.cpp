/*
 * MIT License
 *
 * Copyright (c) 2022 PhotonVision
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
#include "photonlib/PhotonPipelineResult.h"
#include "photonlib/PhotonTrackedTarget.h"

TEST(PacketTest, PhotonTrackedTarget) {
  photonlib::PhotonTrackedTarget target{
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

  photonlib::Packet p;
  p << target;

  photonlib::PhotonTrackedTarget b;
  p >> b;

  for (auto& c : p.GetData()) {
    std::cout << static_cast<int>(c) << ",";
  }

  EXPECT_EQ(target, b);
}

TEST(PacketTest, PhotonPipelineResult) {
  photonlib::PhotonPipelineResult result{1_s, {}};
  photonlib::Packet p;
  p << result;

  photonlib::PhotonPipelineResult b;
  p >> b;

  EXPECT_EQ(result, b);

  wpi::SmallVector<photonlib::PhotonTrackedTarget, 2> targets{
      photonlib::PhotonTrackedTarget{
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
      photonlib::PhotonTrackedTarget{
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

  photonlib::PhotonPipelineResult result2{2_s, targets};
  photonlib::Packet p2;
  p2 << result2;

  photonlib::PhotonPipelineResult b2;
  p2 >> b2;

  EXPECT_EQ(result2, b2);
}
