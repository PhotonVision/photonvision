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
#include "photon/PhotonCamera.h"
#include <frc/geometry/Translation3d.h>

void print_t(frc::Translation3d t) {
    fmt::println("x {} y {} z {}", t.X(), t.Y(), t.Z());
}

// actual nanobind include
#include <pybind11/pybind11.h>

namespace py = pybind11;

PYBIND11_MODULE(_photonlibpy, m) {

  m.doc() = "C++ bindings for photonlib";

  // hack -- can't make stubs...
  py::module wpimath = py::module::import("wpimath");
  py::module geom = py::module::import("wpimath.geometry");

  auto func = m.def("print_t", &print_t, "Print an frc::Translation3d", py::arg("t"));

//   py::class_<photon::PhotonPipelineMetadata>(m, "PhotonPipelineMetadata")
//       .def(py::init<>())
//       .def_readonly("sequenceID", &photon::PhotonPipelineMetadata::sequenceID)
//       .def_readonly("captureTimestampMicros",
//               &photon::PhotonPipelineMetadata::captureTimestampMicros)
//       .def_readonly("publishTimestampMicros",
//               &photon::PhotonPipelineMetadata::publishTimestampMicros);

//   py::class_<photon::PhotonTrackedTarget>(m, "PhotonTrackedTarget")
//       .def_readonly("yaw", &photon::PhotonTrackedTarget::yaw)
//       .def_readonly("pitch", &photon::PhotonTrackedTarget::pitch)
//       // String representation
//       .def("__repr__", [](const photon::PhotonTrackedTarget& t) {
//         std::string s;
//         fmt::format_to(std::back_inserter(s),
//                        "PhotonTrackedTarget<yaw={},pitch={}>", t.yaw, t.pitch);
//         return s;
//       });
//   py::class_<photon::MultiTargetPNPResult>(m, "MultiTargetPNPResult")
//       .def_readonly("fiducialIDsUsed", &photon::MultiTargetPNPResult::fiducialIDsUsed)
//   ;

//   py::class_<photon::PhotonPipelineResult>(m, "PhotonPipelineResult")
//       .def_readonly("metadata", &photon::PhotonPipelineResult::metadata)
//       .def_readonly("targets", &photon::PhotonPipelineResult::targets)
//       .def_readonly("multitagResult", &photon::PhotonPipelineResult::multitagResult);

//   py::class_<photon::PhotonCamera>(m, "PhotonCamera")
//       .def(py::init<const std::string&>())
//       .def("GetDriverMode", &photon::PhotonCamera::GetDriverMode)
//       .def("GetLatestResult", &photon::PhotonCamera::GetLatestResult);
}
