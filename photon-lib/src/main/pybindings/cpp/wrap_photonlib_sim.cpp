#include <fmt/format.h>

#include <memory>

#include "photon/PhotonCamera.h"
#include "photon/simulation/VisionSystemSim.h"

#include <pybind11/smart_holder.h>
#include <pybind11/stl.h>

namespace py = pybind11;
using namespace pybind11::literals;

void wrap_photon_sim(py::module_ m) {
  using namespace photon;

  py::class_<PhotonCameraSim>(m, "PhotonCameraSim")
      .def(py::init<std::shared_ptr<PhotonCamera>>())
      .def("EnableDrawWireframe", &PhotonCameraSim::EnableDrawWireframe, "enabled"_a)
      ;

  py::class_<VisionTargetSim>(m, "VisionTargetSim")
      .def(py::init<frc::Pose3d, TargetModel>(), "pose"_a, "model"_a, "Create a simulated target at a given pose")
      .def(py::init<frc::Pose3d, TargetModel, int>(), "pose"_a, "model"_a, "fiducial_id"_a, "Create a simulated AprilTag at a given pose");

  py::class_<VisionSystemSim>(m, "VisionSystemSim")
      .def(py::init<const std::string&>(), "visionSystemName"_a)
      .def("AddCamera", [](std::shared_ptr<VisionSystemSim> self,
            std::shared_ptr<PhotonCameraSim> cameraSim,
            frc::Transform3d robotToCamera) {
                self->AddCamera(cameraSim.get(), robotToCamera);
            }, "cameraSim"_a, "robotToCamera"_a)
      .def("AddAprilTags", &VisionSystemSim::AddAprilTags, "layout"_a)
      .def("AddVisionTargets",
           py::overload_cast<std::string, const std::vector<VisionTargetSim>&>(
               &VisionSystemSim::AddVisionTargets), "type"_a, "targets"_a)
      .def("Update",
           py::overload_cast<const frc::Pose3d&>(&VisionSystemSim::Update));
}
