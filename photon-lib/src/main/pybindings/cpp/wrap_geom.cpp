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

frc::Pose3d makePose(double x, double y, double z, double W, double X, double Y,
                     double Z) {
  return frc::Pose3d{frc::Translation3d{units::meter_t{x}, units::meter_t{y},
                                        units::meter_t{z}},
                     frc::Rotation3d{frc::Quaternion{W, X, Y, Z}}};
}

void wrap_geom(nb::module_ m) {
  using namespace frc;
  nb::class_<Transform3d>(m, "Transform3d").def(nb::init<>());
  nb::class_<Pose3d>(m, "Pose3d")
      .def(nb::init<>())
      .def(nb::new_(&makePose),
           "Create a Pose3d from translation/rotation components");
}
