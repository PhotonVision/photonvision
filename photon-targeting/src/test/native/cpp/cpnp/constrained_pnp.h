#pragma once

#include <frc/geometry/Pose2d.h>
#include <frc/geometry/Transform3d.h>
#include <Eigen/Core>
#include <Eigen/LU>
#include <vector>

namespace cpnp {
    struct ProblemParams {
        // Homogonous world points, (x y z 1)^T
        std::vector<double> worldPoints;
        // Image points, 
        std::vector<double> imagePoints;
        double f_x, f_y, c_x, c_y;
    };

    frc::Pose2d solve_naive(const ProblemParams& problem);

    frc::Pose2d solve_polynomial(const ProblemParams& problem);
}
