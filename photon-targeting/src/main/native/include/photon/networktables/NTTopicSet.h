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

#include <networktables/BooleanTopic.h>
#include <networktables/DoubleArrayTopic.h>
#include <networktables/DoubleTopic.h>
#include <networktables/IntegerTopic.h>
#include <networktables/NetworkTable.h>
#include <networktables/RawTopic.h>

namespace photon {
class NTTopicSet {
 public:
  std::shared_ptr<nt::NetworkTable> subTable;
  nt::RawPublisher rawBytesEntry;

  nt::IntegerPublisher pipelineIndexPublisher;
  nt::IntegerSubscriber pipelineIndexRequestSub;

  nt::BooleanTopic driverModeEntry;
  nt::BooleanPublisher driverModePublisher;
  nt::BooleanSubscriber driverModeSubscriber;

  nt::DoublePublisher latencyMillisEntry;
  nt::BooleanPublisher hasTargetEntry;
  nt::DoublePublisher targetPitchEntry;
  nt::DoublePublisher targetYawEntry;
  nt::DoublePublisher targetAreaEntry;
  nt::DoubleArrayPublisher targetPoseEntry;
  nt::DoublePublisher targetSkewEntry;

  nt::DoublePublisher bestTargetPosX;
  nt::DoublePublisher bestTargetPosY;

  nt::IntegerTopic heartbeatTopic;
  nt::IntegerPublisher heartbeatPublisher;

  nt::DoubleArrayPublisher cameraIntrinsicsPublisher;
  nt::DoubleArrayPublisher cameraDistortionPublisher;

  void UpdateEntries() {
    nt::PubSubOptions options;
    options.periodic = 0.01;
    options.sendAll = true;
    rawBytesEntry =
        subTable->GetRawTopic("rawBytes").Publish("rawBytes", options);

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
    targetPoseEntry = subTable->GetDoubleArrayTopic("targetPose").Publish();
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

 private:
};
}  // namespace photon
