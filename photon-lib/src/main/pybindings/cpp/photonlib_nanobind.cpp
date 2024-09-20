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

// actual nanobind include
#include <nanobind/nanobind.h>
#include <nanobind/stl/string.h>
#include <nanobind/stl/vector.h>
#include <nanobind/stl/optional.h>

NB_MODULE(_photonlibpy, m) {
  namespace nb = nanobind;

  m.doc() = "C++ bindings for photonlib";

  nb::class_<photon::PhotonPipelineMetadata>(m, "PhotonPipelineMetadata")
      .def_ro("sequenceID", &photon::PhotonPipelineMetadata::sequenceID)
      .def_ro("captureTimestampMicros",
              &photon::PhotonPipelineMetadata::captureTimestampMicros)
      .def_ro("publishTimestampMicros",
              &photon::PhotonPipelineMetadata::publishTimestampMicros);

  nb::class_<photon::PhotonTrackedTarget>(m, "PhotonTrackedTarget")
      .def_ro("yaw", &photon::PhotonTrackedTarget::yaw)
      .def_ro("pitch", &photon::PhotonTrackedTarget::pitch)
      // String representation
      .def("__repr__", [](const photon::PhotonTrackedTarget& t) {
        std::string s;
        fmt::format_to(std::back_inserter(s),
                       "PhotonTrackedTarget<yaw={},pitch={}>", t.yaw, t.pitch);
        return s;
      });
  nb::class_<photon::MultiTargetPNPResult>(m, "MultiTargetPNPResult")
      .def_ro("fiducialIDsUsed", &photon::MultiTargetPNPResult::fiducialIDsUsed)
  ;

  nb::class_<photon::PhotonPipelineResult>(m, "PhotonPipelineResult")
      .def_ro("metadata", &photon::PhotonPipelineResult::metadata)
      .def_ro("targets", &photon::PhotonPipelineResult::targets)
      .def_ro("multitagResult", &photon::PhotonPipelineResult::multitagResult);

  nb::class_<photon::PhotonCamera>(m, "PhotonCamera")
      .def(nb::init<const std::string&>())
      .def("GetDriverMode", &photon::PhotonCamera::GetDriverMode)
      .def("GetLatestResult", &photon::PhotonCamera::GetLatestResult);
}
