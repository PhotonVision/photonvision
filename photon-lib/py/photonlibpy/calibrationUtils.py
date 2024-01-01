import base64
from dataclasses import dataclass
import io
import json
import os
from typing import List, TypedDict
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
    data: List[float]


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
    locationInObjectSpace: List[Point3]
    # Observed location in pixel space
    locationInImageSpace: List[Point2]
    # (measured location in pixels) - (expected from FK)
    reprojectionErrors: List[Point2]
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
    cameraExtrinsics: JsonMatOfDoubles
    observations: List[Observation]


filename = "/home/matt/Downloads/photon_calibration_Microsoft_LifeCam_HD-3000_800x600.json"
output_folder = "photon_calibration_Microsoft_LifeCam_HD-3000_800x600"

with open(filename, "r") as cal_json:

    class Generic:
        @classmethod
        def from_dict(cls, dict):
            obj = cls()
            obj.__dict__.update(dict)
            return obj

    camera_cal_data: CameraCalibration = json.loads(cal_json.read(), object_hook=Generic.from_dict)

    out_dir = f"{output_folder}"
    if not os.path.exists(out_dir):
        os.makedirs(out_dir)

    for obs in camera_cal_data.observations:
        image = obs.snapshotData.data
        decoded_data = base64.b64decode(image)
        np_data = np.frombuffer(decoded_data,np.uint8)
        img = cv2.imdecode(np_data,cv2.IMREAD_UNCHANGED)
        cv2.imwrite(f"{out_dir}/{obs.snapshotName}", img)

    with open(f"{out_dir}/corners.vnl", "w+") as vnl_file:
        vnl_file.write("# filename x y level\n")

        for obs in camera_cal_data.observations:
            for corner in obs.locationInImageSpace:
                # Always level zero
                vnl_file.write(f"{obs.snapshotName} {corner.x} {corner.y} 0\n")

        vnl_file.flush()
