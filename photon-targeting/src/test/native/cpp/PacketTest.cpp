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

#include <units/angle.h>

#include "gtest/gtest.h"
#include "photon/dataflow/structures/Packet.h"
#include "photon/targeting/MultiTargetPNPResult.h"
#include "photon/targeting/PNPResult.h"
#include "photon/targeting/PhotonPipelineResult.h"
#include "photon/targeting/PhotonTrackedTarget.h"

TEST(PacketTest, PNPResult) {
  photon::PNPResult result;
  photon::Packet p;
  p << result;

  photon::PNPResult b;
  p >> b;

  EXPECT_EQ(result, b);
}

TEST(PacketTest, MultiTargetPNPResult) {
  photon::MultiTargetPNPResult result;
  photon::Packet p;
  p << result;

  photon::MultiTargetPNPResult b;
  p >> b;

  EXPECT_EQ(result, b);
}

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

  EXPECT_EQ(target, b);
}

TEST(PacketTest, PhotonPipelineResult) {
  photon::PhotonPipelineResult result{0, 0_s, 1_s, {}};
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

  photon::PhotonPipelineResult result2{0, 0_s, 1_s, targets};
  photon::Packet p2;
  p2 << result2;

  photon::PhotonPipelineResult b2;
  p2 >> b2;

  EXPECT_EQ(result2, b2);
}
