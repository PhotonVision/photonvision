import argparse
import base64
import os
import sys
from typing import Union
import cv2
import numpy as np
import mrcal
from wpimath.geometry import Quaternion as _Quat

from pydantic.dataclasses import dataclass
from pydantic import TypeAdapter


@dataclass
class Size:
    width: int
    height: int


@dataclass
class JsonMatOfDoubles:
    rows: int
    cols: int
    type: int
    data: list[float]


@dataclass
class JsonMat:
    rows: int
    cols: int
    type: int
    data: str  # Base64-encoded PNG data

@dataclass
class Point2:
    x: float
    y: float


@dataclass
class Translation3d:
    x: float
    y: float
    z: float


@dataclass
class Quaternion:
    X: float
    Y: float
    Z: float
    W: float


@dataclass
class Rotation3d:
    quaternion: Quaternion


@dataclass
class Pose3d:
    translation: Translation3d
    rotation: Rotation3d


@dataclass
class Point3:
    x: float
    y: float
    z: float


@dataclass
class Observation:
    # Expected feature 3d location in the camera frame
    locationInObjectSpace: list[Point3]
    # Observed location in pixel space
    locationInImageSpace: list[Point2]
    # (measured location in pixels) - (expected from FK)
    reprojectionErrors: list[Point2]
    # Solver optimized board poses
    optimisedCameraToObject: Pose3d
    # If we should use this observation when re-calculating camera calibration
    includeObservationInCalibration: bool
    snapshotName: str
    # The actual image the snapshot is from
    snapshotData: JsonMat


@dataclass
class CameraCalibration:
    resolution: Size
    cameraIntrinsics: JsonMatOfDoubles
    distCoeffs: JsonMatOfDoubles
    observations: list[Observation]
    calobjectWarp: list[float]
    calobjectSize: Size
    calobjectSpacing: float

def __convert_cal_to_mrcal_cameramodel(
    cal: CameraCalibration,
) -> mrcal.cameramodel | None:
    if len(cal.distCoeffs.data) == 5:
        model = "LENSMODEL_OPENCV5"
    elif len(cal.distCoeffs.data) == 8:
        model = "LENSMODEL_OPENCV8"
    else:
        print("Unknown camera model? giving up")
        return None

    def opencv_to_mrcal_intrinsics(ocv):
        return [ocv[0], ocv[4], ocv[2], ocv[5]]

    def pose_to_rt(pose: Pose3d):
        r = _Quat(
            w=pose.rotation.quaternion.W,
            x=pose.rotation.quaternion.X,
            y=pose.rotation.quaternion.Y,
            z=pose.rotation.quaternion.Z,
        ).toRotationVector()
        t = [
            pose.translation.x,
            pose.translation.y,
            pose.translation.z,
        ]
        return np.concatenate((r, t))

    imagersize = (cal.resolution.width, cal.resolution.height)

    # Always weight=1 for Photon data
    WEIGHT = 1
    observations_board = np.array(
        [
            # note that we expect row-major observations here. I think this holds
            np.array(
                list(map(lambda it: [it.x, it.y, WEIGHT], o.locationInImageSpace))
            ).reshape((cal.calobjectSize.width, cal.calobjectSize.height, 3))
            for o in cal.observations
        ]
    )

    optimization_inputs = {
        "intrinsics": np.array(
            [
                opencv_to_mrcal_intrinsics(cal.cameraIntrinsics.data)
                + cal.distCoeffs.data
            ],
            dtype=np.float64,
        ),
        "extrinsics_rt_fromref": np.zeros((0, 6), dtype=np.float64),
        "frames_rt_toref": np.array(
            [pose_to_rt(o.optimisedCameraToObject) for o in cal.observations]
        ),
        "points": None,
        "observations_board": observations_board,
        "indices_frame_camintrinsics_camextrinsics": np.array(
            [[i, 0, -1] for i in range(len(cal.observations))], dtype=np.int32
        ),
        "observations_point": None,
        "indices_point_camintrinsics_camextrinsics": None,
        "lensmodel": model,
        "imagersizes": np.array([imagersize], dtype=np.int32),
        "calobject_warp": np.array(cal.calobjectWarp)
        if len(cal.calobjectWarp) > 0
        else None,
        # We always do all the things
        "do_optimize_intrinsics_core": True,
        "do_optimize_intrinsics_distortions": True,
        "do_optimize_extrinsics": True,
        "do_optimize_frames": True,
        "do_optimize_calobject_warp": len(cal.calobjectWarp) > 0,
        "do_apply_outlier_rejection": True,
        "do_apply_regularization": True,
        "verbose": False,
        "calibration_object_spacing": cal.calobjectSpacing,
        "imagepaths": np.array([it.snapshotName for it in cal.observations]),
    }

    return mrcal.cameramodel(
        optimization_inputs=optimization_inputs,
        icam_intrinsics=0,
    )

def _photon_cal_from_json(photon_cal_json_path: str):
    with open(photon_cal_json_path, "r") as cal_json:
        return TypeAdapter(CameraCalibration).validate_json(cal_json.read())

def _photon_cal_to_json(cal: CameraCalibration):
    return TypeAdapter(CameraCalibration).dump_json(cal).decode("utf-8")

def _observationJsonSize(obs: Observation):
    return len(TypeAdapter(Observation).dump_json(obs).decode("utf-8"))

