import os
import shutil
from pathlib import Path

from hatchling.builders.hooks.plugin.interface import BuildHookInterface


class PhotonlibNativeBuildHook(BuildHookInterface):

    def initialize(self, version, build_data):
        target = os.environ.get("PHOTON_TARGET", "linuxx86-64")
        variant = os.environ.get("PHOTON_VARIANT", "release")

        dest = Path("src")

        def register(d: Path):
            for f in d.rglob("*"):
                if f.is_file():
                    build_data["force_include"][str(f)] = str(f)

        # When building a wheel from an sdist the hook runs from a temp dir
        # where the .so was already copied during the sdist step — skip the copy.
        if (dest / "lib" / "libphotonlib.so").exists():
            register(dest / "lib")
            register(dest / "include")
            return

        # build_hook.py lives at photon-lib/src/main/python/photonlib-native/
        # repo root is 5 levels up from that directory
        repo_root = Path(__file__).resolve().parents[5]
        lib_root_dir = repo_root / "photon-lib"

        gradle_lib = (
            lib_root_dir
            / "build"
            / "libs"
            / "photonlib"
            / "shared"
            / target
            / variant
            / "libphotonlib.so"
        )

        gradle_include = lib_root_dir / "src" / "main" / "native" / "include"

        if not gradle_lib.exists():
            raise FileNotFoundError(
                f"photonlib library not found at {gradle_lib}. "
                f"Run the build-native.sh script first."
            )

        if not gradle_include.exists():
            raise FileNotFoundError(f"Include directory not found at {gradle_include}")

        if dest.exists():
            shutil.rmtree(dest)

        (dest / "lib").mkdir(parents=True)
        (dest / "include").mkdir(parents=True)

        shutil.copy2(gradle_lib, dest / "lib")
        shutil.copytree(gradle_include, dest / "include", dirs_exist_ok=True)

        register(dest / "lib")
        register(dest / "include")
