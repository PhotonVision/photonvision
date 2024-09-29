# force-load native library dependencies
import ntcore  # nopa
import wpiutil  # nopa
import wpinet  # nopa
import wpimath  # nopa
import wpilib  # nopa
import hal  # nopa
import wpilib.cameraserver  # noqa
import robotpy_apriltag  # noqa

from os.path import abspath, join, dirname

# If on windows, we can't set rpath - force it like this
import platform

if platform.system().lower() == "Windows":
    import os

    _root = abspath(dirname(__file__))
    os.add_dll_directory(join(_root, "lib"))
