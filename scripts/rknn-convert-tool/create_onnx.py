import argparse
import os.path
import subprocess
import sys

# This will work for all models that don't use anchors (e.g. all YOLO models except YOLOv5/v7)
# This includes YOLOv5u
yolo_non_anchor_repo = "https://github.com/airockchip/ultralytics_yolo11"

# For original YOLOv5 models
yolov5_repo = "https://github.com/airockchip/yolov5"

valid_yolo_versions = ["yolov5", "yolov8", "yolov11"]
comma_sep_yolo_versions = ", ".join(valid_yolo_versions)

ultralytics_folder_name_yolov5 = "airockchip_yolo_pkg_yolov5"
ultralytics_default_folder_name = "airockchip_yolo_pkg"

bad_model_msg = """
This is usually due to passing in the wrong model version.
Please make sure you have the right model version and try again.
"""


def print_bad_model_msg(cause):
    print(f"{cause}{bad_model_msg}")


def run_and_exit_with_error(cmd, error_msg, enable_error_output=True):
    try:
        if enable_error_output:
            subprocess.run(
                cmd,
                stderr=subprocess.STDOUT,
                stdout=subprocess.PIPE,
                universal_newlines=True,
            ).check_returncode()
        else:
            subprocess.run(cmd).check_returncode()
    except subprocess.CalledProcessError as e:
        print(error_msg)

        if enable_error_output:
            print(e.stdout)

        sys.exit(1)


def check_git_installed():
    run_and_exit_with_error(
        ["git", "--version"],
        """Git is not installed or not found in your PATH.
Please install Git from https://git-scm.com/downloads and try again.""",
    )


def check_or_clone_rockchip_repo(repo_url, repo_name=ultralytics_default_folder_name):
    if os.path.exists(repo_name):
        print(
            f'Existing Rockchip repo "{repo_name}" detected, skipping installation...'
        )
    else:
        print(f'Cloning Rockchip repo to "{repo_name}"')
        run_and_exit_with_error(
            ["git", "clone", repo_url, repo_name],
            "Failed to clone Rockchip repo, please see error output",
        )


def run_pip_install_or_else_exit(args):
    print("Running pip install...")
    run_and_exit_with_error(
        ["pip", "install"] + args,
        "Pip install rockchip repo failed, please see error output",
    )


def run_onnx_conversion_yolov5(model_path):
    check_or_clone_rockchip_repo(yolov5_repo, ultralytics_folder_name_yolov5)
    run_pip_install_or_else_exit(
        [
            "-r",
            os.path.join(ultralytics_folder_name_yolov5, "requirements.txt"),
            "torch<2.6.0",
            "onnx",
        ]
    )

    model_abspath = os.path.abspath(model_path)

    try:
        subprocess.run(
            [
                "python",
                f"{ultralytics_folder_name_yolov5}/export.py",
                "--weights",
                model_abspath,
                "--rknpu",
                "--include",
                "onnx",
            ],
            stderr=subprocess.STDOUT,
            stdout=subprocess.PIPE,
            universal_newlines=True,
        ).check_returncode()
    except subprocess.CalledProcessError as e:
        print("Failed to run YOLOv5 export, please see error output")

        if "ModuleNotFoundError" in e.stdout and "ultralytics" in e.stdout:
            print_bad_model_msg(
                "It seems the YOLOv5 repo could not find an ultralytics installation."
            )
        elif "AttributeError" in e.stdout and "_register_detect_seperate" in e.stdout:
            print_bad_model_msg("It seems that you received a model attribute error.")
        else:
            print("Unknown Error when converting:")
            print(e.stdout)

        sys.exit(1)


def run_onnx_conversion_no_anchor(model_path):
    check_or_clone_rockchip_repo(yolo_non_anchor_repo)
    run_pip_install_or_else_exit(["-e", ultralytics_default_folder_name, "onnx"])

    sys.path.insert(0, os.path.abspath(ultralytics_default_folder_name))
    model_abs_path = os.path.abspath(model_path)

    from ultralytics import YOLO

    try:
        model = YOLO(model_abs_path)
        model.export(format="rknn")
    except TypeError as e:
        if "originally trained" in str(e):
            print_bad_model_msg(
                "Ultralytics has detected that this model is a YOLOv5 model."
            )
        else:
            raise e

        sys.exit(1)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Generate valid ONNX file for yolo model"
    )

    parser.add_argument(
        "-m",
        "--model_path",
        required=True,
        help=(f"Path to YOLO model"),
    )

    parser.add_argument(
        "-v",
        "--version",
        required=True,
        choices=valid_yolo_versions,
        help=(f"Model version, must be one of: {comma_sep_yolo_versions}"),
    )

    args = parser.parse_args()

    check_git_installed()

    try:
        if args.version.lower() == "yolov5":
            run_onnx_conversion_yolov5(args.model_path)
        else:
            run_onnx_conversion_no_anchor(args.model_path)

        print(
            "Model export finished. Please use the generated ONNX file to convert to RKNN."
        )
    except SystemExit:
        print("Model export failed. Please see output above.")
