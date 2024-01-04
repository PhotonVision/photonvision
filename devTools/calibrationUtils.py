import base64
from dataclasses import dataclass
import json
import os
import cv2
import numpy as np


@dataclass
class Resolution:
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
    resolution: Resolution
    cameraIntrinsics: JsonMatOfDoubles
    distCoeffs: JsonMatOfDoubles
    observations: list[Observation]


def convert_photon_to_mrcal(photon_cal_json_path: str, output_folder: str):
    """
    Unpack a Photon calibration JSON (eg, photon_calibration_Microsoft_LifeCam_HD-3000_800x600.json) into
    the output_folder directory with images and corners.vnl file for use with mrcal.
    """
    with open(photon_cal_json_path, "r") as cal_json:
        # Convert to nested objects instead of nameddicts on json-loads
        class Generic:
            @classmethod
            def from_dict(cls, dict):
                obj = cls()
                obj.__dict__.update(dict)
                return obj

        camera_cal_data: CameraCalibration = json.loads(
            cal_json.read(), object_hook=Generic.from_dict
        )

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
