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

#include "photonlib/MultiTargetPNPResult.h"

namespace photonlib {

Packet& operator<<(Packet& packet, const MultiTargetPnpResult& target) {
  packet << target.result;

  size_t i;
  for (i = 0; i < target.fiducialIdsUsed.size(); i++) {
    packet << target.fiducialIdsUsed[i];
  }
  for (; i < target.fiducialIdsUsed.capacity(); i++) {
    packet << -128;
  }

  return packet;
}

Packet& operator>>(Packet& packet, MultiTargetPnpResult& target) {
  packet >> target.result;

  target.fiducialIdsUsed.clear();
  for (size_t i = 0; i < target.fiducialIdsUsed.capacity(); i++) {
    int8_t id = 0;
    packet >> id;

    if (id > -128) {
      target.fiducialIdsUsed.push_back(id);
    }
  }

  return packet;
}

// Encode a transform3d
Packet& operator<<(Packet& packet, const frc::Transform3d& transform) {
  packet << transform.Translation().X().value()
         << transform.Translation().Y().value()
         << transform.Translation().Z().value()
         << transform.Rotation().GetQuaternion().W()
         << transform.Rotation().GetQuaternion().X()
         << transform.Rotation().GetQuaternion().Y()
         << transform.Rotation().GetQuaternion().Z();

  return packet;
}

// Decode a transform3d
Packet& operator>>(Packet& packet, frc::Transform3d& transform) {
  frc::Transform3d ret;

  // We use these for best and alt transforms below
  double x = 0;
  double y = 0;
  double z = 0;
  double w = 0;

  // decode and unitify translation
  packet >> x >> y >> z;
  const auto translation = frc::Translation3d(
      units::meter_t(x), units::meter_t(y), units::meter_t(z));

  // decode and add units to rotation
  packet >> w >> x >> y >> z;
  const auto rotation = frc::Rotation3d(frc::Quaternion(w, x, y, z));

  transform = frc::Transform3d(translation, rotation);

  return packet;
}

Packet& operator<<(Packet& packet, PNPResults const& result) {
  packet << result.isValid << result.best << result.alt
         << result.bestReprojectionErr << result.altReprojectionErr
         << result.ambiguity;

  return packet;
}
Packet& operator>>(Packet& packet, PNPResults& result) {
  packet >> result.isValid >> result.best >> result.alt >>
      result.bestReprojectionErr >> result.altReprojectionErr >>
      result.ambiguity;

  return packet;
}

}  // namespace photonlib
