#include <pybind11/pybind11.h>
#include <iostream>

#include "frc/geometry/Translation3d.h"

namespace py = pybind11;
using namespace frc;
using namespace std;

void print_t(frc::Translation3d t) {
    using namespace std;
    cout << "Translation x " << t.X().to<double>() << " y " << t.Y().to<double>() << " z " << t.Z().to<double>() << endl;
}

PYBIND11_MODULE(wrap_a, m) {
  m.def("print_t", &print_t, "A function to print a pose");
}
