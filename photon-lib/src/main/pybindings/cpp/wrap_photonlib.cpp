
#include <fmt/format.h>

#include <memory>

#include "photon/PhotonCamera.h"
#include "photon/simulation/VisionSystemSim.h"

#include <pybind11/smart_holder.h>
#include <pybind11/stl.h>

namespace py = pybind11;
using namespace pybind11::literals;

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

void wrap_photon(py::module_ m) {
  py::class_<photon::PhotonPipelineMetadata>(m, "PhotonPipelineMetadata", "Metadata about the frame that this result was constructed from")
      .def_readonly("sequenceID", &photon::PhotonPipelineMetadata::sequenceID, "Number of frames processed since this VisionModule was started")
      .def_readonly("captureTimestampMicros",
              &photon::PhotonPipelineMetadata::captureTimestampMicros, "Timestamp (in the coprocessor timebase) that this frame was captured at")
      .def_readonly("publishTimestampMicros",
              &photon::PhotonPipelineMetadata::publishTimestampMicros, "Timestamp (in the coprocessor timebase) that this frame was published to NetworkTables at");

  py::class_<photon::PhotonTrackedTarget>(m, "PhotonTrackedTarget")
      .def(py::init<>())
      .def_readonly("yaw", &photon::PhotonTrackedTarget::yaw)
      .def_readonly("pitch", &photon::PhotonTrackedTarget::pitch)
      .def_readonly("bestCameraToTarget", &photon::PhotonTrackedTarget::bestCameraToTarget)
      .def_readonly("altCameraToTarget", &photon::PhotonTrackedTarget::altCameraToTarget)
      // String representation
      .def("__repr__", [](const photon::PhotonTrackedTarget& t) {
        std::string s;
        fmt::format_to(std::back_inserter(s),
                       "{}>", t);
        return s;
      });
  py::class_<photon::MultiTargetPNPResult>(m, "MultiTargetPNPResult")
      .def_readonly("fiducialIDsUsed",
              &photon::MultiTargetPNPResult::fiducialIDsUsed);

  py::class_<photon::PhotonPipelineResult>(m, "PhotonPipelineResult")
      .def_readonly("metadata", &photon::PhotonPipelineResult::metadata)
      .def_readonly("targets", &photon::PhotonPipelineResult::targets)
      .def_readonly("multitagResult", &photon::PhotonPipelineResult::multitagResult)
      .def("__repr__", [](const photon::PhotonPipelineResult& t) {
        std::string s;
        fmt::format_to(std::back_inserter(s),
                       "PhotonPipelineResult<metadata={},targets=[{}]>", t.metadata, fmt::join(t.targets, ", "));
        return s;
      });

  py::class_<photon::PhotonCamera>(m, "PhotonCamera")
      .def(py::init<std::string>())
      .def("GetDriverMode", &photon::PhotonCamera::GetDriverMode)
      .def("GetLatestResult", &photon::PhotonCamera::GetLatestResult);

  py::class_<photon::TargetModel>(m, "TargetModel")
      .def_property_readonly("Vertices", &photon::TargetModel::GetVertices)
      .def_property_readonly("IsPlaner", &photon::TargetModel::GetIsPlanar)
      .def_property_readonly("IsSpherical", &photon::TargetModel::GetIsSpherical)
    .def_readonly_static("kAprilTag16h5", &photon::kAprilTag16h5)
    .def_readonly_static("kAprilTag36h11", &photon::kAprilTag36h11)
  ;
}
