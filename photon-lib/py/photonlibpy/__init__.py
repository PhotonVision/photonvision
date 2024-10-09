###############################################################################
## Copyright (C) Photon Vision.
###############################################################################
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU General Public License for more details.
##
## You should have received a copy of the GNU General Public License
## along with this program.  If not, see <https://www.gnu.org/licenses/>.
###############################################################################


def _prepare_to_load_photonlib():
    # force-load native libraries
    import ntcore
    import wpiutil
    import wpinet
    import wpimath
    import wpilib
    import hal
    import wpilib.cameraserver
    import robotpy_apriltag

    # and now our extension module
    import platform

    if platform.system().lower() == "windows":
        import os

        os.add_dll_directory(
            os.path.dirname(os.path.dirname(os.path.realpath(__file__))) + os.path.sep + "lib"
        )
    if platform.system().lower() == "darwin":
        import sys
        import os
        sys.path.append(os.path.dirname(os.path.realpath(__file__)) + os.path.sep + "lib")


_prepare_to_load_photonlib()
import sys
print(sys.path)
from .lib._photonlibpy import *
