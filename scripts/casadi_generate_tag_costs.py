#!/usr/bin/env python3

from pathlib import Path

import casadi as ca
from casadi import *
from numpy import *


def generate_costs(num_tags, robot_heading_free):

    # Decision variables
    robot_x = ca.SX.sym("robot_x")
    robot_y = ca.SX.sym("robot_y")
    robot_z = 0  # Fixed at 0
    robot_θ = ca.SX.sym("robot_θ")

    # External gyro measurement - potentially unused
    gyro_θ = ca.SX.sym("gyro_θ")
    gyro_error_scale_fac = ca.SX.sym("gyro_r")

    # Precompute trigonometric functions
    sinθ = ca.sin(robot_θ)
    cosθ = ca.cos(robot_θ)

    # Transformation matrices
    field2robot = ca.vertcat(
        ca.horzcat(cosθ, -sinθ, 0, robot_x),
        ca.horzcat(sinθ, cosθ, 0, robot_y),
        ca.horzcat(0, 0, 1, robot_z),
        ca.horzcat(0, 0, 0, 1),
    )

    robot2camera = ca.SX.sym("robot2camera", 4, 4)

    field2camera = field2robot @ robot2camera

    # 4 corners per tag
    NUM_LANDMARKS = 4 * num_tags

    # Points in the field (homogeneous coordinates). Rows are [x, y, z, 1]
    field2points = ca.SX.sym("field2landmark", 4, NUM_LANDMARKS)

    # Observed points in the image
    point_observations = ca.SX.sym("observations_px", 2, NUM_LANDMARKS)

    # landmarks in camera frame
    camera2field = ca.inv(field2camera)
    camera2point = camera2field @ field2points

    # Camera frame coordinates
    x = camera2point[0, :]
    y = camera2point[1, :]
    z = camera2point[2, :]

    # Observed coordinates
    # Note that instead of using camera calibration, we expect the caller to provide
    # "normalized pixel coordinates". Convert from (u, v) coordinates to normalized
    # (x'', y'') coordinates with:
    # x'' = (u - c_x) / f_x
    # y'' = (u - c_y) / f_y
    xʼʼ_observed = point_observations[0, :]
    yʼʼ_observed = point_observations[1, :]

    # Where we expected to see the landmarks at, in normalized pixel coordinates
    xʼʼ = x / z
    yʼʼ = y / z

    # Reprojection error
    xʼʼ_err = xʼʼ - xʼʼ_observed
    yʼʼ_err = yʼʼ - yʼʼ_observed

    # Frobenius norm - sqrt(sum squared of each component). Square to remove sqrt
    J = ca.norm_fro(xʼʼ_err) ** 2 + ca.norm_fro(yʼʼ_err) ** 2

    # And penalize gyro error excursion
    if not robot_heading_free:
        J += gyro_error_scale_fac * ((gyro_θ - robot_θ) ** 2)

    x_vec = ca.vertcat(robot_x, robot_y, robot_θ)

    # Hessian + gradient
    hess_J, _ = ca.hessian(J, x_vec)
    grad_J = ca.gradient(J, x_vec)

    # Cost, plus grad and hessian of cost
    func_base_name = f"J_{num_tags}_tags{'_heading_free' if robot_heading_free else '_heading_fixed'}"
    func_inputs = [
        robot_x,
        robot_y,
        robot_θ,
        # fx,
        # fy,
        # cx,
        # cy,
        robot2camera,
        field2points,
        point_observations,
        gyro_θ,
        gyro_error_scale_fac,
    ]
    func_input_names = [
        "robot_x",
        "robot_y",
        "robot_θ",
        # "fx",
        # "fy",
        # "cx",
        # "cy",
        "robot2camera",
        "field2points",
        "point_observations",
        "gyro_θ",
        "gyro_error_scale_fac",
    ]

    J_func = ca.Function(
        f"calc_{func_base_name}",
        func_inputs,
        [J],
        func_input_names,
        ["J"],
    )
    grad_func = ca.Function(
        f"calc_grad{func_base_name}",
        func_inputs,
        [grad_J],
        func_input_names,
        ["grad_J"],
    )
    hess_func = ca.Function(
        f"calc_hess{func_base_name}",
        func_inputs,
        [hess_J],
        func_input_names,
        ["hess_J"],
    )

    cg = CodeGenerator(
        f"constrained_solvepnp_{num_tags}_tags_{'free' if robot_heading_free else 'fixed'}",
        {
            "with_header": True,
            "cpp": False,
        },
    )

    cg.add(J_func)
    cg.add(grad_func)
    cg.add(hess_func)
    output_dir = str(
        Path(__file__).parent.parent
        / "photon-targeting"
        / "src"
        / "main"
        / "native"
        / "cpp"
        / "photon"
        / "constrained_solvepnp"
        / "generate"
    )
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    cg.generate(output_dir + os.path.sep)

    return J, grad_J, hess_J


if __name__ == "__main__":
    for i in range(1, 11):
        for j in [True, False]:
            print(f"{i} tags, {j}")
            generate_costs(i, j)
