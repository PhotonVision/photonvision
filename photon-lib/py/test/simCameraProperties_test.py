import math

import numpy as np
import pytest
from photonlibpy.estimation import RotTrlTransform3d
from photonlibpy.simulation import SimCameraProperties
from wpimath.geometry import Rotation2d, Translation3d


@pytest.fixture(autouse=True)
def scp() -> SimCameraProperties:
    props = SimCameraProperties()
    props.setCalibrationFromFOV(1000, 1000, fovDiag=Rotation2d(math.radians(90.0)))
    return props


def test_GetPixelYaw(scp) -> None:
    rot = scp.getPixelYaw(scp.getResWidth() / 2)
    assert rot.degrees() == pytest.approx(0.0, abs=1.0)
    rot = scp.getPixelYaw(0.0)
    # FOV is square
    assert rot.degrees() == pytest.approx(45.0 / math.sqrt(2.0), abs=5.0)
    rot = scp.getPixelYaw(scp.getResWidth())
    assert rot.degrees() == pytest.approx(-45.0 / math.sqrt(2.0), abs=5.0)


def test_GetPixelPitch(scp) -> None:
    rot = scp.getPixelPitch(scp.getResHeight() / 2)
    assert rot.degrees() == pytest.approx(0.0, abs=1.0)
    rot = scp.getPixelPitch(0.0)
    # FOV is square
    assert rot.degrees() == pytest.approx(-45.0 / math.sqrt(2.0), abs=5.0)
    rot = scp.getPixelPitch(scp.getResHeight())
    assert rot.degrees() == pytest.approx(45.0 / math.sqrt(2.0), abs=5.0)


def test_GetPixelRot(scp) -> None:
    rot = scp.getPixelRot(np.array([scp.getResWidth() / 2.0, scp.getResHeight() / 2.0]))
    assert rot.x_degrees == pytest.approx(0.0, abs=5)
    assert rot.y_degrees == pytest.approx(0.0, abs=5)
    assert rot.z_degrees == pytest.approx(0.0, abs=5)
    rot = scp.getPixelRot(np.array([0.0, 0.0]))
    assert rot.x_degrees == pytest.approx(0.0, abs=5)
    assert rot.y_degrees == pytest.approx(-45.0 / math.sqrt(2.0), abs=5)
    assert rot.z_degrees == pytest.approx(45.0 / math.sqrt(2.0), abs=5)
    rot = scp.getPixelRot(np.array([scp.getResWidth(), scp.getResHeight()]))
    assert rot.x_degrees == pytest.approx(0.0, abs=5)
    assert rot.y_degrees == pytest.approx(45.0 / math.sqrt(2.0), abs=5)
    assert rot.z_degrees == pytest.approx(-45.0 / math.sqrt(2.0), abs=5)


def test_GetCorrectedPixelRot(scp) -> None:
    rot = scp.getCorrectedPixelRot(
        np.array([scp.getResWidth() / 2.0, scp.getResHeight() / 2.0])
    )
    assert rot.x_degrees == pytest.approx(0.0, abs=5)
    assert rot.y_degrees == pytest.approx(0.0, abs=5)
    assert rot.z_degrees == pytest.approx(0.0, abs=5)
    rot = scp.getCorrectedPixelRot(np.array([0.0, 0.0]))
    assert rot.x_degrees == pytest.approx(0.0, abs=5)
    assert rot.y_degrees == pytest.approx(-45.0 / math.sqrt(2.0), abs=5)
    assert rot.z_degrees == pytest.approx(45.0 / math.sqrt(2.0), abs=5)
    rot = scp.getCorrectedPixelRot(np.array([scp.getResWidth(), scp.getResHeight()]))
    assert rot.x_degrees == pytest.approx(0.0, abs=5)
    assert rot.y_degrees == pytest.approx(45.0 / math.sqrt(2.0), abs=5)
    assert rot.z_degrees == pytest.approx(-45.0 / math.sqrt(2.0), abs=5)


def test_GetVisibleLine(scp) -> None:
    camRt = RotTrlTransform3d()
    a = Translation3d()
    b = Translation3d()
    retval = scp.getVisibleLine(camRt, a, b)
    assert retval == (None, None)

    a = Translation3d(-5.0, -0.1, 0)
    b = Translation3d(5.0, 0.1, 0)
    retval = scp.getVisibleLine(camRt, a, b)
    assert retval == (0.5, 0.5)


def test_EstPixelNoise(scp) -> None:
    with pytest.raises(Exception):
        scp.test_EstPixelNoise(np.array([0, 0]))
    with pytest.raises(Exception):
        scp.test_EstPixelNoise(np.array([[0], [0]]))

    pts = np.array([[[0, 0]], [[0, 0]]])

    # No noise parameters set
    noisy = scp.estPixelNoise(pts)
    for n, p in zip(noisy, pts):
        assert n.all() == p.all()

    # Noise parameters set
    scp.setCalibError(1.0, 1.0)
    noisy = scp.estPixelNoise(pts)
    for n, p in zip(noisy, pts):
        assert n.any() != p.any()
