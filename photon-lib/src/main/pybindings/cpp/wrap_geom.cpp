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

// repr helpers
template <> struct fmt::formatter<frc::Pose3d> : formatter<string_view> {
  auto format(frc::Pose3d const &c, format_context &ctx) const {
    return fmt::format_to(ctx.out(), "pp_Pose3d<x={},y={},z={}>", c.X(), c.Y(), c.Z());
  }
};

frc::Pose3d makePoseDouble(double x, double y, double z, double W, double X, double Y,
                     double Z) {
  return frc::Pose3d{frc::Translation3d{units::meter_t{x}, units::meter_t{y},
                                        units::meter_t{z}},
                     frc::Rotation3d{frc::Quaternion{W, X, Y, Z}}};
}

frc::Pose3d makePoseObject(nb::object obj) {
  auto rotation {
    nb::getattr(obj, "rotation")()
  };
  auto quat {
    nb::getattr(rotation, "getQuaternion")()
  };

  return frc::Pose3d{
    frc::Translation3d{
      units::meter_t{nb::cast<double>(nb::getattr(obj, "x"))},
      units::meter_t{nb::cast<double>(nb::getattr(obj, "y"))},
      units::meter_t{nb::cast<double>(nb::getattr(obj, "z"))}
    },
    frc::Rotation3d{frc::Quaternion{
      nb::cast<double>(nb::getattr(quat, "W")()),
      nb::cast<double>(nb::getattr(quat, "X")()),
      nb::cast<double>(nb::getattr(quat, "Y")()),
      nb::cast<double>(nb::getattr(quat, "Z")())
    }}
  };
}

void wrap_geom(nb::module_ m) {
  using namespace frc;

  nb::class_<Translation3d>(m, "Translation3d")
    .def_prop_ro("x", [](Translation3d& t) { return t.X().to<double>(); })
    ;
  nb::class_<Rotation3d>(m, "Rotation3d")
    .def_prop_ro("quaternion", [](Rotation3d& t) { return t.GetQuaternion(); })
    ;
  nb::class_<Quaternion>(m, "Quaternion")
    .def_prop_ro("W", [](Quaternion& t) { return t.W(); })
    ;

  nb::class_<Pose3d>(m, "Pose3d")
      .def(nb::init<>())
      .def(nb::new_(&makePoseDouble),
           "Create a Pose3d from translation/rotation components")
      .def(nb::new_(&makePoseObject),
           "Create a Pose3d from a pyobject")
      .def_prop_ro("Translation", &Pose3d::Translation)
      .def_prop_ro("Rotation", &Pose3d::Rotation)
      .def("__repr__", [](const Pose3d& p) {
        std::string s;
        fmt::format_to(std::back_inserter(s),
                       "{}>", p);
        return s;
      });
}
