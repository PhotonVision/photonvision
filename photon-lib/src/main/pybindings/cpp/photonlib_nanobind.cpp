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

// actual nanobind include
#include <nanobind/nanobind.h>
#include <nanobind/stl/string.h>
#include <nanobind/stl/vector.h>
#include <nanobind/stl/optional.h>

namespace nb = nanobind;
using namespace nb::literals;

// repr helper
template <> struct fmt::formatter<photon::PhotonTrackedTarget> : formatter<string_view> {
  auto format(photon::PhotonTrackedTarget const &c, format_context &ctx) const {
    return fmt::format_to(ctx.out(), "PhotonTrackedTarget<yaw={},pitch={}>", c.yaw, c.pitch);
  }
};
template <> struct fmt::formatter<photon::PhotonPipelineMetadata> : formatter<string_view> {
  auto format(photon::PhotonPipelineMetadata const &c, format_context &ctx) const {
    return fmt::format_to(ctx.out(), "PhotonPipelineMetadata<sequenceID={}>", c.sequenceID);
  }
};

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

void wrap_photon(nb::module_ m) {
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
                       "{}>", t);
        return s;
      });
  nb::class_<photon::MultiTargetPNPResult>(m, "MultiTargetPNPResult")
      .def_ro("fiducialIDsUsed",
              &photon::MultiTargetPNPResult::fiducialIDsUsed);

  nb::class_<photon::PhotonPipelineResult>(m, "PhotonPipelineResult")
      .def_ro("metadata", &photon::PhotonPipelineResult::metadata)
      .def_ro("targets", &photon::PhotonPipelineResult::targets)
      .def_ro("multitagResult", &photon::PhotonPipelineResult::multitagResult)
      .def("__repr__", [](const photon::PhotonPipelineResult& t) {
        std::string s;
        fmt::format_to(std::back_inserter(s),
                       "PhotonPipelineResult<metadata={},targets=[{}]>", t.metadata, fmt::join(t.targets, ", "));
        return s;
      });

  nb::class_<photon::PhotonCamera>(m, "PhotonCamera")
      .def(nb::init<const std::string&>())
      .def("GetDriverMode", &photon::PhotonCamera::GetDriverMode)
      .def("GetLatestResult", &photon::PhotonCamera::GetLatestResult);
}

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

NB_MODULE(_photonlibpy, m) {
  m.doc() = "C++ bindings for photonlib";

  wrap_photon(m);
  wrap_photon_sim(m);
  wrap_geom(m);
}
