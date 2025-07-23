import subprocess
import sys
import argparse
import os.path

yolo_git_repos = {
    "yolov5": "https://github.com/airockchip/yolov5",
    "yolov8": "https://github.com/airockchip/ultralytics_yolov8",
    "yolov11": "https://github.com/airockchip/ultralytics_yolo11",
}

valid_yolo_version = list(yolo_git_repos.keys())
comma_sep_yolo_versions = ", ".join(valid_yolo_version)

ultralytics_folder_name = "airockchip_yolo_pkg"


def check_git_installed():
    try:
        subprocess.run(["git", "--version"]).check_returncode()
    except:
        print("Git is not installed or not found in your PATH.")
        print("Please install Git from https://git-scm.com/downloads and try again.")
        sys.exit(1)


def run_onnx_conversion(version, model_path):
    rc_repo = yolo_git_repos[version]
    
    if rc_repo is None:
        # achievement: how did we get here?
        print(
            f"Invalid yolo version \"{version}\" must be one of the following {comma_sep_yolo_versions}"
        )

    if os.path.exists(ultralytics_folder_name):
        print("Existing Rockchip Repo detected, no install required")
    else:
        print("Cloning Rockchip repo...")
        
        try:
            subprocess.run(
                ["git", "clone", rc_repo, ultralytics_folder_name]
            ).check_returncode()
        except subprocess.CalledProcessError as e:
            print("Failed to clone rockchip repo, see error output below")
            print(e.output)
            sys.exit(1)
        
    print("Running pip install...")
    try: 
      subprocess.run(["pip", "install", "-e", ultralytics_folder_name]).check_returncode()
    except subprocess.CalledProcessError as e:
      print("Pip install rockchip repo failed, see error output")
      print(e.output)
      sys.exit(1)
        
    from ultralytics import YOLO

    model = YOLO(model_path)
    model.export(format="rknn")
    


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Generate valid ONNX file for yolo model"
    )
    
    parser.add_argument(
        "-v",
        "--version",
        choices=valid_yolo_version,
        required=True,
        help=(f"YOLO version to use. Must be one of: {comma_sep_yolo_versions}"),
    )
    
    parser.add_argument(
        "-m",
        "--model_path",
        required=True,
        help=(f"Path to YOLO model"),
    )

    args = parser.parse_args()

    check_git_installed()
    run_onnx_conversion(args.version, args.model_path)
