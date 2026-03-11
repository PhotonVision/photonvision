#include "constrained_pnp.h"

#include <sleipnir/optimization/OptimizationProblem.hpp>
#include <sleipnir/autodiff/VariableMatrix.hpp>
#include <sleipnir/autodiff/Variable.hpp>
#include <sleipnir/autodiff/VariableBlock.hpp>
#include <iostream>
#include <type_traits>

// Returns the value of x which minimizes 
// 
//   y = a_4 * x^4 + a_3 * x^3 + a_2 * x^2 + a_1 * x + a_0. 
// 
// Note we assume the polynomial has a finite value for the minimum.
double minimize_quartic(double a0, double a1, double a2, double a3, double a4) {
  double a = 4 * a4;
  double b = 3 * a3;
  double c = 2 * a2;
  double d = a1;

  double p = (3 * a * c - b * b) / (3 * a * a);
  double q = (2 * b * b * b - 9 * a * b * c + 27 * a * a * d) / (27 * a * a * a);

  double temp = std::sqrt(q * q / 4 + p * p * p / 27);
  double u1 = -q * 0.5 + temp;
  double u2 = -q * 0.5 - temp;

  double root1 = std::cbrt(u1) + std::cbrt(u2);
  double min_x = root1 - b / (3 * a);
  double min_y = a0 + a1 * min_x + a2 * min_x * min_x + a3 * min_x * min_x * min_x + a4 * min_x * min_x * min_x * min_x;

  // Get other two roots
  double A = 1;
  double B = root1;
  double C = p + root1 * root1;

  double discriminant = B * B - 4 * A * C;

  if (discriminant >= 0) {
    double sqrt_disc = std::sqrt(discriminant);
    double root2 = (-B + sqrt_disc) / (2 * A);
    double root3 = (-B - sqrt_disc) / (2 * A);

    double x2 = root2 - b / (3 * a);
    double x3 = root3 - b / (3 * a);

    double y2 = a0 + a1 * x2 + a2 * x2 * x2 + a3 * x2 * x2 * x2 + a4 * x2 * x2 * x2 * x2;
    double y3 = a0 + a1 * x3 + a2 * x3 * x3 + a3 * x3 * x3 * x3 + a4 * x3 * x3 * x3 * x3;

    if (y2 < min_y) {
      min_y = y2;
      min_x = x2;
    }

    if (y3 < min_y) {
      return x3;
    }
    
    return min_x;

  } else {
    return min_x;
  }
}

