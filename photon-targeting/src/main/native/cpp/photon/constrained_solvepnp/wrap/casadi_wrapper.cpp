/*
 * Copyright (C) Photon Vision.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

#include "photon/constrained_solvepnp/wrap/casadi_wrapper.h"

#include <fmt/core.h>

#include <chrono>
#include <cstdio>
#include <iostream>
#include <optional>
#include <vector>

#include <Eigen/Cholesky>
#include <Eigen/Core>
#include <Eigen/LU>
#include <frc/fmt/Eigen.h>
#include <wpi/timestamp.h>

#include "../generate/constrained_solvepnp_10_tags_fixed.h"
#include "../generate/constrained_solvepnp_10_tags_free.h"
#include "../generate/constrained_solvepnp_1_tags_fixed.h"
#include "../generate/constrained_solvepnp_1_tags_free.h"
#include "../generate/constrained_solvepnp_2_tags_fixed.h"
#include "../generate/constrained_solvepnp_2_tags_free.h"
#include "../generate/constrained_solvepnp_3_tags_fixed.h"
#include "../generate/constrained_solvepnp_3_tags_free.h"
#include "../generate/constrained_solvepnp_4_tags_fixed.h"
#include "../generate/constrained_solvepnp_4_tags_free.h"
#include "../generate/constrained_solvepnp_5_tags_fixed.h"
#include "../generate/constrained_solvepnp_5_tags_free.h"
#include "../generate/constrained_solvepnp_6_tags_fixed.h"
#include "../generate/constrained_solvepnp_6_tags_free.h"
#include "../generate/constrained_solvepnp_7_tags_fixed.h"
#include "../generate/constrained_solvepnp_7_tags_free.h"
#include "../generate/constrained_solvepnp_8_tags_fixed.h"
#include "../generate/constrained_solvepnp_8_tags_free.h"
#include "../generate/constrained_solvepnp_9_tags_fixed.h"
#include "../generate/constrained_solvepnp_9_tags_free.h"

constexpr bool VERBOSE = false;

struct Problem {
  int numTags;
  bool headingFree;
  int (*calc_J)(const casadi_real** arg, casadi_real** res, casadi_int* iw,
                casadi_real* w, int mem);
  int (*calc_gradJ)(const casadi_real** arg, casadi_real** res, casadi_int* iw,
                    casadi_real* w, int mem);
  int (*calc_hessJ)(const casadi_real** arg, casadi_real** res, casadi_int* iw,
                    casadi_real* w, int mem);
};

static std::optional<Problem> createProblem(int numTags, bool heading_free) {
#define MAKE_P(tags, suffix)                                   \
  Problem {                                                    \
    tags, heading_free, calc_J_##tags##_tags_heading_##suffix, \
        calc_gradJ_##tags##_tags_heading_##suffix,             \
        calc_hessJ_##tags##_tags_heading_##suffix              \
  }
#define MAKE_CASE(n) \
  case n:            \
    return heading_free ? MAKE_P(n, free) : MAKE_P(n, fixed);
  switch (numTags) {
    MAKE_CASE(1)
    MAKE_CASE(2)
    MAKE_CASE(3)
    MAKE_CASE(4)
    MAKE_CASE(5)
    MAKE_CASE(6)
    MAKE_CASE(7)
    MAKE_CASE(8)
    MAKE_CASE(9)
    MAKE_CASE(10)
    // TODO include more cases here
    default:
      return std::nullopt;
  }
#undef MAKE_P
#undef MAKE_CASE
}

template <int nState>
struct ProblemState {
  // Note that we use the full state vector regardless of if we optimize for it,
  // as we need to remember robot heading
  using FullStateMat = Eigen::Matrix<casadi_real, 3, 1, Eigen::ColMajor>;
  using StateMat = Eigen::Matrix<casadi_real, nState, 1, Eigen::ColMajor>;
  using GradientMat = Eigen::Matrix<casadi_real, nState, 1>;
  using HessianMat =
      Eigen::Matrix<casadi_real, nState, nState, Eigen::ColMajor>;

  // Parameters held constant through optimization
  Eigen::Matrix<casadi_real, 4, 4, Eigen::ColMajor> robot2camera;
  Eigen::Matrix<casadi_real, 4, Eigen::Dynamic, Eigen::ColMajor> field2points;
  Eigen::Matrix<casadi_real, 2, Eigen::Dynamic, Eigen::ColMajor>
      point_observations;
  constrained_solvepnp::CameraCalibration cameraCal;

  // our Problem with function pointers
  Problem problemSelected;

  // Measurements from external gyro
  casadi_real gyro_θ;
  casadi_real gyro_error_scale_fac;

#define MAKE_ARGV(x)                                      \
  const casadi_real* argv[] = {&x[0],                     \
                               &x[1],                     \
                               &x[2],                     \
                               &cameraCal.fx,             \
                               &cameraCal.fy,             \
                               &cameraCal.cx,             \
                               &cameraCal.cy,             \
                               robot2camera.data(),       \
                               field2points.data(),       \
                               point_observations.data(), \
                               &gyro_θ,                   \
                               &gyro_error_scale_fac}

  // helpers
  inline casadi_real calculateJ(FullStateMat x) {
    MAKE_ARGV(x);
    casadi_real J;
    casadi_real* j_out[] = {&J};
    if (problemSelected.calc_J(argv, j_out, NULL, NULL, 0)) {
      throw std::runtime_error("Failure calculating J!");
    }
    return J;
  }
  inline GradientMat calculateGradJ(FullStateMat x) {
    MAKE_ARGV(x);
    GradientMat g;
    casadi_real* grad_j_out[] = {g.data()};
    if (problemSelected.calc_gradJ(argv, grad_j_out, 0, 0, 0)) {
      throw std::runtime_error("Failure calculating gradJ!");
    }
    return g;
  }
  inline HessianMat calculateHessJ(FullStateMat x) {
    MAKE_ARGV(x);
    HessianMat H;
    casadi_real* hess_j_out[] = {H.data()};
    if (problemSelected.calc_hessJ(argv, hess_j_out, 0, 0, 0)) {
      throw std::runtime_error("Failure calculating hessJ!");
    }
    return H;
  }
#undef MAKE_ARGV
};

wpi::expected<constrained_solvepnp::RobotStateMat,
              sleipnir::SolverExitCondition>
constrained_solvepnp::do_optimization(
    bool heading_free, int nTags,
    constrained_solvepnp::CameraCalibration cameraCal,
    // Note that casadi is column major, apparently
    Eigen::Matrix<casadi_real, 4, 4, Eigen::ColMajor> robot2camera,
    constrained_solvepnp::RobotStateMat x_guess,
    Eigen::Matrix<casadi_real, 4, Eigen::Dynamic, Eigen::ColMajor> field2points,
    Eigen::Matrix<casadi_real, 2, Eigen::Dynamic, Eigen::ColMajor>
        point_observations,
    double gyroθ, double gyroErrorScaleFac) {
  if (field2points.cols() != (nTags * 4) ||
      point_observations.cols() != (nTags * 4)) {
    if constexpr (VERBOSE) fmt::println("Got unexpected num cols!");
    // TODO find a new error code
    return wpi::unexpected{
        sleipnir::SolverExitCondition::kNonfiniteInitialCostOrConstraints};
  }

  if constexpr (VERBOSE) {
    fmt::println("----------------------------------");
    fmt::println("heading free {}; heading {}, cost={}", heading_free, gyroθ,
                 gyroErrorScaleFac);
    fmt::println("Camera cal {} {} {} {}", cameraCal.fx, cameraCal.fy,
                 cameraCal.cx, cameraCal.cy);
    fmt::println("{} tags", nTags);
    // fmt::println("nstate {}", nState);

    std::cout << "robot2camera:\n" << robot2camera << std::endl;
    std::cout << "x guess:\n" << x_guess << std::endl;
    std::cout << "field2pt:\n" << field2points << std::endl;
    std::cout << "observations:\n" << point_observations << std::endl;
    fmt::println("---------^^^^^^^^---------");
  }

  auto problemOpt = createProblem(nTags, heading_free);
  if (!problemOpt) {
    return wpi::unexpected{
        sleipnir::SolverExitCondition::kNonfiniteInitialCostOrConstraints};
  }

  ProblemState<3> pState{robot2camera,     field2points, point_observations,
                         cameraCal,        *problemOpt,  gyroθ,
                         gyroErrorScaleFac};

  using FullStateMat = typename decltype(pState)::FullStateMat;

  using StateMat = typename decltype(pState)::StateMat;
  using HessianMat = typename decltype(pState)::HessianMat;
  using GradMat = typename decltype(pState)::GradientMat;

  FullStateMat x = x_guess;

  // Sleipnir's delta_I caching algo and Newton.cpp inspiration from
  // https://github.com/SleipnirGroup/Sleipnir/blob/5af8519f268a8075e245bb7cd411a81e1598f521/src/optimization/RegularizedLDLT.hpp#L163
  // licensed under BSD 3-Clause

  /// The value of δ from the previous iteration - 1e-4 is a sane guess
  /// TUNABLE
  double δ = 1e-4 * 2.0;

  constexpr double ERROR_TOL = 1e-4;

  for (int iter = 0; iter < 100; iter++) {
    auto iter_start = wpi::Now();

    // Check for diverging iterates
    if (x.template lpNorm<Eigen::Infinity>() > 1e20 || !x.allFinite()) {
      return wpi::unexpected{sleipnir::SolverExitCondition::kDivergingIterates};
    }

    GradMat g = pState.calculateGradJ(x);

    // If our previous step found an x such grad(J) is acceptable, we're done
    auto norm_g = g.template lpNorm<Eigen::Infinity>();
    if (norm_g < ERROR_TOL) {
      // Done!
      if constexpr (VERBOSE)
        fmt::println("{}: Exiting due to convergence (‖∇J‖={})", iter, norm_g);
      break;
    }

    HessianMat H = pState.calculateHessJ(x);

    /// Regularization. If the Hessian inertia is already OK, don't adjust

    auto H_ldlt = H.ldlt();
    if (H_ldlt.info() != Eigen::Success) {
      std::cerr << "LDLT decomp failed! H=" << std::endl << H << std::endl;
      return wpi::unexpected{sleipnir::SolverExitCondition::kLocallyInfeasible};
    }

    // Make sure H is positive definite (all eigenvalues are > 0)
    int i_reg{0};
    HessianMat H_reg =
        H;  // keep track of our regularized H TODO - is this valid?
    if ((H_ldlt.vectorD().array() <= 0.0).any()) {
      // If δthe Hessian wasn't regularized in a previous iteration, start at a
      // small value of δ. Otherwise, attempt a δ half as big as the previous
      // run so δ can trend downwards over time.
      δ = δ / 2.0;

      // Arbitrary max on regularization iterations
      int MAX_REG_STEPS = 100;
      for (i_reg = 0; i_reg < MAX_REG_STEPS; i_reg++) {
        // Try δ, which we may have adjusted above
        // std::printf("Trying %f\n", δ);
        HessianMat delta_I = HessianMat::Identity() * δ;
        H_reg = H + delta_I;
        H_ldlt = H_reg.ldlt();

        if (H_ldlt.info() != Eigen::Success) {
          std::cerr << "LDLT decomp failed! H=" << std::endl << H << std::endl;
          return wpi::unexpected{
              sleipnir::SolverExitCondition::kLocallyInfeasible};
        }

        // If our eigenvalues aren't positive definite, pick a new δ for next
        // loop
        if ((H_ldlt.vectorD().array() <= 0.0).any()) {
          δ *= 10.0;

          // If the Hessian perturbation is too high, report failure
          if (δ > 1e20) {
            return wpi::unexpected{
                sleipnir::SolverExitCondition::kLocallyInfeasible};
          }
        } else {
          // Done!
          break;
        }

        H = H + delta_I;
      }

      if (i_reg == MAX_REG_STEPS) {
        return wpi::unexpected{
            sleipnir::SolverExitCondition::kLocallyInfeasible};
      }
    } else {
      // std::printf("Already regularized\n");
    }

    // Solve for p_x, and refine our solution
    auto Hsolver = H_ldlt;  // H.fullPivLu();
    StateMat p_x = Hsolver.solve(-g);

    casadi_real old_cost = pState.calculateJ(x);
    constexpr double α_max = 1.0;
    double alpha = α_max;

    // Iterate until our chosen trial_x decreases our cost
    int alpha_refinement{0};
    FullStateMat trial_x = x;
    for (alpha_refinement = 0; alpha_refinement < 100; alpha_refinement++) {
      trial_x = x + alpha * p_x;

      casadi_real new_cost = pState.calculateJ(trial_x);

      // If f(xₖ + αpₖˣ) isn't finite, reduce step size immediately
      if (!std::isfinite(new_cost)) {
        // Reduce step size
        alpha *= 0.5;
        continue;
      }

      // Make sure we see an improvement
      if (new_cost < old_cost) {
        // Step accepted - update x
        x = trial_x;
        break;
      } else {
        alpha *= 0.5;

        // Safety factor for the minimal step size
        constexpr double α_min_frac = 0.05;
        constexpr double γConstraint = 1e-5;

        // If our step size shrank too much, report local infesibility
        if (alpha < α_min_frac * γConstraint) {
          return wpi::unexpected{
              sleipnir::SolverExitCondition::kLocallyInfeasible};
        }
      }
    }

    auto iter_end = wpi::Now();
    if constexpr (VERBOSE) {
      fmt::println(
          "{}: {} uS, ‖∇J‖={}, α={} ({} refinement steps), {} regularization "
          "steps",
          iter, iter_end - iter_start, g.norm(), alpha, alpha_refinement,
          i_reg);
      // fmt::println("∇J={}", g);
      // fmt::println("H={}", H);
      // fmt::println("p_x={}", p_x);
      // fmt::println("|Hp_x + ∇f|₂={}", (H * p_x + g).norm());
    }
  }
  if constexpr (VERBOSE) fmt::println("======================");

  return x;
}
