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

#include <fmt/format.h>

#include <memory>

#include "photon/PhotonCamera.h"
#include "photon/simulation/VisionSystemSim.h"
#include "photonlib_nanobind.hpp"

// actual nanobind include
#include <nanobind/nanobind.h>
#include <nanobind/stl/string.h>
#include <nanobind/stl/vector.h>
#include <nanobind/stl/optional.h>

namespace nb = nanobind;
using namespace nb::literals;

void wrap_photon_sim(nb::module_ m) {
  using namespace photon;

  nb::class_<VisionSystemSim>(m, "VisionSystemSim")
      .def(nb::init<const std::string&>())
      .def("AddCamera", &VisionSystemSim::AddCamera)
      .def("AddVisionTargets",
           nb::overload_cast<std::string, const std::vector<VisionTargetSim>&>(
               &VisionSystemSim::AddVisionTargets))
      .def("Update",
           nb::overload_cast<const frc::Pose3d&>(&VisionSystemSim::Update));

  nb::class_<PhotonCameraSim>(m, "PhotonCameraSim")
      .def(nb::init<PhotonCamera*>());

  nb::class_<VisionTargetSim>(m, "VisionTargetSim")
      .def(nb::init<frc::Pose3d, TargetModel>(), "pose"_a, "model"_a, "Create a simulated target at a given pose")
      .def(nb::init<frc::Pose3d, TargetModel, int>(), "pose"_a, "model"_a, "fiducial_id"_a, "Create a simulated AprilTag at a given pose");

  nb::class_<TargetModel>(m, "TargetModel").def(nb::init<units::meter_t, units::meter_t>());
}