frc::Pose2d cpnp::solve_naive(const ProblemParams & params)
{
  using namespace cpnp;
  using namespace sleipnir;

  // Convert the points from the WPI coordinate system (NWU) to OpenCV's coordinate system 
  // (EDN).
  // Eigen::Matrix<double, 4, 4> nwu_to_edn;
  // nwu_to_edn <<
  //   0, -1,  0,  0,
  //   0,  0, -1,  0,
  //   1,  0,  0,  0,
  //   0,  0,  0,  1;
  // auto world_points_opencv = nwu_to_edn * params.worldPoints;

  OptimizationProblem problem{};

  // robot pose
  auto robot_x = problem.DecisionVariable();
  auto robot_z = problem.DecisionVariable();
  auto robot_θ = problem.DecisionVariable();

  robot_x.SetValue(-1.5);
  robot_z.SetValue(-1);
  robot_θ.SetValue(0);

  // Generate r_t
  // rotation about +Y plus pose
  auto sinθ = sleipnir::sin(robot_θ);
  auto cosθ = sleipnir::cos(robot_θ);
  VariableMatrix R_T{
      {cosθ, 0, sinθ, robot_x},
      {0, 1, 0, 0},
      {-sinθ, 0, cosθ, robot_z},
  };

  Eigen::Matrix<double, 3, 3> K;
  K << params.f_x, 0, params.c_x, 0, params.f_y, params.c_y, 0, 0, 1;

  // TODO - can i just do this whole matrix at once, one col per observation?
  // auto predicted_image_point = K * (R_T * world_points_opencv);

  // auto u_pred = sleipnir::CwiseReduce(predicted_image_point.Row(0), predicted_image_point.Row(2), std::divides<>{});
  // auto v_pred = sleipnir::CwiseReduce(predicted_image_point.Row(1), predicted_image_point.Row(2), std::divides<>{});

  Variable cost;
  // for (int i = 0; i < u_pred.Cols(); ++i) {
  //   auto E_x = u_pred(0, i) - params.imagePoints[2 * i];
  //   auto E_y = v_pred(0, i) - params.imagePoints[2 * i + 1];
  //   cost += E_x * E_x + E_y * E_y;
  // }

  fmt::println("Initial cost: {}", cost.Value());
  // fmt::println("Predicted corners:\n{}\n{}", u_pred.Value(), v_pred.Value());

  problem.Minimize(cost);

  auto status = problem.Solve({.diagnostics=false});
  
  fmt::println("Optimization state: {}", static_cast<int>(status.exitCondition));
  fmt::println("Final cost: {}", cost.Value());

  frc::Pose3d pose{frc::Translation3d{units::meter_t{robot_x.Value()}, units::meter_t{0}, units::meter_t(robot_z.Value())}, 
                   frc::Rotation3d{units::radian_t{0}, units::radian_t{robot_θ.Value()}, units::radian_t{0}}};

  Eigen::Matrix3d transform;
  transform <<
    0, 0, 1,
    -1, 0, 0,
    0, -1, 0;
  frc::Rotation3d edn_to_nwu{transform};
  frc::Pose3d nwu_pose{pose.Translation().RotateBy(edn_to_nwu), 
                       -edn_to_nwu + pose.Rotation() + edn_to_nwu};

  frc::Pose3d inv_pose{-nwu_pose.Translation().RotateBy(-nwu_pose.Rotation()),
                       -nwu_pose.Rotation()};

  return inv_pose.ToPose2d();
}

