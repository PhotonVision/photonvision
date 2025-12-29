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

#pragma once

#include <memory>
#include <string>

#include <wpi/math/geometry/Transform3d.hpp>
#include <wpi/nt/BooleanTopic.hpp>
#include <wpi/nt/DoubleArrayTopic.hpp>
#include <wpi/nt/DoubleTopic.hpp>
#include <wpi/nt/IntegerTopic.hpp>
#include <wpi/nt/NetworkTable.hpp>
#include <wpi/nt/RawTopic.hpp>
#include <wpi/nt/StructTopic.hpp>

namespace photon {
const std::string PhotonPipelineResult_TYPE_STRING =
    std::string{"photonstruct:PhotonPipelineResult:"} +
    std::string{SerdeType<PhotonPipelineResult>::GetSchemaHash()};

class NTTopicSet {
 public:
  std::shared_ptr<wpi::nt::NetworkTable> subTable;
  wpi::nt::RawPublisher rawBytesEntry;

  wpi::nt::IntegerPublisher pipelineIndexPublisher;
  wpi::nt::IntegerSubscriber pipelineIndexRequestSub;

  wpi::nt::BooleanTopic driverModeEntry;
  wpi::nt::BooleanPublisher driverModePublisher;
  wpi::nt::BooleanSubscriber driverModeSubscriber;

  wpi::nt::DoublePublisher latencyMillisEntry;
  wpi::nt::BooleanPublisher hasTargetEntry;
  wpi::nt::DoublePublisher targetPitchEntry;
  wpi::nt::DoublePublisher targetYawEntry;
  wpi::nt::DoublePublisher targetAreaEntry;
  wpi::nt::StructPublisher<wpi::math::Transform3d> targetPoseEntry;
  wpi::nt::DoublePublisher targetSkewEntry;

  wpi::nt::DoublePublisher bestTargetPosX;
  wpi::nt::DoublePublisher bestTargetPosY;

  wpi::nt::IntegerTopic heartbeatTopic;
  wpi::nt::IntegerPublisher heartbeatPublisher;

  wpi::nt::DoubleArrayPublisher cameraIntrinsicsPublisher;
  wpi::nt::DoubleArrayPublisher cameraDistortionPublisher;

  void UpdateEntries() {
    wpi::nt::PubSubOptions options;
    options.periodic = 0.01;
    options.sendAll = true;
    rawBytesEntry = subTable->GetRawTopic("rawBytes")
                        .Publish(PhotonPipelineResult_TYPE_STRING, options);

    pipelineIndexPublisher =
        subTable->GetIntegerTopic("pipelineIndexState").Publish();
    pipelineIndexRequestSub =
        subTable->GetIntegerTopic("pipelineIndexRequest").Subscribe(0);

    driverModePublisher = subTable->GetBooleanTopic("driverMode").Publish();
    driverModeSubscriber =
        subTable->GetBooleanTopic("driverModeRequest").Subscribe(0);

    driverModeSubscriber.GetTopic().Publish().SetDefault(false);

    latencyMillisEntry = subTable->GetDoubleTopic("latencyMillis").Publish();
    hasTargetEntry = subTable->GetBooleanTopic("hasTargets").Publish();

    targetPitchEntry = subTable->GetDoubleTopic("targetPitch").Publish();
    targetAreaEntry = subTable->GetDoubleTopic("targetArea").Publish();
    targetYawEntry = subTable->GetDoubleTopic("targetYaw").Publish();
    targetPoseEntry =
        subTable->GetStructTopic<wpi::math::Transform3d>("targetPose")
            .Publish();
    targetSkewEntry = subTable->GetDoubleTopic("targetSkew").Publish();

    bestTargetPosX = subTable->GetDoubleTopic("targetPixelsX").Publish();
    bestTargetPosY = subTable->GetDoubleTopic("targetPixelsY").Publish();

    heartbeatTopic = subTable->GetIntegerTopic("heartbeat");
    heartbeatPublisher = heartbeatTopic.Publish();

    cameraIntrinsicsPublisher =
        subTable->GetDoubleArrayTopic("cameraIntrinsics").Publish();
    cameraDistortionPublisher =
        subTable->GetDoubleArrayTopic("cameraDistortion").Publish();
  }
};
}  // namespace photon
