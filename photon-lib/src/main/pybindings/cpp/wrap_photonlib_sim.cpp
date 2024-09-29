#include <fmt/format.h>

#include <memory>

#include "photon/PhotonCamera.h"
#include "photon/simulation/VisionSystemSim.h"

#include <pybind11/pybind11.h>
#include <pybind11/stl.h>

namespace py = pybind11;
using namespace pybind11::literals;

void wrap_photon_sim(py::module_ m) {
  using namespace photon;

  py::class_<PhotonCameraSim>(m, "PhotonCameraSim")
      .def(py::init<PhotonCamera*>());

  py::class_<VisionTargetSim>(m, "VisionTargetSim")
      .def(py::init<frc::Pose3d, TargetModel>(), "pose"_a, "model"_a, "Create a simulated target at a given pose")
      .def(py::init<frc::Pose3d, TargetModel, int>(), "pose"_a, "model"_a, "fiducial_id"_a, "Create a simulated AprilTag at a given pose");

  py::class_<VisionSystemSim>(m, "VisionSystemSim")
      .def(py::init<const std::string&>(), "visionSystemName"_a)
      .def("AddCamera", &VisionSystemSim::AddCamera)
      .def("AddVisionTargets",
           py::overload_cast<std::string, const std::vector<VisionTargetSim>&>(
               &VisionSystemSim::AddVisionTargets), "type"_a, "targets"_a)
      .def("Update",
           py::overload_cast<const frc::Pose3d&>(&VisionSystemSim::Update));
}
