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

#include "gtest/gtest.h"

#include <map>
#include <utility>
#include <vector>

#include <frc/geometry/Pose3d.h>

#include "photonlib/PhotonPipelineResult.h"
#include "photonlib/PhotonTrackedTarget.h"
#include "photonlib/RobotPoseEstimator.h"

TEST(RobotPoseEstimator, LowestAmbiguityStrategy) {
    std::map<int, frc::Pose3d> aprilTags;
    aprilTags.put(0, frc::Pose3d(3, 3, 3, frc::Rotation3d()))
    aprilTags.put(1, frc::Pose3d(5, 5, 5, frc::Rotation3d()))

    vector<std::pair<PhotonCamera, frc::Transform3d>> cameras;
    EXPECT_EQ(0, 1);
}
