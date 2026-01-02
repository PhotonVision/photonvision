# Minimal interpolation submodule stub for docs
class TimeInterpolatableRotation2dBuffer:
    def __init__(self, *args, **kwargs):
        pass

    def addSample(self, *args, **kwargs):
        pass

    def sample(self, *args, **kwargs):
        return None


class TimeInterpolatablePose3dBuffer:
    def __init__(self, *args, **kwargs):
        # buffer of Pose3d-like objects for docs import
        pass

    def addSample(self, *args, **kwargs):
        pass

    def sample(self, *args, **kwargs):
        return None


__all__ = ["TimeInterpolatableRotation2dBuffer", "TimeInterpolatablePose3dBuffer"]
