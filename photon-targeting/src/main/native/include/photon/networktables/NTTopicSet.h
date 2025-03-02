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

#pragma once

#include <memory>
#include <string>

#include <frc/geometry/Transform3d.h>
#include <networktables/BooleanTopic.h>
#include <networktables/DoubleArrayTopic.h>
#include <networktables/DoubleTopic.h>
#include <networktables/IntegerTopic.h>
#include <networktables/NetworkTable.h>
#include <networktables/RawTopic.h>
#include <networktables/StructTopic.h>

namespace photon {
const std::string PhotonPipelineResult_TYPE_STRING =
    std::string{"photonstruct:PhotonPipelineResult:"} +
    std::string{SerdeType<PhotonPipelineResult>::GetSchemaHash()};

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
  nt::StructPublisher<frc::Transform3d> targetPoseEntry;
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
        subTable->GetStructTopic<frc::Transform3d>("targetPose").Publish();
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
