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
        if (dest / "lib" / "libphotontargeting.so").exists():
            register(dest / "lib")
            register(dest / "include")
            return

        # build_hook.py lives at photon-lib/src/main/python/photontargeting-native/
        # repo root is 5 levels up from that directory
        repo_root = Path(__file__).resolve().parents[5]
        lib_root_dir = repo_root / "photon-targeting"

        gradle_lib = (
            lib_root_dir
            / "build"
            / "libs"
            / "photontargeting"
            / "shared"
            / target
            / variant
            / "libphotontargeting.so"
        )

        include_dirs = [
            lib_root_dir / "src" / "main" / "native" / "include",
            lib_root_dir / "src" / "generated" / "main" / "native" / "include",
        ]

        if not gradle_lib.exists():
            raise FileNotFoundError(
                f"photontargeting library not found at {gradle_lib}. "
                f"Run the build-native.sh script first."
            )

        if not any(d.exists() for d in include_dirs):
            raise FileNotFoundError(
                f"No include directories found. Expected at least one of: {include_dirs}"
            )

        if dest.exists():
            shutil.rmtree(dest)

        (dest / "lib").mkdir(parents=True)
        (dest / "include").mkdir(parents=True)

        shutil.copy2(gradle_lib, dest / "lib")
        for include_dir in include_dirs:
            if include_dir.exists():
                shutil.copytree(include_dir, dest / "include", dirs_exist_ok=True)

        register(dest / "lib")
        register(dest / "include")
