import shutil
import os
from pathlib import Path

from hatchling.builders.hooks.plugin.interface import BuildHookInterface

class PhotonlibNativeBuildHook(BuildHookInterface):

    def initialize(self, version, build_data):

        lib_root_dir = Path(__file__).resolve().parent.parent.parent / "photon-targeting"
        
        #target = os.environ.get("PHOTON_TARGET", "linuxx86-64")
        #variant = os.environ.get("PHOTON_VARIANT", "release")
        target = "linuxx86-64"
        variant = "release"

        gradle_lib = lib_root_dir / "build" / "libs" / "photontargeting" / "shared" / target / variant / "libphotontargeting.so"

        include_dirs = [
            lib_root_dir / "src" / "main" / "native" / "include",
            lib_root_dir / "src" / "generated" / "main" / "native" / "include"
        ]

        dest = "src"

        # clean
        if os.path.exists(dest):
            shutil.rmtree(dest)

        os.makedirs(f"{dest}/lib", exist_ok=True)
        os.makedirs(f"{dest}/include", exist_ok=True)

        shutil.copy2(gradle_lib, f"{dest}/lib/")
        for include_dir in include_dirs:
            shutil.copytree(include_dir, f"{dest}/include/", dirs_exist_ok=True)