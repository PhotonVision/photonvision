import shutil
import os
#from pathlib import Path

from hatchling.builders.hooks.plugin.interface import BuildHookInterface

class PhotonlibNativeBuildHook(BuildHookInterface):

    def initialize(self, version, build_data):
        
        #target = os.environ.get("PHOTON_TARGET", "linuxx86-64")
        #variant = os.environ.get("PHOTON_VARIANT", "release")
        target = "linuxx86-64"
        variant = "release"

        gradle_lib = f"../../build/libs/photonlib/shared/{target}/{variant}/libphotonlib.so"
        dest = "src/photonlib-native"

        gradle_include = "../../src/main/native/include"

        # clean
        if os.path.exists(dest):
            shutil.rmtree(dest)

        os.makedirs(f"{dest}/lib", exist_ok=True)
        os.makedirs(f"{dest}/include", exist_ok=True)

        shutil.copy2(gradle_lib, f"{dest}/lib/")
        shutil.copytree(gradle_include, f"{dest}/include/", dirs_exist_ok=True)