def _calibrationJsonSize(cal: CameraCalibration):
    return len(TypeAdapter(CameraCalibration).dump_json(cal).decode("utf-8"))

def downscale_calibration(camera_cal_data: CameraCalibration, size_mb: int = 50) -> CameraCalibration:
    """
    Remove some image data to force the file size to be less than size_mb. This is the size
    required by PV import.
    """
    original_observations = camera_cal_data.observations
    trimmed_observations: list[Observation] = []
    camera_cal_data.observations = trimmed_observations

    # Use 1000 * 1000 instead of 1024 * 1024 as this makes PV happy when going to 50'MB'
    bytes_remaining = size_mb * 1000 * 1000 - _calibrationJsonSize(camera_cal_data)

    for obs in original_observations:
        bytes_remaining -= _observationJsonSize(obs)
        if bytes_remaining < 0:
            break
        trimmed_observations.append(obs)

    camera_cal_data.observations = trimmed_observations
    return camera_cal_data

def upsert_mrcal_into_photon(photon_cal_json_path: str, mrcal_model_path: str) -> CameraCalibration:
    """
    Take intrinsics calculated my mrcal command line and insert them into an existing photon
    calibration. Be sure to use the same calibration file that generated the mrcal data.
    """
    def mrcal_to_opencv_intrinsics(mrcal_model):
        _, data = mrcal_model.intrinsics()
        data = data.tolist()
        intrinsics = data[0:4]
        diffCoeff = data[4:]
        return [intrinsics[0], 0.0, intrinsics[2], 0.0, intrinsics[1], intrinsics[3], 0.0, 0.0, 1.0], diffCoeff

    camera_cal_data: CameraCalibration = _photon_cal_from_json(photon_cal_json_path)
    model = mrcal.cameramodel(mrcal_model_path)
    intrinsics, dist_coeffs = mrcal_to_opencv_intrinsics(model)
    resolution = model.imagersize()

    # Likely need more checks here, but at least have one...
    assert camera_cal_data.resolution.width == resolution[0]
    assert camera_cal_data.resolution.height == resolution[1]

    camera_cal_data.distCoeffs.data = dist_coeffs
    camera_cal_data.cameraIntrinsics.data = intrinsics
    return camera_cal_data

def convert_photon_to_mrcal(photon_cal_json_path: str, output_folder: str):
    """
    Unpack a Photon calibration JSON (eg, photon_calibration_Microsoft_LifeCam_HD-3000_800x600.json) into
    the output_folder directory with images and corners.vnl file for use with mrcal.
    """
    camera_cal_data: CameraCalibration = _photon_cal_from_json(photon_cal_json_path)

    # Create output_folder if not exists
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)

    # Decode each image and save it as a png
    for obs in camera_cal_data.observations:
        image = obs.snapshotData.data
        decoded_data = base64.b64decode(image)
        np_data = np.frombuffer(decoded_data, np.uint8)
        img = cv2.imdecode(np_data, cv2.IMREAD_UNCHANGED)
        cv2.imwrite(f"{output_folder}/{obs.snapshotName}", img)

    # And create a VNL file for use with mrcal
    with open(f"{output_folder}/corners.vnl", "w+") as vnl_file:
        vnl_file.write("# filename x y level\n")

        for obs in camera_cal_data.observations:
            for corner in obs.locationInImageSpace:
                # Always level zero
                vnl_file.write(f"{obs.snapshotName} {corner.x} {corner.y} 0\n")

        vnl_file.flush()

    mrcal_model = __convert_cal_to_mrcal_cameramodel(camera_cal_data)

    with open(f"{output_folder}/camera-0.cameramodel", "w+") as mrcal_file:
        mrcal_model.write(
            mrcal_file,
            note="Generated from PhotonVision calibration file: "
            + photon_cal_json_path
            + "\nCalobject_warp (m): "
            + str(camera_cal_data.calobjectWarp),
        )

def main():
    parser = argparse.ArgumentParser(
        description="Developer tools to work with camera calibration in photonvision and mrcal"
    )
    parser.add_argument("input", type=str, help="Path to Photon calibration JSON file")
    parser.add_argument(
        "--mrcal-output", type=str, help="Output folder for mrcal VNL file + images"
    )
    parser.add_argument(
        "--pv-output", type=str, help="Remove images to shrink the photonvision calibration size, output filename"
    )
    parser.add_argument(
        "--shrink-size", type=int, help="Output folder for mrcal VNL file + images"
    )
    parser.add_argument(
        "--merge-model", type=str, help="Camera model file from mrcal to merge into photonvision calibration"
    )

    args = parser.parse_args()

    if len(sys.argv) == 2:
        parser.print_usage()

    if args.mrcal_output:
        convert_photon_to_mrcal(args.input, args.mrcal_output)

    if args.pv_output:
        if not (args.merge_model or args.shrink_size):
            print("Must provide either --merge-model or --shrink-size with --pv-output")
            return

        camera_cal_data: CameraCalibration = _photon_cal_from_json(args.input)
        if args.merge_model:
            camera_cal_data = upsert_mrcal_into_photon(args.input, args.merge_model)

        if args.shrink_size:
            camera_cal_data = downscale_calibration(camera_cal_data, args.shrink_size)

        with open(args.pv_output, "w") as f:
            f.write(_photon_cal_to_json(camera_cal_data))


if __name__ == "__main__":
    main()