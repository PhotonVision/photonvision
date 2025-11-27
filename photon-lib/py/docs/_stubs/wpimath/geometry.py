# Minimal geometry stubs for Sphinx documentation

class Rotation3d:
    def __init__(self, roll=0.0, pitch=0.0, yaw=0.0):
        # store yaw as the primary rotation for simple stubs
        self.roll = roll
        self.pitch = pitch
        self.yaw = yaw

    def toRotation2d(self):
        # convert yaw to a Rotation2d for simple compatibility in docs build
        return Rotation2d(self.yaw)

class Translation3d:
    def __init__(self, x=0.0, y=0.0, z=0.0):
        # Support both (x, y, z) and (distance, Rotation3d) forms used by the real wpimath
        # If y is a Rotation3d, compute a point at 'distance' along its yaw/pitch
        try:
            from math import cos, sin
        except Exception:
            def cos(x):
                return x
            def sin(x):
                return x

        if hasattr(y, "yaw") and hasattr(y, "pitch"):
            # interpret constructor as Translation3d(distance, Rotation3d)
            distance = float(x)
            pitch = float(getattr(y, "pitch", 0.0))
            yaw = float(getattr(y, "yaw", 0.0))
            # approximate spherical -> cartesian
            self._x = distance * cos(pitch) * cos(yaw)
            self._y = distance * cos(pitch) * sin(yaw)
            self._z = distance * sin(pitch)
        else:
            self._x = float(x)
            self._y = float(y)
            self._z = float(z)

    def X(self):
        return self._x

    def Y(self):
        return self._y

    def Z(self):
        return self._z

class Pose3d:
    def __init__(self, *args, **kwargs):
        pass

class Rotation2d:
    def __init__(self, *args):
        # Accept several initialization forms used in the real wpimath Rotation2d
        # - Rotation2d(angle)
        # - Rotation2d(fx, xOffset) used by SimCameraProperties.getPixelYaw
        if len(args) == 0:
            self._angle = 0.0
        elif len(args) == 1:
            self._angle = float(args[0])
        else:
            # fallback: when called with fx, xOffset, approximate angle as 0.0
            self._angle = 0.0

    def degrees(self):
        from math import degrees

        return degrees(self._angle)

    def radians(self):
        return float(self._angle)
    
    def __add__(self, other):
        # allow Rotation2d + Rotation2d or Rotation2d + numeric
        if hasattr(other, "_angle"):
            return Rotation2d(self._angle + float(other._angle))
        try:
            return Rotation2d(self._angle + float(other))
        except Exception:
            return NotImplemented

    def __radd__(self, other):
        # numeric + Rotation2d
        return self.__add__(other)

    def __sub__(self, other):
        if hasattr(other, "_angle"):
            return Rotation2d(self._angle - float(other._angle))
        try:
            return Rotation2d(self._angle - float(other))
        except Exception:
            return NotImplemented

    def __neg__(self):
        return Rotation2d(-self._angle)

    def __repr__(self):
        return f"Rotation2d({self._angle})"

class Translation2d:
    def __init__(self, x=0.0, y=0.0):
        self._x = float(x)
        self._y = float(y)

    def X(self):
        return self._x

    def Y(self):
        return self._y

class Pose2d:
    def __init__(self, *args, **kwargs):
        pass

    def __repr__(self) -> str:
        return "Pose2d()"


class Transform3d:
    def __init__(self, *args, **kwargs):
        pass


class Quaternion:
    def __init__(self, *args, **kwargs):
        pass

# Expose names commonly used by photonlibpy
__all__ = ["Rotation3d", "Translation3d", "Pose3d", "Rotation2d", "Translation2d", "Pose2d"]
