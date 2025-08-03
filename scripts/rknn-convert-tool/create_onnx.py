import subprocess
import sys
import argparse
import os.path

# This will work for all models that don't use anchors (e.g. all YOLO models except YOLOv5/v7)
# This includes YOLOv5u
yolo_non_anchor_repo = "https://github.com/airockchip/ultralytics_yolo11"

# For original YOLOv5 models
yolov5_repo = "https://github.com/airockchip/yolov5"

valid_yolo_versions = ["yolov5", "yolov8", "yolov11"]
comma_sep_yolo_versions = ", ".join(valid_yolo_versions)

ultralytics_folder_name_yolov5 = "airockchip_yolo_pkg_yolov5"
ultralytics_default_folder_name = "airockchip_yolo_pkg"


def print_bad_model_msg():
    print("This is usually due to passing in the wrong model version.")
    print("Please make sure you have the right model version and try again.")


def check_git_installed():
    try:
        subprocess.run(["git", "--version"]).check_returncode()
    except:
        print("Git is not installed or not found in your PATH.")
        print("Please install Git from https://git-scm.com/downloads and try again.")
        sys.exit(1)


def check_or_clone_rockchip_repo(repo_url, repo_name=ultralytics_default_folder_name):
    if os.path.exists(repo_name):
        print(
            f'Existing Rockchip repo "{repo_name}" detected, skipping installation...'
        )
    else:
        print(f'Cloning Rockchip repo to "{repo_name}"')
        try:
            subprocess.run(["git", "clone", repo_url, repo_name]).check_returncode()
        except subprocess.CalledProcessError as e:
            print("Failed to clone Rockchip repo, see error output below")
            print(e.output)
            exit(1)


def run_pip_install_or_else_exit(args):
    print("Running pip install...")

    try:
        subprocess.run(["pip", "install"] + args).check_returncode()
    except subprocess.CalledProcessError as e:
        print("Pip install rockchip repo failed, see error output")
        print(e.output)
        sys.exit(1)


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
            capture_output=True,
            text=True,
        ).check_returncode()
    except subprocess.CalledProcessError as e:
        print("Failed to run YOLOv5 export, see output below")
        output_string = (e.stdout or "") + (e.stderr or "")
        print(output_string)

        is_bad_model = False

        if "ModuleNotFoundError" in output_string and "ultralytics" in output_string:
            print(
                "It seems the YOLOv5 repo could not find an ultralytics installation."
            )
            is_bad_model = True
        elif (
            "AttributeError" in output_string
            and "_register_detect_seperate" in output_string
        ):
            print("It seems that you received a model attribute error.")
            is_bad_model = True

        if is_bad_model:
            print_bad_model_msg()

        exit(1)


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
        print(e)
        print()
        print()
        if "originally trained" in str(e):
            print("Ultralytics has detected that this model is invalid.")
            print_bad_model_msg()
        exit(1)
    except:
        exit(1)


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

    if args.version.lower() == "yolov5":
        run_onnx_conversion_yolov5(args.model_path)
    else:
        run_onnx_conversion_no_anchor(args.model_path)

    print(
        "Model export finished. Please use the generated ONNX file to convert to RKNN."
    )