frc::Pose2d cpnp::solve_polynomial(const ProblemParams& params) {
  using namespace cpnp;

  // The algorithm works as follows
  // 
  //   1. Formulate a cost function using a change of variables:
  // 
  //       tau = tan(theta / 2)
  //       x' = x * (1 + tau * tau)
  //       z' = z * (1 + tau * tau)
  // 
  //      Note this cost function is a fourth-order polynomial in terms of tau and second-
  //      order in terms of x' and y'.
  //   2. Solve for x' and y' in terms of tau and eliminate them so the cost function 
  //      becomes a fourth-order polynomial in terms of tau.
  //   3. Minimize the polynomial. This is possible by taking the derivative and finding
  //      the roots of the resulting third-degree polynomial. There is a closed form
  //      solution for finding the roots of cubic polynomials with Carbano's formula 
  //      (fancy quadratic formula).
  //   4. Undo the change of variables and solve for theta, x, and z in terms of tau.
  //   5. Undo the opencv transform and invert the transform.
  int N = params.imagePoints.size() / 2;

  double inv_f_x = 1 / params.f_x;
  double inv_f_y = 1 / params.f_y;
  double c_x = params.c_x;
  double c_y = params.c_y;

  // std::vector<double> imagePoints(2 * N);
  // std::vector<double> worldPoints(3 * N);
  // for (int i = 0; i < N; i++) {
  //   imagePoints[2 * i] = params.imagePoints(0, i);
  //   imagePoints[2 * i + 1] = params.imagePoints(1, i);
  //   worldPoints[3 * i] = params.worldPoints(0, i);
  //   worldPoints[3 * i + 1] = params.worldPoints(1, i);
  //   worldPoints[3 * i + 2] = params.worldPoints(2, i);
  // }

  // Step 1
  // ok this looks unreadable but i swear it makes sense
  // auto t0 = std::chrono::high_resolution_clock::now();
  double a_400 = 0;
  double a_300 = 0;
  double a_200 = 0;
  double a_210 = 0;
  double a_201 = 0;
  double a_100 = 0;
  double a_110 = 0;
  double a_101 = 0;
  double a_020 = 0;
  double a_010 = 0;
  double a_011 = 0;
  double a_002 = 0;
  double a_001 = 0;
  double a_000 = 0;

  for (int i = 0; i < N; i++) {
    // Convert image points to normalized points
    double p_x = params.imagePoints[2 * i];
    double p_y = params.imagePoints[2 * i + 1];
    double u = (p_x - c_x) * inv_f_x;
    double v = (p_y - c_y) * inv_f_y;

    // We do the opencv coordinate transform here to avoid an extra matrix multiply.
    double Z = params.worldPoints[3 * i];
    double X = -params.worldPoints[3 * i + 1];
    double Y = -params.worldPoints[3 * i + 2];

    double Ax_tau = -Z * u + X;
    double Bx_tau = -2 * (X * u + Z);
    double Cx_tau = Z * u - X;

    double Ay_tau = -Z * v - Y;
    double By_tau = -2 * X * v;
    double Cy_tau = Z * v - Y;

    double Ax_xp = -1;
    double Ax_zp = u;

    double Ay_xp = 0;
    double Ay_zp = v;

    // Add the components from the x residual
    a_400 += Ax_tau * Ax_tau + Ay_tau * Ay_tau;
    a_300 += 2 * (Ax_tau * Bx_tau + Ay_tau * By_tau);
    a_200 += 2 * (Ax_tau * Cx_tau + Ay_tau * Cy_tau) + Bx_tau * Bx_tau + By_tau * By_tau;
    a_210 += 2 * (Ax_tau * Ax_xp + Ay_tau * Ay_xp);
    a_201 += 2 * (Ax_tau * Ax_zp + Ay_tau * Ay_zp);
    a_100 += 2 * (Bx_tau * Cx_tau + By_tau * Cy_tau);
    a_110 += 2 * (Bx_tau * Ax_xp + By_tau * Ay_xp);
    a_101 += 2 * (Bx_tau * Ax_zp + By_tau * Ay_zp);
    a_020 += Ax_xp * Ax_xp + Ay_xp * Ay_xp;
    a_010 += 2 * (Ax_xp * Cx_tau + Ay_xp * Cy_tau);
    a_011 += 2 * (Ax_xp * Ax_zp + Ay_xp * Ay_zp);
    a_002 += Ax_zp * Ax_zp + Ay_zp * Ay_zp;
    a_001 += 2 * (Ax_zp * Cx_tau + Ay_zp * Cy_tau);
    a_000 += Cx_tau * Cx_tau + Cy_tau * Cy_tau;
  }
  // auto t1 = std::chrono::high_resolution_clock::now();


  // Step 2. We want to find the optimal x' and z' value for each value of theta.
  // 
  // Taking the derivative of the cost function c(x, y, z) and setting it zero gives
  // 
  //   d/dy c(x,y,z) = a_210 * x² + a_110 * x + 2 * a_020 * y + a_010 + a_011 * z = 0
  //   d/dz c(x,y,z) = a_201 * x² + a_101 * x + 2 * a_002 * z + a_001 + a_011 * y = 0
  // 
  // Next we solve for y and z in terms of x in a linear system
  // 
  //   [2 * a_020    a_011  ][y] = [-(a_210 * x² + a_110 * x + a_010)]  
  //   [  a_011    2 * a_002][z]   [-(a_201 * x² + a_101 * x + a_001)]
  // 
  // This gives
  // 
  //   y = 1 / (4 * a_020 * a_002 - a_011 * a_011) * ((2 * a_002) * (-(a_210 * x² + a_110 * x + a_010)) + (-a_011) * (-(a_201 * x² + a_101 * x + a_001)))
  //   z = 1 / (4 * a_020 * a_002 - a_011 * a_011) * ((-a_011) * (-(a_210 * x² + a_110 * x + a_010)) + (2 * a_020) * (-(a_201 * x² + a_101 * x + a_001)))
  // 
  // Finally we can simplify this into constants
  // 
  //   y = A_y * x^2 + B_y * x + C_y
  //   z = A_z * x^2 + B_z * x + C_z
  double inv_det = 1 / (4 * a_020 * a_002 - a_011 * a_011);

  double A_y = (-2 * a_002 * a_210 + a_011 * a_201) * inv_det;
  double B_y = (-2 * a_002 * a_110 + a_011 * a_101) * inv_det;
  double C_y = (-2 * a_002 * a_010 + a_011 * a_001) * inv_det;

  double A_z = (a_011 * a_210 - 2 * a_020 * a_201) * inv_det;
  double B_z = (a_011 * a_110 - 2 * a_020 * a_101) * inv_det;
  double C_z = (a_011 * a_010 - 2 * a_020 * a_001) * inv_det;

  // Substituting back in gives
  double b_4 = a_400 + a_210 * A_y + a_201 * A_z + a_020 * (A_y * A_y) + a_011 * (A_y * A_z) + a_002 * (A_z * A_z);
  double b_3 = a_300 + 
               a_210 * B_y + 
               a_201 * B_z + 
               a_110 * A_y + 
               a_101 * A_z + 
               a_011 * (A_y * B_z + B_y * A_z) + 
               a_002 * (A_z * B_z + B_z * A_z) +
               a_020 * (A_y * B_y + B_y * A_y);
  double b_2 = a_200 +
               a_210 * C_y +
               a_201 * C_z +
               a_110 * B_y +
               a_101 * B_z +
               a_020 * (B_y * B_y + A_y * C_y + C_y * A_y) +
               a_010 * A_y +
               a_011 * (A_y * C_z + B_y * B_z + C_y * A_z) +
               a_002 * (B_z * B_z + A_z * C_z + C_z * A_z) +
               a_001 * A_z;
  double b_1 = a_100 +
               a_020 * (B_y * C_y + C_y * B_y) +
               a_110 * C_y +
               a_101 * C_z +
               a_010 * B_y +
               a_011 * (B_y * C_z + C_y * B_z) +
               a_002 * (B_z * C_z + C_z * B_z) +
               a_001 * B_z;
  double b_0 = a_020 * (C_y * C_y) +
               a_010 * C_y +
               a_011 * (C_y * C_z) +
               a_002 * (C_z * C_z) +
               a_001 * C_z +
               a_000;
  
  // auto t2 = std::chrono::high_resolution_clock::now();

  // Step 5
  double tau = minimize_quartic(b_0, b_1, b_2, b_3, b_4);

  // auto t3 = std::chrono::high_resolution_clock::now();

  // Step 6
  double x_prime = A_y * tau * tau + B_y * tau + C_y;
  double z_prime = A_z * tau * tau + B_z * tau + C_z;
  double x = x_prime / (1 + tau * tau);
  double z = z_prime / (1 + tau * tau);
  double theta = 2 * atan(tau);

  // auto t4 = std::chrono::high_resolution_clock::now();

  // Manually writing out the math instead of using wpilib geometry objects.
  double nwu_x = z;
  double nwu_y = -x;
  double nwu_theta = -theta;

  double ncos = cos(-nwu_theta);
  double nsin = sin(-nwu_theta);

  // auto t6 = std::chrono::high_resolution_clock::now();

  // fmt::println("Time 1: {}ms", std::chrono::duration_cast<std::chrono::nanoseconds>(t1 - t0).count() / 1e6);
  // fmt::println("Time 2: {}ms", std::chrono::duration_cast<std::chrono::nanoseconds>(t2 - t1).count() / 1e6);
  // fmt::println("Time 3: {}ms", std::chrono::duration_cast<std::chrono::nanoseconds>(t3 - t2).count() / 1e6);
  // fmt::println("Time 4: {}ms", std::chrono::duration_cast<std::chrono::nanoseconds>(t4 - t3).count() / 1e6);


  return frc::Pose2d{units::meter_t{-(ncos * nwu_x - nsin * nwu_y)}, 
                     units::meter_t{-(nsin * nwu_x + ncos * nwu_y)},
                     units::radian_t{-nwu_theta}};
}